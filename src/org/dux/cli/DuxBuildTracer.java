package org.dux.cli;

import com.google.common.hash.HashCode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            "-e", "trace=open,execve,readlink",  // we care about calls to open or exec
            "-o", TMP_FILE               // write to tmp file
    };

    private String[] args;
    private Map<Path, HashCode> fileHashes;

    private Map<Path, Path> links;
    private Map<Path, String> envPaths;
    private Set<String> envVarsWithPathSep;

    private Set<DuxConfigurationVar> varsToSave;

    public DuxBuildTracer(List<String> args) {
        ArrayList<String> argList = new ArrayList<>(Arrays.asList(STRACE_CALL));
        argList.addAll(args);
        this.args = argList.toArray(new String[0]);

        fileHashes = new HashMap<>();
        links = new HashMap<>();
        envPaths = new HashMap<>();
        envVarsWithPathSep = new HashSet<>();
        varsToSave = new HashSet<>();

        // initialize the map of environment variable values (as paths) to the names of the variables
        String pathSeparator = System.getProperty("path.separator");
        for (Map.Entry<String, String> var : System.getenv().entrySet()) {
            String value = var.getValue();
            String[] values = value.split(pathSeparator);
            if (values.length > 1) {
                envVarsWithPathSep.add(var.getKey());
            }
            for (String path : values) {
                try {
                    Path p = Paths.get(path).normalize();
                    envPaths.put(p, var.getKey());
                    DuxCLI.logger.debug("path: {} | variable: {}", p, var.getKey());
                } catch (InvalidPathException e) {
                    // an environment variable had an invalid path as its value. This is fine.
                }
            }
        }
    }

    private static boolean pathInSubdirectory(Path parent, Path candidate)
            throws IOException {
        String canonicalParent = parent.toFile().getCanonicalPath();
        String canonicalCandidate = candidate.toFile().getCanonicalPath();
        return canonicalCandidate.startsWith(canonicalParent);
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

    // https://stackoverflow.com/questions/1732455/redirect-process-output-to-stdout
    class StreamGobbler extends Thread {
        InputStream is;

        // reads everything from is until empty.
        StreamGobbler(InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line=null;
                while ( (line = br.readLine()) != null)
                    System.out.println(line);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public void trace(boolean includeProjDir, boolean includeDefaultBlacklist) throws IOException, InterruptedException {
        DuxCLI.logger.debug("beginning a trace, getting runtime");
        DuxCLI.logger.debug("trace params: {}, {}", includeProjDir, includeDefaultBlacklist);
        Runtime rt = Runtime.getRuntime();
        DuxCLI.logger.debug("runtime acquired, executing program");
        Process proc = rt.exec(args);
        StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());
        DuxCLI.logger.debug("waiting for build to terminate");
        outputGobbler.start();
        proc.waitFor();
        DuxCLI.logger.debug("Loading trace blacklist");
        DuxTraceBlacklist blacklist = new DuxTraceBlacklist(includeDefaultBlacklist);
        DuxCLI.logger.debug("parsing strace file");
        parseStraceFile(includeProjDir, blacklist);
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

        for (Map.Entry<Path, Path> entry : links.entrySet()) {
            Path link = entry.getKey();
            Path target = entry.getValue();
            config.addLink(new DuxConfigurationLink(link, target));
        }

        for (DuxConfigurationVar var : varsToSave) {
            config.addVar(var);
        }
    }

    private void parseStraceFile(boolean includeProjDir, DuxTraceBlacklist blacklist) throws IOException, FileNotFoundException {
        List<DuxStraceCall> calls = DuxStraceParser.parse(TMP_FILE);

        DuxCLI.logger.debug("created strace call list");

        for (DuxStraceCall c : calls) {
            DuxCLI.logger.debug("recording a call: {}", c);

            // disregard everything but open, exec, and readlink calls, for now
            DuxCLI.logger.debug("checking if the call is an open or exec");
            boolean fOpenOrExec = c.call.equals("open") || c.call.matches("exec.*");
            boolean fReadlink = c.call.equals("readlink");
            if (!fOpenOrExec && !fReadlink) {
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

            Path p = Paths.get(path).normalize();

            if (fOpenOrExec) {
                if ((p = canHashPath(p, blacklist, includeProjDir)) != null) {
                    DuxCLI.logger.debug("generating hash");
                    try {
                        HashCode hash = hashFile(path);
                        fileHashes.put(p, hash);
                    } catch (FileNotFoundException e) {
                        // must be a file created and deleted during the build
                        continue;
                    }
                }
            } else if (fReadlink) {
                // readlink calls are treated differently. We need to record the two paths into the
                // configuration file - assuming they pass all our regular tests - as a special pair
                // that's turned into a symbolic link by the config checker.

                // p is the symbolic link, and now we need to read the actual file.

                DuxCLI.logger.debug("getting rawpath for link target");
                String rawPathTarget = c.args[1];
                DuxCLI.logger.debug("getting path from this rawpath: {} for link target", rawPathTarget);
                String pathTarget = rawPathTarget.substring(1, rawPathTarget.length() - 1);
                DuxCLI.logger.debug("got path: {} for link target", pathTarget);

                Path pTarget = Paths.get(pathTarget).normalize();
                // if we can't or don't want to hash the target, then don't include this symbolic link.
                if ((p =canHashPath(pTarget, blacklist, includeProjDir)) != null) {
                    links.put(p, pTarget);
                }

            }
        }

        DuxCLI.logger.debug("completed recording of calls");
    }

    /**
     * null return indicates failure. You must check the return value.
     */
    private Path canHashPath(Path p, DuxTraceBlacklist blacklist, boolean includeProjDir) throws IOException{

        // check for blacklisted files
        if (blacklist.contains(p)) {
            DuxCLI.logger.debug("{} is blacklisted, ignoring", p.toString());
            return null;
        }

        // we only want to hash regular files
        DuxCLI.logger.debug("checking if file is a regular file");
        if (!Files.isRegularFile(p)) {
            DuxCLI.logger.debug("{} is not a regular file", p.toString());
            return null;
        }

        Path currentDir = Paths.get(".").toAbsolutePath().normalize();

        // disregard project files (heuristic: they're not dependencies)
        if (!includeProjDir && pathInSubdirectory(currentDir, p)) {
            DuxCLI.logger.debug("{} is in the current project directory", p.toString());
            return null;
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
            return null;
        }

        // We need to save both variables that include a file that's being referenced
        // and those that include a directory that contains such a file.
        // This is a heuristic, but the latter is useful for e.g. the PATH

        saveVarFromPath(p, p);
        saveVarFromPath(p.toAbsolutePath().normalize(), p);

        saveVarFromPath(p.getParent(), p.getParent());
        saveVarFromPath(p.toAbsolutePath().normalize().getParent(), p.getParent());

        return p;
    }

    private void saveVarFromPath(Path p, Path toSave) {
        // does this path contain an environment variable that we want to save the value of?

        DuxCLI.logger.debug("checking whether {} is a key into the envPaths map", p);

        if (envPaths.containsKey(p)) {
            String name = envPaths.get(p);
            DuxCLI.logger.debug("it is - to env var {}", name);
            DuxConfigurationVar var = new DuxConfigurationVar(name, toSave.toString(), envVarsWithPathSep.contains(name));
            varsToSave.add(var);
        }
    }
}
