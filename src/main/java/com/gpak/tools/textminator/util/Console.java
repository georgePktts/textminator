package com.gpak.tools.textminator.util;

import picocli.CommandLine.Help.Ansi;

public class Console {
    
    private Console() { }

    private static boolean isQuiet = false;
    private static boolean isTrace = false;
    private static boolean[] verbose;

    // Setters variables
    public static void setQuiet(boolean q) {
        isQuiet = q;
    }

    public static void setTrace(boolean t) {
        isTrace = t;
    }

    public static void setVerbose(boolean[] v) {
        verbose = v;
    }

    // Find verbose level
    private static int verboseLevel() {
        return (verbose == null) ? 0 : verbose.length;
    }

    /**
     * Prints to stderr with new line. Respects --quiet option
     * 
     * @param message
     */
    private static void printlnStderr(String message) {
        if (!isQuiet)
            System.err.println(message);
    }

    public static void error(String message) {
        if (message != null)
            printlnStderr(Ansi.AUTO.string("@|bold,red [ERROR]|@ " + message));
    }

    public static void warn(String message) {
        if (verboseLevel() >= 1)
            printlnStderr(Ansi.AUTO.string("@|bold,yellow [WARNING]|@ " + message));
    }

    public static void info(String message) {
        if (verboseLevel() >= 2)
            printlnStderr(Ansi.AUTO.string("@|bold,green [INFO]|@ " + message));
    }

    public static void debug(String message) {
        if (verboseLevel() >= 3)
            printlnStderr(Ansi.AUTO.string("@|bold,blue [DEBUG]|@ " + message));
    }

    public static void debug(Exception e) {
        if (verboseLevel() >= 3)
            e.printStackTrace(System.err);
    }

    public static void trace(String message) {
        if (isTrace)
            printlnStderr(Ansi.AUTO.string("@|bold,magenta [TRACE]|@ " + message));
    }

    /**
     * Print to stderr. Should be used to print stats only
     * 
     * @param message
     */
    public static void stats(String message) {
        printlnStderr(Ansi.AUTO.string("@|bold,green [STATS]|@ " + message));
    }

    /**
     * Print to stderr. Should be used to print config only
     * 
     * @param message
     */
    public static void config(String message) {
        printlnStderr(Ansi.AUTO.string("@|bold,green [CONFIG]|@ " + message));
    }

    /**
     * Print to stdout.
     * 
     * @param message
     */
    public static void println(String message) {
        System.out.println(message);
    }
}
