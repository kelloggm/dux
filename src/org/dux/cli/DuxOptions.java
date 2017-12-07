package org.dux.cli;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

/**
 * Command-line options definition for a dux instance.
 */
public class DuxOptions extends OptionsBase {

    @Option(
            name = "help",
            abbrev = 'h',
            help = "Prints usage info.",
            defaultValue = "false"
    )
    public boolean help;

    @Option(
            name = "level",
            abbrev = 'v',
            help = "Set the logging level. Allowed values: info, debug, trace, warn, error",
            defaultValue = "info"
    )
    public String level;

    @Option(
            name = "command",
            abbrev = 'c',
            help = "The command to trace.",
            defaultValue = "NOT SET"
    )
    public String command;

    @Option(
            name = "name",
            abbrev = 'n',
            help = "A name to associate with the generated config file.",
            defaultValue = "NOT SET"
    )
    public String displayName;

    @Option(
            name = "file",
            abbrev = 'f',
            help = "The name of the configuration file.",
            defaultValue = "build.dux"
    )
    public String file;

    @Option(
            name = "store",
            abbrev = 's',
            help = "The type of backing store in use. Must be one of: google",
            defaultValue = "google"
    )
    public String storeType;

    @Option(
            name = "bucket",
            abbrev = 'b',
            help = "If using a google cloud backing store (-s google), the name of the bucket to upload/download to/from.",
            defaultValue = "duxserver-test0"
    )
    public String bucketName;

    @Option(
            name = "saveconfig",
            abbrev = 'a',
            help = "If this option is set, the generated configuration file is uploaded along with the dependencies",
            defaultValue = "false"
    )
    public boolean fSaveConfig;

    @Option(
	    name = "ignoreprojdir",
	    abbrev = 'i',
	    help = "If this option is set to false, then the current directory (the project directory) is counted as a dependency",
	    defaultValue = "false"
    )
    public boolean ignoreProjDir;

    @Option(
	    name = "includedefaultblacklist",
	    help = "If this option is set to false, then Dux will not use the default blacklist while tracing (mostly low-level system files)",
	    defaultValue = "true"
    )
    public boolean includeDefaultBlacklist;
}
