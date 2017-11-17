package org.dux.cli;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import com.google.common.hash.Hashing;
import com.google.common.hash.Hasher;
import com.google.common.hash.HashFunction;
import com.google.common.hash.HashCode;

/**
 * An object responsible for invoking the appropriate build tracer
 * and running the build tool. It parses the traced output and dumps
 * to a config file.
 * <p>
 * Currently depends on having strace available
 */
public class DuxBuildTracer {
    private static final int FILE_BUF_SIZE = 1024;
    private static final String TMP_FILE = ".dux_out";
    private static final String[] STRACE_CALL = {
            "strace",
            "-f",               // trace subprocesses as well
            "-e", "trace=open",  // only check calls to open
            "-o", TMP_FILE      // write to tmp file
    };

    private String[] args;
    private Map<String, HashCode> fileHashes;

    public DuxBuildTracer(List<String> args) {
        ArrayList<String> argList = new ArrayList<>(Arrays.asList(STRACE_CALL));
        argList.addAll(args);
        this.args = (String[]) argList.toArray();

        fileHashes = new HashMap<String, HashCode>();
    }

    public void trace() throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(args);
        proc.waitFor();

        parseStraceFile();

        // get rid of strace TMP file once we're done
        File f = new File(TMP_FILE);
        f.delete();
    }

    public void dumpToConfiguration(DuxConfiguration config) {
        for (Map.Entry<String, HashCode> entry : fileHashes.entrySet()) {
            String path = entry.getKey();
            HashCode hash = entry.getValue();
            File f = new File(path);
            config.add(new DuxConfigurationEntry(path, hash, false, f));
        }
    }

    private void parseStraceFile() throws IOException, FileNotFoundException {
        List<DuxStraceCall> calls = DuxStraceParser.parse(TMP_FILE);

        for (DuxStraceCall c : calls) {
            if (!c.call.equals("open")) {
                continue;
            }

	    // disregard if return value unknown or indicated failure
	    if (!c.knownReturn || c.returnValue == -1) {
		continue;
	    }

            // don't hash if it's already present
            if (fileHashes.containsKey(c.call)) {
                continue;
            }

            // need to get first argument, which is absolute path surrounded in quotes
            String rawPath = c.args[0];
            String path = rawPath.substring(1, rawPath.length() - 1);

            HashCode hash = hashFile(path);
            fileHashes.put(path, hash);
        }
    }

    private static HashCode hashFile(String path)
            throws IOException, FileNotFoundException {
        HashFunction hf = Hashing.sha256();
        Hasher hasher = hf.newHasher();

        try (FileInputStream fs = new FileInputStream(path)) {
            byte[] buf = new byte[FILE_BUF_SIZE];
            while (fs.available() > 0) {
                int len = fs.read(buf);
                hasher.putBytes(buf, 0, len);
            }
        }

        return hasher.hash();
    }
}
