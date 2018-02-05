package org.dux.stracetool;

import org.checkerframework.checker.nullness.qual.Nullable;

public class WindowsStraceParser extends StraceParser {

    // e.g. "1:41:00.4573350 PM","Explorer.EXE","7572","RegQueryKey","HKCU\Software\Classes","SUCCESS","Query: Name
    @Override
    protected @Nullable StraceCall parseLine(String line) {
        Tracer.logger.debug("parsing this line: {}", line);

        char timestampFirstChar = line.charAt(1);
        if (timestampFirstChar >= '9' || timestampFirstChar <= '0') {
            // lines should begin with a timestamp in quotes, e.g. "1:41:00.4572969 PM"
            // if not, ignore this line -- it is the schema
            return null;
        }

        // TODO Filter by process name, make more robust, possibly add more attributes
        String[] parts = line.split(","); // Process Monitor outputs csv
        String[] args = {parts[4]};
        return new StraceCall(parts[3], args, parts[5]);
    }
}
