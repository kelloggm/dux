package org.dux.cli;

import com.google.devtools.common.options.OptionsParser;
import org.dux.backingstore.DuxBackingStore;
import org.dux.backingstore.DuxBackingStoreBuilder;

import java.io.IOException;
import java.util.Collections;

import static org.dux.cli.DuxVerbosePrinter.debugPrint;

/**
 * The driver for the Dux build orchestration system.
 *
 * This is a command line utility.
 *
 * To invoke it, run "java Dux [options]"
 */
public class DuxCLI {

    private static boolean DEBUG = false;

    public static void main(String[] args) {
        OptionsParser parser = OptionsParser.newOptionsParser(DuxOptions.class);
        parser.parseAndExitUponError(args);
        DuxOptions options = parser.getOptions(DuxOptions.class);

        if (options.help) {
            printUsage(parser);
            return;
        }

        if (options.debug) {
            DuxVerbosePrinter.DEBUG = true;
        }

        DuxBackingStore backingStore = new DuxBackingStoreBuilder()
                .type(options.storeType)
                .bucket(options.bucketName)
                .build();

        if (options.command.equals("NOT SET")) {
            // This means no command was specified. Read and print the specified dux file.
            debugPrint("reading configuration file: " + options.file);
            DuxConfiguration config = DuxConfigurationIO.read(options.file);
            System.out.println(config);
            return;
        } else {
            // A command was specified, so execute and trace it, and print the results to
            // the specified config file.
            debugPrint("creating build tracer");
            DuxBuildTracer tracer = new DuxBuildTracer(Collections.singletonList(options.command));
            debugPrint("beginning trace of this program: " + options.command);
            try {
                tracer.trace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return;
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                return;
            }
            debugPrint("tracing complete");
            String displayName = options.displayName.equals("NOT SET") ? null : options.displayName;
            debugPrint("display name computed: " + displayName);
            DuxConfiguration config = new DuxConfiguration(displayName);
            debugPrint("new configuration created");
            tracer.dumpToConfiguration(config);
            debugPrint("finished dumping trace to configuration");
            DuxConfigurationIO.write(options.file, config);
            debugPrint("wrote configuration file: " + options.file);
            return;
        }
    }

    private static void printUsage(OptionsParser parser) {
        System.out.println("Usage: dux OPTIONS");
        System.out.println(parser.describeOptions(Collections.<String, String>emptyMap(),
                OptionsParser.HelpVerbosity.LONG));
    }
}