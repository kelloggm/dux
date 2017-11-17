package org.dux.cli;

import com.google.devtools.common.options.OptionsParser;

import java.io.IOException;
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
            return;
        }

        if (options.command.equals("NOT SET")) {
            // This means no command was specified. Read and print the specified dux file.
            DuxConfiguration config = DuxConfigurationIO.read(options.file);
            System.out.println(config);
            return;
        } else {
            // A command was specified, so execute and trace it, and print the results to
            // the specified config file.
            DuxBuildTracer tracer = new DuxBuildTracer(Collections.singletonList(options.command));
            try {
                tracer.trace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return;
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                return;
            }
            String displayName = options.displayName.equals("NOT SET") ? null : options.displayName;
            DuxConfiguration config = new DuxConfiguration(displayName);
            tracer.dumpToConfiguration(config);
            DuxConfigurationIO.write(options.file, config);
        }
    }

    private static void printUsage(OptionsParser parser) {
        System.out.println("Usage: dux OPTIONS");
        System.out.println(parser.describeOptions(Collections.<String, String>emptyMap(),
                OptionsParser.HelpVerbosity.LONG));
    }
}