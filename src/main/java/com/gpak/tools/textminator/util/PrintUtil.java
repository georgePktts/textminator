package com.gpak.tools.textminator.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import com.gpak.tools.textminator.Main;
import com.gpak.tools.textminator.core.ToolContext;
import com.gpak.tools.textminator.model.Rule;

public class PrintUtil {

    private PrintUtil() { }
    
    public static void printConfigExample() {
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

    public static void printRules(List<Rule> rules, File userConfigFile) {
        Console.debug("Print rules");

        if (rules == null || rules.isEmpty()) {
            Console.config("  (no rules loaded)");
            return;
        }

        Console.config("Config source: " + 
                        (userConfigFile != null
                            ? userConfigFile.getAbsolutePath()
                            : "default (built-in) - or properties next to jar"));

        Console.config("");
        Console.config("Loaded rules in execution order:");

        for (Rule rule : rules) {
            Console.config("  " + rule.getName());
            Console.config("    order  : " + rule.getOrder());
            Console.config("    enabled: " + rule.isEnabled());
            Console.config("    regex  : " + rule.getPattern());
            Console.config("    replace: " + rule.getReplacement());
        }
    }

    public static void printStatsSummary(Map<String, Long> statistics, long elapsedNanos, long totalNumberOfLines) {
        Console.debug("Print summary");
        Console.stats(ToolContext.TOOL_NAME + " stats:");

        Console.stats(String.format("  elapsed time:     %.3f s", elapsedNanos / 1_000_000_000.0));
        Console.stats("  total file lines: " + totalNumberOfLines);
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
}
