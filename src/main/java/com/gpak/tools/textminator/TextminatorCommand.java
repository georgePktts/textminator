package com.gpak.tools.textminator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextminatorCommand {

    ToolContext context;

    public TextminatorCommand(ToolContext context) {
        this.context = context;
    }
    
    public int executeCommand() throws FileNotFoundException, IOException {
        if (context == null) {
            throw new IllegalStateException("Tool context is not initialized!");
        }
        if (context.getConfigGroup().printConfigExample) {
            Util.printConfigExample(context);
            return ToolContext.EXIT_OK;
        }

        Util.validateInputOptions(context);
        loadRules();

        if (context.getConfigGroup().printConfigInfo) {
            Util.printRules(context);
            return ToolContext.EXIT_OK;
        }

        context.setStartNanos(System.nanoTime());
        context.setInteractive(Util.isInteractive(context));

        try (BufferedReader reader = createReader();
            PrintWriter writer = createWriter()) {
            Console.info("Start processing");

            String line;
            while ((line = reader.readLine()) != null) {
                context.incrementTotalNumberOfLines();

                Console.trace("Sanitize line: " + context.getTotalNumberOfLines());
                String salitizedLine = sanitizeLine(line);

                if (!context.getDiagnosticsGroup().isDryRun) {
                    writer.println(salitizedLine);
                }
            }

            if (context.getTotalNumberOfLines() == 0 && !context.isInteractive()) {
                throw new IllegalStateException("Input was empty!");
            }

            if (!context.isMatchFound()) {
                throw new IllegalStateException("No match found!");
            }

            Console.info("Processing finished");
        }

        if (context.getIoGroup().outputFile != null && !context.getDiagnosticsGroup().isDryRun) {
            Console.info("Writing output file: " + context.getIoGroup().outputFile.getName());

            try {
                CopyOption[] copyOptions = context.getIoGroup().overwriteOutputFile 
                    ? new CopyOption[] { StandardCopyOption.REPLACE_EXISTING, 
                                        StandardCopyOption.ATOMIC_MOVE }
                    : new CopyOption[] { StandardCopyOption.ATOMIC_MOVE };
                
                Files.move(context.getTempOutputFile().toPath(), context.getIoGroup().outputFile.toPath(), copyOptions);
            } catch (IOException e) {
                throw new IOException("Failed to move temporary file to output: " + e.getMessage());
            }
        }

        if (context.getDiagnosticsGroup().printStats || context.getDiagnosticsGroup().isDryRun) {
            context.setElapsedNanos(System.nanoTime() - context.getStartNanos());
            Util.printStatsSummary(context);
        }

        return ToolContext.EXIT_OK;
    }

    private void loadRules() {
        Console.info("Load rules");

        context.setRules(new ArrayList<>());
        Properties properties = null;

        // 1. Load rules from user config file if provided
        if (context.getConfigGroup().userConfigFile != null) {
            properties = loadUserConfigFile();
        }

        // 3. Load from textminator.properties next to the jar
        if (properties == null) {
            properties = loadFolderConfigFile();
        }

        // 4. Fallback to built-in config file
        if (properties == null) {
            Console.warn("Fallback to built-in config file");
            properties = loadDefaultConfigFile();
        }

        // If no properties found then throw an exception
        if (properties == null) {
            throw new IllegalStateException("No config file found!");
        }

        // Create rules list
        Map<Integer, List<String>> uniqueOrder = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.endsWith(".regex")) {
                continue;
            }

            String baseName = key.substring(0, key.length() - ".regex".length());
            String regex = properties.getProperty(baseName + ".regex");

            if (regex == null || regex.isEmpty()) {
                continue;
            }

            String replacement = properties.getProperty(baseName + ".replacement");
            String enabledString = properties.getProperty(baseName + ".enabled");
            String orderString = properties.getProperty(baseName + ".order");

            if (orderString == null) {
                throw new IllegalStateException("order is missing from rule: " + baseName);
            }

            if (ToolContext.DEFUALT_REPLACEMENT_VALUE.equals(replacement)) {
                Console.warn("Property replacement is missing from: " + baseName);
                Console.warn("Using default value of " + ToolContext.DEFUALT_REPLACEMENT_VALUE);
                replacement = ToolContext.DEFUALT_REPLACEMENT_VALUE;
            }

            boolean enabled = true;
            if (enabledString == null) {
                Console.warn("Property enabled is missing");
                Console.warn("Using default value of " + true);
            } else {
                enabled = Boolean.valueOf(enabledString);
            }
            
            Rule rule = new Rule(baseName, Pattern.compile(regex), replacement, Integer.parseInt(orderString), enabled);

            context.getRules().add(rule);
            uniqueOrder.computeIfAbsent(rule.getOrder(), k -> new ArrayList<>()).add(rule.getName());
        }

        if (context.getRules().size() == 0) {
            throw new IllegalStateException("No rules found in " + context.getLoadedConfigFile() + " config file!");
        }

        if (context.getRules().size() != uniqueOrder.size()) {
            Console.warn("Multiple rules found with the same order");

            for (Entry<Integer, List<String>> set : uniqueOrder.entrySet()) {
                if (set.getValue().size() > 1) {
                    Console.warn(set.getValue().size() + " rules have order: " + set.getKey());

                    for (String s : set.getValue()) {
                        Console.warn("  " + s);
                    }
                }
            } 
        }

        context.getRules().sort(Comparator.comparingInt(Rule::getOrder).thenComparing(r -> r.getName()));

        Console.info("Initialize statistics");
        context.setStatistics(new LinkedHashMap<>());
        for (Rule r : context.getRules()) {
            context.getStatistics().put(r.getName(), 0L);
        }

        Console.info("Rules loaded");
    }

    private Properties loadUserConfigFile() {
        Console.debug("Loading custom config file...");
        Properties properties = new Properties();

        if (!context.getConfigGroup().userConfigFile.exists()) {
            throw new IllegalStateException("Config file not found: " + context.getConfigGroup().userConfigFile.getAbsolutePath());
        }

        try (InputStream in = new FileInputStream(context.getConfigGroup().userConfigFile)) {
            properties.load(in);
            context.setLoadedConfigFile(context.getConfigGroup().userConfigFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load custom config file: " + e.getMessage());
        }

        if (properties.size() == 0) {
            throw new IllegalStateException("Custom config file doesn't contains any rules!");
        }

        return properties;
    }

    private Properties loadFolderConfigFile() {
        Console.debug("Loading config file next to jar...");
        Properties properties = new Properties();

        try {
            File jarFile = new File(Main.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());

            File defaultConfig = new File(jarFile.getParentFile(), ToolContext.DEFAULT_CONFIG_FILE_NAME);
            if (defaultConfig.exists()) {
                try (InputStream in = new FileInputStream(defaultConfig)) {
                    properties.load(in);
                    context.setLoadedConfigFile(defaultConfig);
                }
            } else {
                properties = null;
                context.setLoadedConfigFile(null);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load next to jar config file: " + e.getMessage());
        }

        return properties;
    }

    private Properties loadDefaultConfigFile() {
        Console.debug("Loading built-in config file...");
        Properties properties = new Properties();

        try (InputStream in = Main.class.getResourceAsStream("/" + ToolContext.DEFAULT_CONFIG_FILE_NAME)) {
            if (in != null) {
                properties.load(in);
            } else {
                throw new IllegalStateException("No built-in config file found on classpath.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load built-in config file: " + e.getMessage());
        }

        return properties;
    }

    private String sanitizeLine(String line) {
        if (context.getRules() == null || context.getRules().isEmpty() || line == null ||line.isEmpty()) {
            return line;
        }

        String result = line;
        for (Rule rule : context.getRules()) {
            Matcher matcher = rule.getPattern().matcher(result);
            StringBuffer sb = new StringBuffer();
            long matches = 0;

            while (matcher.find()) {
                matches++;
                context.setMatchFound(true);
                matcher.appendReplacement(sb, rule.getReplacement());
            }

            if (matches == 0) {
                continue;
            }

            matcher.appendTail(sb);
            result = sb.toString();

            Console.trace("Rule: " + rule.getName() + " matched " + matches + " time(s)");
            if (context.getDiagnosticsGroup().printStats || context.getDiagnosticsGroup().isDryRun) {
                context.getStatistics().merge(rule.getName(), matches, Long::sum);
            }
        }

        return result;
    }

    private BufferedReader createReader() throws FileNotFoundException {
        if (context.getIoGroup().inputFile != null) {
            return new BufferedReader(new InputStreamReader(new FileInputStream(context.getIoGroup().inputFile), StandardCharsets.UTF_8));
        }
        return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

    private PrintWriter createWriter() throws FileNotFoundException {
        if (context.getIoGroup().outputFile != null) {
            return new PrintWriter(new OutputStreamWriter(new FileOutputStream(context.getTempOutputFile()), StandardCharsets.UTF_8), false);
        }
        return new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    }
}
