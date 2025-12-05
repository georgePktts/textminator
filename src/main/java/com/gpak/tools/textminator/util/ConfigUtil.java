package com.gpak.tools.textminator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

import com.gpak.tools.textminator.Main;
import com.gpak.tools.textminator.core.ToolContext;
import com.gpak.tools.textminator.model.Rule;

public class ConfigUtil {

    // *************************************************************************
    //
    // Load configuration
    //
    // *************************************************************************
    
    public static List<Rule> loadConfigFile(File userConfigFile) {
        Console.info("Load rules");
        List<Rule> rules = new ArrayList<>();

        Properties properties = null;

        // 1. Load rules from user config file if provided
        if (userConfigFile != null) {
            properties = loadUserConfigFile(userConfigFile);
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

        rules = parseProperties(properties);
        validateRules(rules);

        return rules;
    }

    private static Properties loadUserConfigFile(File userConfigFile) {
        Console.debug("Loading custom config file...");
        Properties properties = new Properties();

        if (!userConfigFile.exists()) {
            throw new IllegalStateException("Config file not found: " + userConfigFile.getAbsolutePath());
        }

        try (InputStream in = new FileInputStream(userConfigFile)) {
            properties.load(in);
            // context.setLoadedConfigFile(context.getConfigGroup().userConfigFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load custom config file: " + e.getMessage());
        }

        if (properties.size() == 0) {
            throw new IllegalStateException("Custom config file doesn't contains any rules!");
        }

        return properties;
    }

    private static Properties loadFolderConfigFile() {
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
                    // context.setLoadedConfigFile(defaultConfig);
                }
            } else {
                properties = null;
                // context.setLoadedConfigFile(null);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load next to jar config file: " + e.getMessage());
        }

        return properties;
    }

    private static Properties loadDefaultConfigFile() {
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

    private static List<Rule> parseProperties(Properties properties) {
        List<Rule> rules = new ArrayList<>();

        // Create rules list
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

            rules.add(rule);
        }

        if (rules.size() == 0) {
            throw new IllegalStateException("No rules found in config file!");
        }

        rules.sort(Comparator.comparingInt(Rule::getOrder).thenComparing(r -> r.getName()));

        return rules;
    }

    // *************************************************************************
    //
    // Validations
    //
    // *************************************************************************

    public static void validateInputOptions(File outputFile, boolean overwriteOutputFile) throws FileAlreadyExistsException {
        Console.debug("Validate input options");

        if (outputFile == null && overwriteOutputFile) {
            Console.warn("--force is used in combination with --output option only");
        }

        if (outputFile == null) {
            return;
        }

        if (!outputFile.exists()) {
            return;
        }

        if (!overwriteOutputFile) {
            Console.error("Output file " + outputFile + " already exists");
            Console.error("Use --force to overwrite, or specify a different --output-file");
            throw new FileAlreadyExistsException(null);
        }
    }

    private static void validateRules(List<Rule> rules) {
        Console.debug("Validate rules");

        if (rules.isEmpty() || rules.size() == 0) {
            throw new IllegalStateException("No rules loaded!");
        }

        boolean atLeastOneEnabled = false;
        Map<Integer, List<String>> uniqueOrder = new HashMap<>();
        for (Rule rule : rules) {
            if (rule.isEnabled()) {
                atLeastOneEnabled = true;
            }

            uniqueOrder.computeIfAbsent(rule.getOrder(), k -> new ArrayList<>()).add(rule.getName());
        }

        if (!atLeastOneEnabled) {
            throw new IllegalAccessError("All rules are disbled!");
        }

        if (rules.size() != uniqueOrder.size()) {
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
    }
}
