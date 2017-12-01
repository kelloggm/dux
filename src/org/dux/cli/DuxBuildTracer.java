package org.dux.cli;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private Map<Path, HashCode> fileHashes;

    public DuxBuildTracer(List<String> args) {
        ArrayList<String> argList = new ArrayList<>(Arrays.asList(STRACE_CALL));
        argList.addAll(args);
        this.args = argList.toArray(new String[0]);

        fileHashes = new HashMap<Path, HashCode>();
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
        for (Map.Entry<Path, HashCode> entry : fileHashes.entrySet()) {
            Path p = entry.getKey();
            HashCode hash = entry.getValue();
            File f = p.toFile();
            config.add(new DuxConfigurationEntry(p.toString(), hash, !p.isAbsolute(), f));
        }
    }

    private void parseStraceFile() throws IOException, FileNotFoundException {
        List<DuxStraceCall> calls = DuxStraceParser.parse(TMP_FILE);
	Path currentDir = Paths.get(".").toAbsolutePath().normalize();

        debugPrint("created strace call list");

        for (DuxStraceCall c : calls) {
            debugPrint("recording a call: " + c);

            // disregard everything but open and exec calls, for now
            debugPrint("checking if the call is an open or exec");
            if (!c.call.equals("open") && !c.call.matches("exec.*")) {
                continue;
            }

            // disregard if return value unknown or indicated failure
            debugPrint("checking if the call succeeded");
            if (!c.knownReturn || c.returnValue == -1) {
                continue;
            }

            // need to get first argument, which is absolute path surrounded in quotes
            debugPrint("getting rawpath");
            String rawPath = c.args[0];
            debugPrint("getting path from this rawpath: " + rawPath);
            String path = rawPath.substring(1, rawPath.length() - 1);
            debugPrint("got path: " + path);

	    // we only want to hash regular files
	    Path p = Paths.get(path).normalize();
	    debugPrint("checking if file is a regular file");
	    if (!Files.isRegularFile(p)) {
		debugPrint(p.toString() + " is not a regular file");
		continue;
	    }

	    // we want to relativize the path if it seems like it could be user-specific
	    // (don't want absolute paths to go down user-specific directories if another
	    //  user might run the build with the same directory structure)
	    // Probably some cases where this is insufficient but it's a start

	    // Min prefix length of 1 ==> they share a prefix that isn't root
	    debugPrint("checking if file shares prefix with the current working directory");
	    if (p.isAbsolute() && pathsSharePrefix(p, currentDir, 1)) {
		debugPrint(p.toString() + "shares prefix with the current directory");
		p = currentDir.relativize(p).normalize();
	    }

            // don't hash if it's already present
            debugPrint("checking if file already hashed");
            if (fileHashes.containsKey(p)) {
                continue;
            }

            debugPrint("generating hash");
            try {
                HashCode hash = hashFile(path);
                fileHashes.put(p, hash);
            } catch (FileNotFoundException e) {
                // must be a file created and deleted during the build
                continue;
            }
        }

        debugPrint("completed recording of calls");
    }

    private static HashCode hashFile(String path)
            throws IOException, FileNotFoundException {

        debugPrint("hashing this path: " + path);

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

        debugPrint("hashing complete for path: " + path);
        return hasher.hash();
    }

    private static boolean pathsSharePrefix(Path p1, Path p2, int minPrefixLength) {
	Path absolute1 = p1.toAbsolutePath().normalize();
	Path absolute2 = p2.toAbsolutePath().normalize();

	int nameCount1 = absolute1.getNameCount();
	int nameCount2 = absolute2.getNameCount();

	int indices = Math.min(nameCount1, nameCount2);
	// paths too short
	if (indices < minPrefixLength) {
	    return false;
	}

	int sharedPrefixLength = 0;
	for (int i = 0; i < indices; i++) {
	    Path elt1 = absolute1.getName(i);
	    Path elt2 = absolute2.getName(i);
	    if (!elt1.equals(elt2)) {
		break;
	    }
	    sharedPrefixLength++;
	}
	
	return sharedPrefixLength >= minPrefixLength;
    }
}
