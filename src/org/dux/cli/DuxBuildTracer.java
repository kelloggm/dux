package org.dux.cli;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.dux.cli.DuxVerbosePrinter.debugPrint;

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
            "-f",                        // trace subprocesses as well
            "-e", "trace=file,process",  // we care about calls to open or exec
            "-o", TMP_FILE               // write to tmp file
    };

    private String[] args;
    private Map<String, HashCode> fileHashes;

    public DuxBuildTracer(List<String> args) {
        ArrayList<String> argList = new ArrayList<>(Arrays.asList(STRACE_CALL));
        argList.addAll(args);
        this.args = argList.toArray(new String[0]);

        fileHashes = new HashMap<String, HashCode>();
    }

    public void trace() throws IOException, InterruptedException {
        debugPrint("beginning a trace, getting runtime");
        Runtime rt = Runtime.getRuntime();
        debugPrint("runtime acquired, executing program");
        Process proc = rt.exec(args);
        debugPrint("waiting for build to terminate");
        proc.waitFor();
        debugPrint("parsing strace file");
        parseStraceFile();
        debugPrint("deleting strace file");
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

        debugPrint("created strace call list");

        for (DuxStraceCall c : calls) {
            debugPrint("recording a call: " + c);

            if (!c.call.equals("open") && !c.call.matches("exec.*")) {
                continue;
            }

            // disregard if return value unknown or indicated failure
            if (!c.knownReturn || c.returnValue == -1) {
                continue;
            }

            // need to get first argument, which is absolute path surrounded in quotes
            String rawPath = c.args[0];
            String path = rawPath.substring(1, rawPath.length() - 1);

            // don't hash if it's already present
            if (fileHashes.containsKey(path)) {
                continue;
            }

            try {
                HashCode hash = hashFile(path);
                fileHashes.put(path, hash);
            } catch (FileNotFoundException e) {
                // must be a file created and deleted during the build
                continue;
            }
        }

        debugPrint("completed recording of calls");
    }

    private static HashCode hashFile(String path)
            throws IOException, FileNotFoundException {
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

        return hasher.hash();
    }
}
