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
            defaultValue = "true"
    )
    public boolean help;
}
