package com.gpak.tools.textminator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.gpak.tools.textminator.core.Sanitizer;
import com.gpak.tools.textminator.core.ToolContext;
import com.gpak.tools.textminator.model.LineResult;
import com.gpak.tools.textminator.model.Rule;
import com.gpak.tools.textminator.util.ConfigUtil;
import com.gpak.tools.textminator.util.Console;
import com.gpak.tools.textminator.util.PrintUtil;

public class TextminatorCommand {

    private ToolContext context;
    private boolean matchFound = false;

    public TextminatorCommand(ToolContext context) {
        this.context = context;
    }
    
    public int executeCommand() throws FileNotFoundException, IOException {
        if (context == null) {
            throw new IllegalStateException("Tool context is not initialized!");
        }

        if (context.getConfigGroup().printConfigExample) {
            PrintUtil.printConfigExample();
            return ToolContext.EXIT_OK;
        }

        ConfigUtil.validateInputOptions(context.getIoGroup().outputFile, context.getIoGroup().overwriteOutputFile);
        List<Rule> rules = ConfigUtil.loadConfigFile(context.getConfigGroup().userConfigFile);

        if (context.getConfigGroup().printConfigInfo) {
            PrintUtil.printRules(rules, context.getConfigGroup().userConfigFile);
            return ToolContext.EXIT_OK;
        }

        context.setStartNanos(System.nanoTime());

        Sanitizer sanitizer = new Sanitizer(rules,
                                            context.getDiagnosticsGroup().isDryRun,
                                            context.getDiagnosticsGroup().printStats);
        try (BufferedReader reader = createReader(context.getIoGroup().inputFile);
            PrintWriter writer = createWriter(context.getIoGroup().outputFile)) {
            Console.info("Start processing");

            String line;
            while ((line = reader.readLine()) != null) {
                context.incrementTotalNumberOfLines();

                Console.trace("Sanitize line: " + context.getTotalNumberOfLines());
                LineResult lineResult = sanitizer.sanitizeLine(line);

                if (!context.getDiagnosticsGroup().isDryRun) {
                    writer.println(lineResult.getLine());
                }

                if (lineResult.isChanged()) {
                    matchFound = true;
                }
            }

            if (context.getTotalNumberOfLines() == 0 && !context.isInteractive()) {
                throw new IllegalStateException("Input was empty!");
            }

            if (!matchFound) {
                throw new IllegalStateException("No match found!");
            }

            Console.info("Processing finished");
        }

        if (context.getIoGroup().outputFile != null && !context.getDiagnosticsGroup().isDryRun) {
            Console.info("Writing output file: " + context.getIoGroup().outputFile.getName());

            try {
                CopyOption[] copyOptions = context.getIoGroup().overwriteOutputFile 
                    ? new CopyOption[] { StandardCopyOption.REPLACE_EXISTING, 
                                        StandardCopyOption.ATOMIC_MOVE }
                    : new CopyOption[] { StandardCopyOption.ATOMIC_MOVE };
                
                Files.move(tempFileFor(context.getIoGroup().outputFile).toPath(), context.getIoGroup().outputFile.toPath(), copyOptions);
            } catch (IOException e) {
                throw new IOException("Failed to move temporary file to output: " + e.getMessage());
            }
        }

        if (context.getDiagnosticsGroup().printStats || context.getDiagnosticsGroup().isDryRun) {
            long elapsedNanos = System.nanoTime() - context.getStartNanos();
            PrintUtil.printStatsSummary(sanitizer.getStatistics(), elapsedNanos, context.getTotalNumberOfLines());
        }

        return ToolContext.EXIT_OK;
    }

    private BufferedReader createReader(File inputFile) throws FileNotFoundException {
        if (inputFile != null) {
            return new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8));
        }
        return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

    private PrintWriter createWriter(File outputFile) throws FileNotFoundException {
        if (outputFile != null) {
            File tempOutputFile = tempFileFor(outputFile);
            return new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempOutputFile), StandardCharsets.UTF_8), false);
        }
        return new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    }

    private File tempFileFor(File outputFile) {
        return new File(outputFile.getAbsolutePath() + ".tmp");
    }
}
