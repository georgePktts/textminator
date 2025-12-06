package com.gpak.tools.textminator;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import com.gpak.tools.textminator.core.ToolContext;
import com.gpak.tools.textminator.util.Console;
import com.gpak.tools.textminator.util.VersionProvider;

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

    public static class ConfigGroup {
        @Option(names = {"-c", "--config"},
            description = {"path to custom config file", "  default: ${COMMAND-NAME}.properties next to the jar"})
        File userConfigFile;

        @Option(names = {"--config-example"},
            description = "print an example configuration file and exit")
        boolean printConfigExample;

        @Option(names = {"--config-info"},
            description = "print the effective configuration and exit")
        boolean printConfigInfo;
    }

    public static class IOGroup {
        @Option(names = {"-i", "--input"},
            description = {"input file", "  default: stdin"})
        File inputFile;

        @Option(names = {"-o", "--output"},
            description = {"output file", "  default: stdout"})
        File outputFile;

        @Option(names = {"-f", "--force"},
            description = {"overwrite output file if exists"})
        boolean overwriteOutputFile;
    }

    public static class DiagnosticsGroup {
        @Option(names = {"-s", "--stats"},
            description = "print per-rule match statistics after processing")
        boolean printStats;

        @Option(names = {"--dry-run"},
            description = "same with --stats but WITHOUT processing")
        boolean isDryRun;

        @Option(names = {"-q", "--quiet"},
            description = "suppress all diagnostic print (including --stats and --config-info). Overrides -v")
        boolean isQuiet;

        @Option(names = "-v",
            description = "specify multiple -v options to increase verbosity. For example, `-v -v -v` or `-vvv`")
        boolean[] verbose;

        @Option(names = {"--trace"},
            description = {"enable low-level match tracing (independent of --verbose)", 
                            "@|bold Warning:|@ this option significantly impacts performance"})
        boolean isTrace;

        @Option(names = {"-h", "--help"},
            usageHelp = true,
            description = "print this help and exit")
        private boolean printHelp;

        @Option(names = {"-V", "--version"},
            versionHelp = true,
            description = "print version and exit")
        private boolean printVersion;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        int statusCode = ToolContext.EXIT_OK;

        try {
            // Initialize console
            Console.setQuiet(diagnosticsGroup.isQuiet);
            Console.setTrace(diagnosticsGroup.isTrace);
            Console.setVerbose(diagnosticsGroup.verbose);

            // Initialize tool context
            ToolContext context = new ToolContext(configGroup, ioGroup, diagnosticsGroup);
            context.setInteractive(isInteractive());

            TextminatorCommand command = new TextminatorCommand(context);
            statusCode = command.executeCommand();
        } catch (Exception e) {
            Console.error(e.getMessage());
            Console.debug(e);
            statusCode = ToolContext.EXIT_ERR;
        } finally {
            Console.debug("Exit with: " + statusCode);
        }

        return statusCode;
    }

    private static boolean isInteractive() {
        Console.debug("Check if is in interactive mode");

        try {
            // pipe or redirect => stdin has data
            if (System.in.available() > 0) {
                Console.debug("Not in interactive mode. Pipe or redirect");
                return false;
            }
        } catch (IOException ignored) {}

        // console exists => interactive terminal
        if (System.console() != null) {
            Console.debug("It's in interactive mode. Terminal");
            return true;
        }

        // fallback: very likely piped/redirected
        Console.debug("Not in interactive mode. Pipe or redirect");
        return false;
    }
}
