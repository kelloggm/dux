package org.dux.cli;

import com.google.devtools.common.options.OptionsParser;

import java.util.Collections;

/**
 * The driver for the Dux build orchestration system.
 *
 * This is a command line utility.
 *
 * To invoke it, run "java Dux [options]"
 */
public class DuxCLI {
    public static void main(String[] args) {
        OptionsParser parser = OptionsParser.newOptionsParser(DuxOptions.class);
        parser.parseAndExitUponError(args);
        DuxOptions options = parser.getOptions(DuxOptions.class);

        if (options.help) {
            printUsage(parser);
        }
    }

    private static void printUsage(OptionsParser parser) {
        System.out.println("Usage: dux OPTIONS");
        System.out.println(parser.describeOptions(Collections.<String, String>emptyMap(),
                OptionsParser.HelpVerbosity.LONG));
    }
}