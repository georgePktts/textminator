package com.gpak.tools.textminator.model;

public class LineResult {
    private String line;
    private boolean changed;

    public LineResult(String line, boolean changed) {
        this.line = line;
        this.changed = changed;
    }

    public String getLine() {
        return line;
    }

    public boolean isChanged() {
        return changed;
    }
}
