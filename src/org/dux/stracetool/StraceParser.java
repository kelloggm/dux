package org.dux.stracetool;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class StraceParser {

    private static Logger LOGGER = (Logger) LoggerFactory.getLogger(StraceParser.class);

    public static List<StraceCall> parse(String path)
            throws IOException, FileNotFoundException {
        String os = System.getProperty("os.name");
        StraceParser sp = null;
        if (os.startsWith("Linux")) {
            sp = new LinuxStraceParser();
        } else if (os.startsWith("Windows")) {
            throw new UnsupportedOperationException("Unsupported OS");
        } else {
            throw new UnsupportedOperationException("Unsupported OS");
        }
        return sp.parseFile(path);
    }

    protected List<StraceCall> parseFile(String path)
            throws IOException, FileNotFoundException {
        ArrayList<StraceCall> calls = new ArrayList<>();

        LOGGER.debug("creating a file reader and a buffered reader");

        try (FileReader fr = new FileReader(path);
             BufferedReader br = new BufferedReader(fr)) {
            while (br.ready()) {
                LOGGER.debug("buffer is ready");
                String line = br.readLine();
                LOGGER.debug("read this line: {}", line);
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