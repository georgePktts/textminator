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
import java.nio.file.FileAlreadyExistsException;
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
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "textminator",
    versionProvider = VersionProvider.class,
    header = "Replaces sensitive data from files or from stdin.",
    synopsisHeading = "usage: ",
    synopsisSubcommandLabel = "",
    descriptionHeading = "%n@|bold Description:|@%n",
    description = "@|bold ${COMMAND-NAME}|@ is a CLI tool that replaces sensitive data — such as " +
        "emails, IP addresses, and custom patterns — from files or stdin. " +
        "It supports files, piping, and an optional interactive mode (exit with " +
        "Ctrl+D on Unix/macOS or Ctrl+Z on Windows).%n"+
        "%n" +
        "The tool is fully extensible through a configuration file, allowing "+
        "users to define custom regex patterns and replacement values to " +
        "replace any type of text.%n" +
        "%n" +
        "@|bold Configuration:|@%n" +
        "The tool uses the following order when loading the config file:" +
        "%n" +
        "  custom      use file provided via --config%n" +
        "  jar         search for ${COMMAND-NAME}.properties next to the JAR%n" +
        "  (default)   built-in configuration file%n" +
        "%n" +
        "To inspect the final loaded configuration with the --config-info option.%n" +
        "%n" +
        "@|bold Logging:|@%n" +
        "Verbosity controls the amount of diagnostic output:" +
        "%n" +
        "  (default)   only errors are printed. Execution stops on the first error%n" +
        "  -v          show warnings%n" +
        "  -vv         show informational messages%n" +
        "  -vvv        enable debug logging (includes full stack traces)%n" +
        "%n" +
        "The --trace option provides low-level rule tracing operates independently " +
        "of -v.%n" +
        "Warning: This option produces extremely verbose output, significantly " +
        "affecting performance",
    optionListHeading = "%n@|bold Options:|@",
    sortOptions = false,
    footerHeading = "%n@|bold Examples:|@%n",
    footer = {
        "  Process text from stdin and output to stdout",
        "    cat input.txt | @|bold ${COMMAND-NAME}|@",
        "",
        "  Sanitize a file and output to stdout",
        "    @|bold ${COMMAND-NAME}|@ -i input.txt",
        "",
        "  Sanitize and write to output file",
        "    @|bold ${COMMAND-NAME}|@ -i input.txt -o clean.txt",
        "",
        "  Preview replacements without writing output",
        "    @|bold ${COMMAND-NAME}|@ --dry-run -i input.txt",
        "",
        "  Show per-rule statistics after processing",
        "    @|bold ${COMMAND-NAME}|@ --stats -i input.txt",
        "",
        "  Use a custom configuration file",
        "    @|bold ${COMMAND-NAME}|@ --config-file myrules.properties -i input.txt",
        "",
    },
    exitCodeListHeading = "%n@|bold Exit Codes:|@%n",
    exitCodeList = {
        "0: Successful execution",
        "1: Processing error"},
    exitCodeOnSuccess = 0,
    exitCodeOnUsageHelp = 0,
    exitCodeOnVersionHelp = 0,
    exitCodeOnInvalidInput = 1,
    exitCodeOnExecutionException = 1
)
public class Main implements Callable<Integer> {

    // CLI options
    @ArgGroup(heading = "%n@|bold Configuration:|@%n",
        exclusive = false,
        order = 1)
    ConfigGroup configGroup = new ConfigGroup();

    @ArgGroup(heading = "%n@|bold Input / Output:|@%n",
        exclusive = false,
        order = 2)
    IOGroup ioGroup = new IOGroup();

    @ArgGroup(heading = "%n@|bold Diagnostics:|@%n", 
        exclusive = false,
        order = 3)
    DiagnosticsGroup diagnosticsGroup = new DiagnosticsGroup();

    static class ConfigGroup {
        @Option(names = {"-c", "--config"},
            description = {"path to custom config file", "  default: ${COMMAND-NAME}.properties next to the jar"})
        private File userConfigFile;

        @Option(names = {"--config-example"},
            description = "print an example configuration file and exit")
        private boolean printConfigExample;

        @Option(names = {"--config-info"},
            description = "print the effective configuration and exit")
        private boolean printConfigInfo;
    }

    static class IOGroup {
        @Option(names = {"-i", "--input"},
            description = {"input file", "  default: stdin"})
        private File inputFile;

        @Option(names = {"-o", "--output"},
            description = {"output file", "  default: stdout"})
        private File outputFile;

        @Option(names = {"-f", "--force"},
            description = {"overwrite output file if exists"})
        private boolean overwriteOutputFile;
    }

    static class DiagnosticsGroup {
        @Option(names = {"-s", "--stats"},
            description = "print per-rule match statistics after processing")
        private boolean printStats;

        @Option(names = {"--dry-run"},
            description = "same with --stats but WITHOUT processing")
        private boolean isDryRun;

        @Option(names = {"-q", "--quiet"},
            description = "suppress all diagnostic print (including --stats and --config-info), except for errors. Overrides -v")
        private boolean isQuiet;

