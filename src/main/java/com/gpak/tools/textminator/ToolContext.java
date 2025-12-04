package com.gpak.tools.textminator;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.gpak.tools.textminator.Main.ConfigGroup;
import com.gpak.tools.textminator.Main.DiagnosticsGroup;
import com.gpak.tools.textminator.Main.IOGroup;

public class ToolContext {

    // Constants
    public static final String TOOL_NAME = "textminator";
    public static final String DEFAULT_CONFIG_FILE_NAME = TOOL_NAME + ".properties";
    public static final String DEFUALT_REPLACEMENT_VALUE = "<REPLACED>";

    public static final Integer EXIT_OK = 0;
    public static final Integer EXIT_ERR = 1;

    // CLI options
    private final ConfigGroup configGroup;
    private final IOGroup ioGroup;
    private final DiagnosticsGroup diagnosticsGroup;

    // Variables
    private List<Rule> rules;
    private Map<String, Long> statistics;

    private boolean matchFound = false;
    private boolean isInteractive = false;
    
    private File tempOutputFile = null;
    private File loadedConfigFile = null;

    private long startNanos = 0;
    private long elapsedNanos = 0;
    private long totalNumberOfLines = 0;

    public ToolContext(ConfigGroup configGroup, IOGroup ioGroup, DiagnosticsGroup diagnosticsGroup) {
        this.configGroup = configGroup;
        this.ioGroup = ioGroup;
        this.diagnosticsGroup = diagnosticsGroup;
    }

    public ConfigGroup getConfigGroup() {
        if (configGroup == null)
            throw new IllegalStateException("CLI options not properly initialized!");
        return configGroup;
    }

    public IOGroup getIoGroup() {
        if (ioGroup == null)
            throw new IllegalStateException("CLI options not properly initialized!");
        return ioGroup;
    }

    public DiagnosticsGroup getDiagnosticsGroup() {
        if (diagnosticsGroup == null)
            throw new IllegalStateException("CLI options not properly initialized!");
        return diagnosticsGroup;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public Map<String, Long> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Long> statistics) {
        this.statistics = statistics;
    }

    public boolean isMatchFound() {
        return matchFound;
    }

    public void setMatchFound(boolean matchFound) {
        this.matchFound = matchFound;
    }

    public boolean isInteractive() {
        return isInteractive;
    }

    public void setInteractive(boolean isInteractive) {
        this.isInteractive = isInteractive;
    }

    public File getTempOutputFile() {
        return tempOutputFile;
    }

    public void setTempOutputFile(File tempOutputFile) {
        this.tempOutputFile = tempOutputFile;
    }

    public File getLoadedConfigFile() {
        return loadedConfigFile;
    }

    public void setLoadedConfigFile(File loadedConfigFile) {
        this.loadedConfigFile = loadedConfigFile;
    }

    public long getStartNanos() {
        return startNanos;
    }

    public void setStartNanos(long startNanos) {
        this.startNanos = startNanos;
    }

    public long getElapsedNanos() {
        return elapsedNanos;
    }

    public void setElapsedNanos(long elapsedNanos) {
        this.elapsedNanos = elapsedNanos;
    }

    public long getTotalNumberOfLines() {
        return totalNumberOfLines;
    }

    public void setTotalNumberOfLines(long totalNumberOfLines) {
        this.totalNumberOfLines = totalNumberOfLines;
    }

    public void incrementTotalNumberOfLines() {
        this.totalNumberOfLines += 1;
    }
}
