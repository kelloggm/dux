package org.dux.cli;

import com.google.common.hash.HashCode;

import org.dux.stracetool.StraceCall;
import org.dux.stracetool.StraceParser;
import org.dux.stracetool.Tracer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.dux.cli.DuxFileHasher.hashFile;

/**
 * An object responsible for invoking the appropriate build tracer
 * and running the build tool. It parses the traced output and dumps
 * to a config file.
 * <p>
 * Currently depends on having strace available
 */
public class DuxBuildTracer {
    private static final String TMP_FILE = ".trace.out";
    private Tracer t;

    private Map<Path, HashCode> fileHashes;

    private Map<Path, Path> links;
    private Map<Path, String> envPaths;
    private Set<String> envVarsWithPathSep;

    private Set<DuxConfigurationVar> varsToSave;

    public DuxBuildTracer(List<String> args) {
        t = new Tracer.Builder(TMP_FILE, args).
                traceSubprocesses().filterCalls().build();

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
                    // As a temp measure, save all environment variables. TODO be more precise.
                    // varsToSave.add(new DuxConfigurationVar(var.getKey(), p.toString(), values.length > 1));
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

    public void trace(boolean includeProjDir, boolean includeDefaultBlacklist) throws IOException, InterruptedException {
        DuxCLI.logger.debug("trace params: {}, {}", includeProjDir, includeDefaultBlacklist);
        t.trace();
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
        List<StraceCall> calls = StraceParser.parse(TMP_FILE);

        DuxCLI.logger.debug("created strace call list");

        for (StraceCall c : calls) {
            DuxCLI.logger.debug("recording a call: {}", c);

            // disregard everything but open, exec, and readlink calls, for now
            DuxCLI.logger.debug("checking if the call is an open or exec");
            boolean fOpenOrExec = c.isOpen() || c.isExec();
            boolean fReadlink = c.isReadLink();
            boolean fStat = c.isStat();
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

            // if this file is in the C:\Windows directory, don't need to store
            String os = System.getProperty("os.name");
            if (os.startsWith("Windows")) {
                String[] parts = path.split(":"); // ["C", "\Windows\..."]
                if (parts.length <= 1) {
                    // the file is "C:" -> ["C"]; nothing to do
                    continue;
                }
                if (parts[1].length() > 8) {
                    String pathNoVolume = parts[1];
                    if (pathNoVolume.substring(1, 8).equalsIgnoreCase("Windows")) {
                        DuxCLI.logger.debug("skipping file in Windows directory: {}", path);
                        continue;
                    }
                }
            }

            Path p = Paths.get(path).normalize();

            if (fOpenOrExec || fStat) {
                if ((p = canHashPath(p, blacklist, includeProjDir)) != null) {
                    DuxCLI.logger.debug("generating hash");
                    try {
                        HashCode hash = hashFile(path);
                        fileHashes.put(p, hash);
                    } catch (FileNotFoundException e) {
                        // must be a file created and deleted during the build
                        DuxCLI.logger.debug("skipping temp file: {}", path);
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

        // nothing to do if nothing to save! this can happen when p is root and toSave is its parent
        if (toSave == null) {
            return;
        }

        if (envPaths.containsKey(p)) {
            String name = envPaths.get(p);
            DuxCLI.logger.debug("it is - to env var {}", name);
            DuxConfigurationVar var = new DuxConfigurationVar(name, toSave.toString(), envVarsWithPathSep.contains(name));
            varsToSave.add(var);
        }
    }
}
