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
            name = "verbose",
            abbrev = 'v',
            help = "Enable debuggin mode. Prints to stderr.",
            defaultValue = "false"
    )
    public boolean debug;

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
}
