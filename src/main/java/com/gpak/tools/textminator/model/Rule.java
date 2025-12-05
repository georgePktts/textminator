package com.gpak.tools.textminator.model;

import java.util.regex.Pattern;

public class Rule {
    private final String name;
    private final Pattern pattern;
    private final String replacement;
    private final int order;
    private final boolean enabled;

    public Rule(String name, Pattern pattern, String replacement, int order, boolean enabled) {
        this.name = name;
        this.pattern = pattern;
        this.replacement = replacement;
        this.order = order;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getReplacement() {
        return replacement;
    }

    public int getOrder() {
        return order;
    }

    public boolean isEnabled() {
        return enabled;
    }
}