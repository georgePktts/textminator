package com.gpak.tools.textminator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileAlreadyExistsException;

public class Util {

    private Util() { }

    public static boolean isInteractive(ToolContext context) {
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

    public static void validateInputOptions(ToolContext context) throws FileAlreadyExistsException {
        Console.debug("Validate input options");

        if (context.getIoGroup().outputFile == null && context.getIoGroup().overwriteOutputFile) {
            Console.warn("--force is taken into account only with --output");
        }

        if (context.getIoGroup().outputFile == null) {
            return;
        }

        context.setTempOutputFile(new File(context.getIoGroup().outputFile.getAbsolutePath() + ".tmp"));

        if (!context.getIoGroup().outputFile.exists()) {
            return;
        }

        if (!context.getIoGroup().overwriteOutputFile) {
            Console.error("Output file " + context.getIoGroup().outputFile + " already exists");
            Console.error("Use --force to overwrite, or specify a different --output-file");
            throw new FileAlreadyExistsException(null);
        }
    }

    /**
     * Prints the built-in config file to stdout
     */
    public static void printConfigExample(ToolContext context) {
        Console.debug("Print config example");

        try (InputStream in = Main.class.getResourceAsStream("/" + ToolContext.DEFAULT_CONFIG_FILE_NAME)) {
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

    public static void printRules(ToolContext context) {
        Console.debug("Print rules");

        Console.config("Config source: " + 
                        (context.getLoadedConfigFile() != null
                            ? context.getLoadedConfigFile().getAbsolutePath()
                            : "default (built-in)"));

        Console.config("");
        Console.config("Loaded rules in execution order:");
        if (context.getRules() == null || context.getRules().isEmpty()) {
            Console.config("  (no rules loaded)");
        } else {
            for (Rule rule : context.getRules()) {
                Console.config("  " + rule.getName());
                Console.config("    order  : " + rule.getOrder());
                Console.config("    enabled: " + rule.isEnabled());
                Console.config("    regex  : " + rule.getPattern());
                Console.config("    replace: " + rule.getReplacement());
            }
        }
    }

    public static void printStatsSummary(ToolContext context) {
        Console.debug("Print summary");
        Console.stats(ToolContext.TOOL_NAME + " stats:");

        Console.stats(String.format("  elapsed time:     %.3f s", context.getElapsedNanos() / 1_000_000_000.0));
        Console.stats("  total file lines: " + context.getTotalNumberOfLines());
        Console.stats("");

        if (context.getStatistics().isEmpty()) {
            Console.stats("  no rules and/or no matches");
            return;
        }

        Console.stats("  rules:");
        context.getStatistics().forEach((name, count) -> {
            Console.stats(String.format("    %-25s %d", name, count));
        });
    }
}
