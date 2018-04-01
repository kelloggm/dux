package org.dux.stracetool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tracer {

    private static Logger LOGGER = (Logger) LoggerFactory.getLogger(Tracer.class);

    // TODO Allow for arbitrary temp file names (need to sanitize in constructor)
    private static final String TMP_FILE = ".dux_out";
    private String[] args;

    private static final String[] STRACE_CALL = {
            "strace",
            "-o", TMP_FILE               // write to tmp file
    };

    private static final String[] PROCMON_CALL = {
            "TODO implement"
    };

    public Tracer(List<String> args) {
        String os = System.getProperty("os.name");
        List<String> argList = null;
        if (os.startsWith("Linux")) {
            argList = new ArrayList<>(Arrays.asList(STRACE_CALL));
        } else if (os.startsWith("Windows")) {
            throw new UnsupportedOperationException("Unsupported OS");
            // argList = new ArrayList<>(Arrays.asList(PROCMON_CALL));
        } else {
            throw new UnsupportedOperationException("Unsupported OS");
        }
        argList.addAll(args);
        this.args = argList.toArray(new String[0]);
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

    public void trace() throws IOException, InterruptedException {
        LOGGER.debug("beginning a trace, getting runtime");
        Runtime rt = Runtime.getRuntime();
        LOGGER.debug("runtime acquired, executing program");
        System.out.println(Arrays.toString(args));
        Process proc = rt.exec(args);
        Tracer.StreamGobbler outputGobbler = new Tracer.StreamGobbler(proc.getInputStream());
        LOGGER.debug("waiting for build to terminate");
        outputGobbler.start();
        proc.waitFor();
    }
}
