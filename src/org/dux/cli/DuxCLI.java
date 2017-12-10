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

    public static Logger logger;

    public static void main(String[] args) {
        OptionsParser parser = OptionsParser.newOptionsParser(DuxOptions.class);
        parser.parseAndExitUponError(args);
        DuxOptions options = parser.getOptions(DuxOptions.class);

        logger = (Logger) LoggerFactory.getLogger(DuxCLI.class);
        logger.setLevel(Level.toLevel(options.level));

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
            logger.debug("reading configuration file: {}", options.file);
            DuxConfiguration config = DuxConfigurationIO.read(options.file);

            if (options.dumpConfig) {
                logger.info("config: {}", config);
            } else {
                logger.debug("config: {}", config);
            }

            // if we've been set to check the config, we'll do that now
            if (options.checkConfig) {
                logger.debug("checking configuration...");
                DuxConfigChecker checker = new DuxConfigChecker(backingStore);
                try {
                    checker.checkConfig(config);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    return;
                }
                logger.debug("finished checking");
            }
        } else {
            // A command was specified, so execute and trace it, and print the results to
            // the specified config file.
            logger.debug("creating build tracer");
            DuxBuildTracer tracer = new DuxBuildTracer(Collections.singletonList(options.command));
            logger.debug("beginning trace of this program: {}", options.command);
            try {
                tracer.trace(options.includeProjDir, options.includeDefaultBlacklist);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return;
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                return;
            }
            logger.debug("tracing complete");
            String displayName = options.displayName.equals("NOT SET") ? null : options.displayName;
            logger.debug("display name computed: {}", displayName);
            DuxConfiguration config = new DuxConfiguration(displayName);
            logger.debug("new configuration created");
            tracer.dumpToConfiguration(config);
            logger.debug("finished dumping trace to configuration");
            boolean result = config.sendToBackingStore(backingStore);
            if (result) {
                logger.debug("finished sending to backing store");
            } else {
                logger.debug("at least one send failed. See the log.");
            }
            DuxConfigurationIO.write(options.file, config);
            logger.debug("wrote configuration file: {}", options.file);
        }

        if (options.fSaveConfig) {
            backingStore.storeFile(options.file, options.file);
        }
    }

    private static void printUsage(OptionsParser parser) {
        logger.info("Usage: dux OPTIONS");
        logger.info(parser.describeOptions(Collections.<String, String>emptyMap(),
                OptionsParser.HelpVerbosity.LONG));
    }
}
