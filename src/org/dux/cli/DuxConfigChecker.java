package org.dux.cli;

import com.google.common.hash.HashCode;
import org.dux.backingstore.DuxBackingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.dux.cli.DuxFileHasher.hashFile;

/**
 * An object responsible for checking the current system against
 * the dependencies logged in the Dux config and attempts to correct.
 */
public class DuxConfigChecker {
    private DuxBackingStore store;

    private static Logger LOGGER = (Logger) LoggerFactory.getLogger(DuxConfigChecker.class);

    public DuxConfigChecker(DuxBackingStore store) {
        this.store = store;
    }

    /**
     * For the given config, iterate through and check that each file is present. 
     * If the file is missing, download it from the store to the desired location. 
     * If the file is present but the hash does not match, print a warning.
     *
     * @throws FileNotFoundException    if a fetch fails
     */
    public void checkConfig(DuxConfiguration config, boolean launch) throws IOException, FileNotFoundException {
        for (DuxConfigurationEntry entry : config.entries()) {
            // if file does not exist, try to fetch it
            LOGGER.debug("Checking if file {} exists", entry.path.toString());
            if (!entry.path.exists()) {
                LOGGER.info("File {} does not exist, pulling", entry.path.toString());
                if (!store.fetchFile(entry.hashCode.toString(),
                        entry.path.toString())) {
                    LOGGER.error("Failed to download entry {} to location {}", entry.hashCode.toString(), entry.path);
                    throw new FileNotFoundException(entry.hashCode.toString());
                }
                LOGGER.info("Successfully fetched file {}", entry.path.toString());
                continue;
            }

            // file exists so let's compare the hash code to the entry's
            LOGGER.debug("Computing hash for {}", entry.path);
            HashCode hash = hashFile(entry.path.toString());
            if (!hash.equals(entry.hashCode)) {
                LOGGER.debug("Hash does not match, printing a warning");
                LOGGER.warn("Hash for {} does not match stored config.\nExpected: {}\nObtained: {}", entry.path.toString(), entry.hashCode.toString(), hash.toString());
                continue;
            }

            LOGGER.debug("{} exists and hash matches", entry.path.toString());
        }

        for (DuxConfigurationLink link : config.links()) {
            LOGGER.debug("reading a link: {}", link);
            link.create();
        }

        for (DuxConfigurationVar var : config.vars()) {
            LOGGER.debug("required environment variable: {}", var);
        }

        if (launch) {
            LOGGER.debug("executing build: {}", config.command);
            ProcessBuilder pb = new ProcessBuilder(config.command);
            for (DuxConfigurationVar var : config.vars()) {
                var.set(pb);
            }
            LOGGER.debug("PATH for launched build: {}", pb.environment().get("PATH"));
            pb.inheritIO();
            pb.start();
        }
    }
}
