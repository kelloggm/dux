package org.dux.cli;

import com.google.common.hash.HashCode;
import org.dux.backingstore.DuxBackingStore;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.dux.cli.DuxFileHasher.hashFile;

/**
 * An object responsible for checking the current system against
 * the dependencies logged in the Dux config and attempts to correct.
 */
public class DuxConfigChecker {
    private DuxBackingStore store;

    public DuxConfigChecker(DuxBackingStore store) {
        this.store = store;
    }

    /**
     * For the config at the specified path, iterate through and
     * check that each file is present. If the file is missing,
     * download it from the store to the desired location. If the
     * file is present but the hash does not match, print a warning.
     *
     * @throws IllegalArgumentException if the config fails to be read
     * @throws FileNotFoundException    if a fetch fails
     */
    public void checkConfig(String configPath) throws IOException, FileNotFoundException {
        DuxConfiguration config = DuxConfigurationIO.read(configPath);
        if (config == null) {
            throw new IllegalArgumentException("Configuration read failed");
        }

        for (DuxConfigurationEntry entry : config) {
            ;
            // file does not exist,
            DuxCLI.logger.debug("Checking if file {} exists", entry.path.toString());
            if (!entry.path.exists()) {
                DuxCLI.logger.debug("File {} does not exist, pulling", entry.path.toString());
                if (!store.fetchFile(entry.hashCode.toString(),
                        entry.path.toString())) {
                    DuxCLI.logger.error("Failed to download entry {} to location {}", entry.hashCode.toString(), entry.path);
                    throw new FileNotFoundException(entry.hashCode.toString());
                }
                DuxCLI.logger.debug("Successfully fetched file {}", entry.path.toString());
                continue;
            }

            // file exists so let's compare the hash code to the entry's
            DuxCLI.logger.debug("Computing hash for {}", entry.path);
            HashCode hash = hashFile(entry.path.toString());
            if (!hash.equals(entry.hashCode)) {
                DuxCLI.logger.debug("Hash does not match, printing a warning");
                DuxCLI.logger.warn("Hash for {} does not match stored config.\nExpected: {}\nObtained: {}", entry.path.toString(), entry.hashCode.toString(), hash.toString());
                continue;
            }

            DuxCLI.logger.debug("{} exists and hash matches", entry.path.toString());
        }
    }
}