        @Option(names = "-v",
            description = "specify multiple -v options to increase verbosity. For example, `-v -v -v` or `-vvv`")
        private boolean[] verbose;

        @Option(names = {"--trace"},
            description = {"enable low-level match tracing (independent of --verbose)", 
                            "@|bold Warning:|@ this option significantly impacts performance"})
        private boolean isTrace;

        @Option(names = {"-h", "--help"},
            usageHelp = true,
            description = "print this help and exit")
        private boolean printHelp;

        @Option(names = {"-V", "--version"},
            versionHelp = true,
            description = "print version and exit")
        private boolean printVersion;
    }

    // Constants
    private static final Integer EXIT_OK = 0;
    private static final Integer EXIT_ERR = 1;

    private static final String TOOL_NAME = "textminator";
    private static final String DEFAULT_CONFIG_FILE_NAME = TOOL_NAME + ".properties";
    private static final String DEFUALT_REPLACEMENT_VALUE = "<REPLACED>";

    // Variables
    private List<Rule> rules;
    private Map<String, Long> statistics;

    private boolean matchFound = false;
    private boolean isInteractive = false;
    
    private File tempOutputFile = null;
    private File loadedConfigFile = null;

    private long startNanos = 0;
    private long elapsedNanos = 0;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        int statusCode = EXIT_OK;

        // try {
        //     isInteractive = (System.in.available() == 0);
        // } catch (IOException e) {
        //     isInteractive = true;
        // }

        // if (isInteractive && ioGroup.inputFile == null) {
        //     System.out.println("Interactive mode!");
        // } else {
        //     System.out.println("Piped / redirected input detected.");
        // }

        // if (isInteractive && ioGroup.inputFile == null) {
        //     System.out.println("Interactive mode!");
        // }

        isInteractive = isInteractive();

        // Initialize console
        Console.setQuiet(diagnosticsGroup.isQuiet);
        Console.setTrace(diagnosticsGroup.isTrace);
        Console.setVerbose(diagnosticsGroup.verbose);

        try {
            if (configGroup.printConfigExample) {
                printConfigExample();
                return EXIT_OK;
            }

            validateInputOptions();
            loadRules();

            if (configGroup.printConfigInfo) {
                printRules();
                return EXIT_OK;
            }

            startNanos = System.nanoTime();

            try (BufferedReader reader = createReader();
                PrintWriter writer = createWriter()) {
                Console.info("Start processing");

                String line;
                long linesCounter = 0;
                while ((line = reader.readLine()) != null) {
                    linesCounter++;

                    Console.trace("Sanitize line: " + linesCounter);
                    String salitizedLine = sanitizeLine(line);

                    if (!diagnosticsGroup.isDryRun) {
                        writer.println(salitizedLine);
                    }
                }

                if (linesCounter == 0 && !isInteractive) {
                    throw new IllegalStateException("Input was empty!");
                }

                if (!matchFound) {
                    throw new IllegalStateException("No match found!");
                }

                Console.info("Processing finished");
            }

            elapsedNanos = System.nanoTime() - startNanos;

            if (ioGroup.outputFile != null && !diagnosticsGroup.isDryRun) {
                Console.info("Writing output file: " + ioGroup.outputFile.getName());

                try {
                    CopyOption[] copyOptions = ioGroup.overwriteOutputFile 
                        ? new CopyOption[] { StandardCopyOption.REPLACE_EXISTING, 
                                            StandardCopyOption.ATOMIC_MOVE }
                        : new CopyOption[] { StandardCopyOption.ATOMIC_MOVE };
                    
                    Files.move(tempOutputFile.toPath(), ioGroup.outputFile.toPath(), copyOptions);
                } catch (IOException e) {
                    throw new IOException("Failed to move temporary file to output: " + e.getMessage());
                }
            }

            if (diagnosticsGroup.printStats || diagnosticsGroup.isDryRun) {
                printStatsSummary();
            }
        } catch (Exception e) {
            Console.error(e.getMessage());
            Console.debug(e);
            statusCode = EXIT_ERR;
        }

