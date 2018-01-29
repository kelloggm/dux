package org.dux.stracetool;

import org.checkerframework.checker.nullness.qual.Nullable;
// TODO What to do about DuxCLI references (primarily debug statements)?
import org.dux.cli.DuxCLI;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class StraceParser {
    public static List<StraceCall> parse(String path)
            throws IOException, FileNotFoundException {
        String os = System.getProperty("os.name");
        StraceParser sp = null;
        if (os.startsWith("Linux")) {
            sp = new LinuxStraceParser();
        } else if (os.startsWith("Windows")) {
            // TODO implement
        } else {
            return null;
        }
        return sp.parseFile(path);
    }

    protected List<StraceCall> parseFile(String path)
            throws IOException, FileNotFoundException {
        ArrayList<StraceCall> calls = new ArrayList<>();

        DuxCLI.logger.debug("creating a file reader and a buffered reader");

        try (FileReader fr = new FileReader(path);
             BufferedReader br = new BufferedReader(fr)) {
            while (br.ready()) {
                DuxCLI.logger.debug("buffer is ready");
                String line = br.readLine();
                DuxCLI.logger.debug("read this line: {}", line);
                StraceCall call = parseLine(line);
                if (call == null) {
                    continue;
                }

                calls.add(call);
            }
        }

        return calls;
    }

    protected abstract @Nullable StraceCall parseLine(String line);
}