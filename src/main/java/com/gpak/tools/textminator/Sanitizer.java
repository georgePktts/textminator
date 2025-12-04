package com.gpak.tools.textminator;

import java.util.regex.Matcher;

public class Sanitizer {

    ToolContext context;

    public Sanitizer(ToolContext context) {
        this.context = context;
    }
    
    public String sanitizeLine(String line) {
        if (context.getRules() == null || context.getRules().isEmpty() || line == null ||line.isEmpty()) {
            return line;
        }

        String result = line;
        for (Rule rule : context.getRules()) {
            if (!rule.isEnabled()) {
                continue;
            }

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
}
