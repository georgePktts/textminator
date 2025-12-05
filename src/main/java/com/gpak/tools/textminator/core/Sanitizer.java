package com.gpak.tools.textminator.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.gpak.tools.textminator.model.LineResult;
import com.gpak.tools.textminator.model.Rule;
import com.gpak.tools.textminator.util.Console;

public class Sanitizer {

    private List<Rule> rules;
    private Map<String, Long> statistics = null;
    private boolean calculateStatistics = false;

    public Sanitizer(List<Rule> rules, boolean isDryRun, boolean printStats) {
        this.rules = rules;
        if (isDryRun || printStats) {
            calculateStatistics = true;
            initStatistics();
        }
    }
    
    public LineResult sanitizeLine(String line) {
        boolean matchFound = false;

        if (rules == null || rules.isEmpty() || line == null ||line.isEmpty()) {
            return new LineResult(line, matchFound);
        }

        String result = line;
        for (Rule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }

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
            if (calculateStatistics) {
                statistics.merge(rule.getName(), matches, Long::sum);
            }
        }

        return new LineResult(result, matchFound);
    }

    private Map<String, Long> initStatistics() {
        Console.info("Initialize statistics");
        statistics = new LinkedHashMap<>();

        for (Rule r : rules) {
            statistics.put(r.getName(), 0L);
        }

        return statistics;
    }

    public Map<String, Long> getStatistics() {
        return statistics;
    }
}
