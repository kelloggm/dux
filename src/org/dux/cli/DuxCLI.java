package org.dux.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.devtools.common.options.OptionsParser;
import org.dux.backingstore.DuxBackingStore;
import org.dux.backingstore.DuxBackingStoreBuilder;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

/**
 * The driver for the Dux build orchestration system.
 * <p>
 * This is a command line utility.
 * <p>
 * To invoke it, run "java Dux [options]"
 */
public class DuxCLI {

    private static Logger LOGGER = (Logger) LoggerFactory.getLogger(DuxCLI.class);

    public static void main(String[] args) {
        OptionsParser parser = OptionsParser.newOptionsParser(DuxOptions.class);
        parser.parseAndExitUponError(args);
        DuxOptions options = parser.getOptions(DuxOptions.class);

        // configuring logback based on the debug level that was passed
        LOGGER.setLevel(Level.toLevel(options.level));

        if (options.help) {
            printUsage(parser);
            return;
        }

        DuxBackingStore backingStore = new DuxBackingStoreBuilder()
                .type(options.storeType)
                .bucket(options.bucketName)
                .build();

        if (options.command.equals("NOT SET")) {
            // This means no command was specified. Read and print the specified dux file.
            LOGGER.debug("reading configuration file: {}", options.file);
            DuxConfiguration config = DuxConfigurationIO.read(options.file);

            if (options.dumpConfig) {
                LOGGER.info("config: {}", config);
            } else {
                LOGGER.debug("config: {}", config);
            }

            // if we've been set to check the config, we'll do that now
            if (options.checkConfig) {
                LOGGER.debug("checking configuration...");
                DuxConfigChecker checker = new DuxConfigChecker(backingStore);
                try {
                    checker.checkConfig(config, options.launch);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    return;
                }
                LOGGER.debug("finished checking");
            }
        } else {
            // A command was specified, so execute and trace it, and print the results to
            // the specified config file.
            LOGGER.debug("creating build tracer");
            DuxBuildTracer tracer = new DuxBuildTracer(Collections.singletonList(options.command));
            LOGGER.debug("beginning trace of this program: {}", options.command);
            try {
                tracer.trace(options.includeProjDir, options.includeDefaultBlacklist);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return;
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                return;
            }
            LOGGER.debug("tracing complete");
            String displayName = options.displayName.equals("NOT SET") ? null : options.displayName;
            LOGGER.debug("display name computed: {}", displayName);
            DuxConfiguration config = new DuxConfiguration(displayName, options.command);
            LOGGER.debug("new configuration created");
            tracer.dumpToConfiguration(config);
            LOGGER.debug("finished dumping trace to configuration");
            boolean result = config.sendToBackingStore(backingStore);
            if (result) {
                LOGGER.debug("finished sending to backing store");
            } else {
                LOGGER.debug("at least one send failed. See the log.");
            }
            DuxConfigurationIO.write(options.file, config);
            LOGGER.debug("wrote configuration file: {}", options.file);
        }

        if (options.fSaveConfig) {
            backingStore.storeFile(options.file, options.file);
        }
    }

    private static void printUsage(OptionsParser parser) {
        LOGGER.info("Usage: dux OPTIONS");
        LOGGER.info(parser.describeOptions(Collections.<String, String>emptyMap(),
                OptionsParser.HelpVerbosity.LONG));
    }
}