        return statusCode;
    }

    private static boolean isInteractive() {
        try {
            // pipe or redirect => stdin has data
            if (System.in.available() > 0) {
                return false;
            }
        } catch (IOException ignored) {}

        // console exists => interactive terminal
        if (System.console() != null) {
            return true;
        }

        // fallback: very likely piped/redirected
        return false;
    }

    private void validateInputOptions() throws FileAlreadyExistsException {
        Console.debug("Validate input options");

        if (ioGroup.outputFile == null && ioGroup.overwriteOutputFile) {
            Console.warn("--force is taken into account only with --output");
        }

        if (ioGroup.outputFile == null) {
            return;
        }

        tempOutputFile = new File(ioGroup.outputFile.getAbsolutePath() + ".tmp");

        if (!ioGroup.outputFile.exists()) {
            return;
        }

        if (!ioGroup.overwriteOutputFile) {
            Console.error("Output file " + ioGroup.outputFile + " already exists");
            Console.error("Use --force to overwrite, or specify a different --output-file");
            throw new FileAlreadyExistsException(null);
        }
    }

    private List<Rule> loadRules() {
        Console.info("Load rules");

        rules = new ArrayList<>();
        Properties properties = null;

        // 1. Load rules from user config file if provided
        if (configGroup.userConfigFile != null) {
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

            if (DEFUALT_REPLACEMENT_VALUE.equals(replacement)) {
                Console.warn("Property replacement is missing from: " + baseName);
                Console.warn("Using default value of " + DEFUALT_REPLACEMENT_VALUE);
                replacement = DEFUALT_REPLACEMENT_VALUE;
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
            uniqueOrder.computeIfAbsent(rule.getOrder(), k -> new ArrayList<>()).add(rule.getName());
        }

        if (rules.size() == 0) {
            throw new IllegalStateException("No rules found in " + loadedConfigFile + " config file!");
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

        rules.sort(Comparator.comparingInt(Rule::getOrder).thenComparing(r -> r.getName()));

        Console.info("Initialize statistics");
        statistics = new LinkedHashMap<>();
        for (Rule r : rules) {
            statistics.put(r.getName(), 0L);
        }

        Console.info("Rules loaded");
        return rules;
    }

    /**
     * Prints the built-in config file to stdout
     */
    private void printConfigExample() {
        Console.debug("Print config example");

        try (InputStream in = Main.class.getResourceAsStream("/" + DEFAULT_CONFIG_FILE_NAME)) {
            if (in != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Console.println(line);
                    }
                }
            } else {
                throw new IllegalStateException("No default rules file found on classpath!");
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }



    private void printStatsSummary() {
        Console.debug("Print summary");
        Console.stats(TOOL_NAME + " stats:");

        Console.stats(String.format("  elapsed time: %.3f s", elapsedNanos / 1_000_000_000.0));
        Console.stats("");

        if (statistics.isEmpty()) {
            Console.stats("  no rules and/or no matches");
            return;
        }

        Console.stats("  rules:");
        statistics.forEach((name, count) -> {
            Console.stats(String.format("    %-25s %d", name, count));
        });
    }

    private void printRules() {
        Console.debug("Print rules");

        Console.config("Config source: " + 
                        (loadedConfigFile != null
                            ? loadedConfigFile.getAbsolutePath()
                            : "default (built-in)"));

        Console.config("");
        Console.config("Loaded rules in execution order:");
        if (rules == null || rules.isEmpty()) {
            Console.config("  (no rules loaded)");
        } else {
            for (Rule rule : rules) {
                Console.config("  " + rule.getName());
                Console.config("    order  : " + rule.getOrder());
                Console.config("    enabled: " + rule.isEnabled());
                Console.config("    regex  : " + rule.getPattern());
                Console.config("    replace: " + rule.getReplacement());
            }
        }
    }

    private Properties loadUserConfigFile() {
        Console.debug("Loading custom config file...");
        Properties properties = new Properties();

        if (!configGroup.userConfigFile.exists()) {
            throw new IllegalStateException("Config file not found: " + configGroup.userConfigFile.getAbsolutePath());
        }

        try (InputStream in = new FileInputStream(configGroup.userConfigFile)) {
            properties.load(in);
            loadedConfigFile = configGroup.userConfigFile;
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

            File defaultConfig = new File(jarFile.getParentFile(), DEFAULT_CONFIG_FILE_NAME);
            if (defaultConfig.exists()) {
                try (InputStream in = new FileInputStream(defaultConfig)) {
                    properties.load(in);
                    loadedConfigFile = defaultConfig;
                }
            } else {
                properties = null;
                loadedConfigFile = null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load next to jar config file: " + e.getMessage());
        }

        return properties;
    }

    private Properties loadDefaultConfigFile() {
        Console.debug("Loading built-in config file...");
        Properties properties = new Properties();

        try (InputStream in = Main.class.getResourceAsStream("/" + DEFAULT_CONFIG_FILE_NAME)) {
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
        if (rules == null || rules.isEmpty() || line == null ||line.isEmpty()) {
            return line;
        }

        String result = line;
        for (Rule rule : rules) {
            Matcher matcher = rule.getPattern().matcher(result);
            StringBuffer sb = new StringBuffer();
            long matches = 0;

            while (matcher.find()) {
                matches++;
                matchFound = true;
                matcher.appendReplacement(sb, rule.getReplacement());
            }

            if (matches == 0) {
                continue;
            }

            matcher.appendTail(sb);
            result = sb.toString();

            Console.trace("Rule: " + rule.getName() + " matched " + matches + " time(s)");
            if (diagnosticsGroup.printStats || diagnosticsGroup.isDryRun) {
                statistics.merge(rule.getName(), matches, Long::sum);
            }
        }

        return result;
    }

    private BufferedReader createReader() throws FileNotFoundException {
        if (ioGroup.inputFile != null) {
            return new BufferedReader(new InputStreamReader(new FileInputStream(ioGroup.inputFile), StandardCharsets.UTF_8));
        }
        return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

    private PrintWriter createWriter() throws FileNotFoundException {
        if (ioGroup.outputFile != null) {
            return new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempOutputFile), StandardCharsets.UTF_8), false);
        }
        return new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    }
}
