package org.dux.cli;

import com.google.common.hash.HashCode;

import java.io.File;
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

import static org.dux.cli.DuxFileHasher.hashFile;

/**
 * An object responsible for invoking the appropriate build tracer
 * and running the build tool. It parses the traced output and dumps
 * to a config file.
 * <p>
 * Currently depends on having strace available
 */
public class DuxBuildTracer {
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
        DuxCLI.logger.debug("beginning a trace, getting runtime");
        Runtime rt = Runtime.getRuntime();
        DuxCLI.logger.debug("runtime acquired, executing program");
        Process proc = rt.exec(args);
        DuxCLI.logger.debug("waiting for build to terminate");
        proc.waitFor();
        DuxCLI.logger.debug("parsing strace file");
        parseStraceFile();
        DuxCLI.logger.debug("deleting strace file");
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

        DuxCLI.logger.debug("created strace call list");

        for (DuxStraceCall c : calls) {
            DuxCLI.logger.debug("recording a call: {}", c);

            // disregard everything but open and exec calls, for now
            DuxCLI.logger.debug("checking if the call is an open or exec");
            if (!c.call.equals("open") && !c.call.matches("exec.*")) {
                continue;
            }

            // disregard if return value unknown or indicated failure
            DuxCLI.logger.debug("checking if the call succeeded");
            if (!c.knownReturn || c.returnValue == -1) {
                continue;
            }

            // need to get first argument, which is absolute path surrounded in quotes
            DuxCLI.logger.debug("getting rawpath");
            String rawPath = c.args[0];
            DuxCLI.logger.debug("getting path from this rawpath: {}", rawPath);
            String path = rawPath.substring(1, rawPath.length() - 1);
            DuxCLI.logger.debug("got path: {}", path);

	    // we only want to hash regular files
	    Path p = Paths.get(path).normalize();
	    DuxCLI.logger.debug("checking if file is a regular file");
	    if (!Files.isRegularFile(p)) {
		DuxCLI.logger.debug("{} is not a regular file", p.toString());
		continue;
	    }

	    // we want to relativize the path if it seems like it could be user-specific
	    // (don't want absolute paths to go down user-specific directories if another
	    //  user might run the build with the same directory structure)
	    // Probably some cases where this is insufficient but it's a start

	    // Min prefix length of 1 ==> they share a prefix that isn't root
	    DuxCLI.logger.debug("checking if file shares prefix with the current working directory");
	    if (p.isAbsolute() && pathsSharePrefix(p, currentDir, 1)) {
		DuxCLI.logger.debug("{} shares prefix with the current directory", p.toString());
		p = currentDir.relativize(p).normalize();
	    }

            // don't hash if it's already present
            DuxCLI.logger.debug("checking if file already hashed");
            if (fileHashes.containsKey(p)) {
                continue;
            }

            DuxCLI.logger.debug("generating hash");
            try {
                HashCode hash = hashFile(path);
                fileHashes.put(p, hash);
            } catch (FileNotFoundException e) {
                // must be a file created and deleted during the build
                continue;
            }
        }

        DuxCLI.logger.debug("completed recording of calls");
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
