package org.dux.stracetool;

import org.checkerframework.checker.nullness.qual.Nullable;

public class WindowsStraceParser extends StraceParser {

    // e.g. "1:41:00.4573350 PM","Explorer.EXE","7572","RegQueryKey","HKCU\Software\Classes","SUCCESS","2422"
    @Override
    protected @Nullable StraceCall parseLine(String line) {
        Tracer.logger.debug("parsing this line: {}", line);

        // TODO make more robust, possibly add more attributes
        line = line.substring(1, line.length() - 1); // strip quotes
        String[] parts = line.split("\",\""); // Process Monitor outputs csv
        String target = "\"" + parts[4] + "\""; // add quotes to match strace
        String[] args = {target};
        return StraceCall.newWindowsInstance(parts[3], args, parts[5]);
    }
}
