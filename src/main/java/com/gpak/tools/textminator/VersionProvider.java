package com.gpak.tools.textminator;

import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = "dev";
        }
        return new String[] { "${COMMAND-NAME} version " + version };
    }
}
