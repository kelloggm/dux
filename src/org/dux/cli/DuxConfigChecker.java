package org.dux.cli;

import com.google.common.hash.HashCode;
import org.dux.backingstore.DuxBackingStore;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.dux.cli.DuxFileHasher.hashFile;
import static org.dux.cli.DuxVerbosePrinter.debugPrint;

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
            debugPrint("Checking if file " + entry.path.toString() + " exists");
            if (!entry.path.exists()) {
                debugPrint("File " + entry.path.toString() + " does not exist, pulling");
                if (!store.fetchFile(entry.hashCode.toString(),
                        entry.path.toString())) {
                    debugPrint("Failed to download entry " + entry.hashCode.toString()
                            + " to location " + entry.path);
                    throw new FileNotFoundException(entry.hashCode.toString());
                }
                debugPrint("Successfully fetched file " + entry.path.toString());
                continue;
            }

            // file exists so let's compare the hash code to the entry's
            debugPrint("Computing hash for " + entry.path);
            HashCode hash = hashFile(entry.path.toString());
            if (!hash.equals(entry.hashCode)) {
                debugPrint("Hash does not match, printing a warning");
                System.out.println("Warning: Hash for " + entry.path.toString()
                        + " does not match stored config."
                        + "\nExpected: " + entry.hashCode.toString()
                        + "\nObtained: " + hash.toString());
                continue;
            }

            debugPrint(entry.path.toString() + " exists and hash matches");
        }
    }
}
