package org.dux.stracetool;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tracer {

    public static Logger logger;
    static {
        logger = (Logger) LoggerFactory.getLogger(Tracer.class);
        logger.setLevel(Level.INFO);
    }

    // TODO Allow for arbitrary temp file names (need to sanitize in constructor)
    private static final String TMP_FILE = "trace.out";
    private List<String> args;

    private static final String[] STRACE_CALL = {
            "strace",
            "-o", TMP_FILE               // write to tmp file
    };
    
    public Tracer(List<String> args) {
        // TODO Sanitize args
        String os = System.getProperty("os.name");
        List<String> argList;
        if (os.startsWith("Linux")) {
            argList = new ArrayList<>(Arrays.asList(STRACE_CALL));
        } else if (os.startsWith("Windows")) {
            // "args" is purely the command to trace (e.g. "make clean")
            argList = new ArrayList<>();
        } else {
            throw new UnsupportedOperationException("Unsupported OS");
        }
        argList.addAll(args);
        this.args = argList;
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
        Tracer.logger.debug("beginning a trace, getting runtime");
        Runtime rt = Runtime.getRuntime();
        Tracer.logger.debug("runtime acquired, executing program");
        System.out.println(args);
        String os = System.getProperty("os.name");
        if (os.startsWith("Linux")) {
            Process proc = rt.exec((String[]) args.toArray());
            Tracer.StreamGobbler outputGobbler = new Tracer.StreamGobbler(proc.getInputStream());
            Tracer.logger.debug("waiting for build to terminate");
            outputGobbler.start();
            proc.waitFor();
        } else if (os.startsWith("Windows")) {
            // turn on Process Monitor
            Process proc1 = rt.exec("cmd /c start_trace.bat");
            proc1.waitFor();

            // run actual command to trace
            args.add(0, "cmd");
            args.add(1, "/c");
            String[] fullArgs = (String[]) args.toArray(new String[args.size()]);
            Process proc = rt.exec(fullArgs);
            Tracer.StreamGobbler outputGobbler = new Tracer.StreamGobbler(proc.getInputStream());
            Tracer.logger.debug("waiting for build to terminate");
            outputGobbler.start();
            proc.waitFor();

            // turn off Process Monitor
            Process proc2 = rt.exec("cmd /c end_trace.bat");
            proc2.waitFor();
        } else {
            throw new UnsupportedOperationException("Unsupported OS");
        }
    }
}
