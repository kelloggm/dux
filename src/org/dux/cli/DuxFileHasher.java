package org.dux.cli;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Class that implements shared code for hashing a file for use in
 * both tracing builds and checking configurations
 */
public class DuxFileHasher {
    private static final int FILE_BUF_SIZE = 1024;

    public static HashCode hashFile(String path)
            throws IOException, FileNotFoundException {

        DuxCLI.logger.debug("hashing this path: {}", path);

        HashFunction hf = Hashing.sha256();
        Hasher hasher = hf.newHasher();

        try (FileInputStream fs = new FileInputStream(path)) {
            byte[] buf = new byte[FILE_BUF_SIZE];
            while (true) {
                int len = fs.read(buf);
                if (len == -1) {
                    break;
                }
                hasher.putBytes(buf, 0, len);
            }
        }

        DuxCLI.logger.debug("hashing complete for path: {}", path);
        return hasher.hash();
    }
}
