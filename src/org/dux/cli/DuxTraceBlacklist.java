package org.dux.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.HashSet;

/**
 * An object responsible for determining what files should be excluded
 * from a Dux configuration. Contains hardcoded defaults (the user can
 * choose whether to include them) and can read additional options from
 * a file.
 */
public class DuxTraceBlacklist {
    private static final String BLACKLIST_FILE_NAME = ".duxignore";
    private static final String[] DEFAULT_LIST = {
	"/proc/meminfo" // system specs
    };

    private Set<Path> paths;

    public DuxTraceBlacklist(boolean includeDefaults) throws IOException {
	paths = new HashSet<Path>();

	if (includeDefaults) {
	    // no need to normalize; Path.equals() checks if two paths
	    // point to the same file
	    for (String path : DEFAULT_LIST) {
		paths.add(Paths.get(path));
	    }
	}

	Path blacklistPath = Paths.get(BLACKLIST_FILE_NAME);
	DuxCLI.logger.debug("Checking for blacklist file");
	if (!blacklistPath.toFile().exists()) {
	    DuxCLI.logger.debug("Blacklist file does not exist");
	    return;
	}
	
	try (BufferedReader br = Files.newBufferedReader(blacklistPath)) {
	    while (br.ready()) { 
		String line = br.readLine();
		paths.add(Paths.get(line));
	    }
	}
    }

    public boolean contains(String path) {
	return paths.contains(Paths.get(path));
    }

    public boolean contains(Path p) {
	return paths.contains(p);
    }

    public boolean contains(File f) {
	return paths.contains(f.toPath());
    }
}
