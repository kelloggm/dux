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

/**
 * Create with the Tracer.Builder class.
 */

public class Tracer {
    public static Logger logger;
    static {
        logger = (Logger) LoggerFactory.getLogger(Tracer.class);
        logger.setLevel(Level.INFO);
    }

    private static final String[] FILTER_ARGS = {
            "-e", "trace=open,execve,readlink,fstat,stat,lstat"
    };

    private List<String> args;

    public static class Builder {
        // Required parameters
        private final String fileName;
        private final List<String> traceCommand;

        // Optional parameters
        private boolean traceSubprocesses = false;
        private boolean filterCalls = false;

        public Builder(String fileName, List<String> traceCommand) {
            // argument checking
            if (fileName == null || traceCommand == null) {
                throw new NullPointerException("Arguments cannot be null");
            }
            if (fileName.length() == 0) {
                throw new IllegalArgumentException("File name cannot be empty");
            }
            if (traceCommand.size() == 0) {
                throw new IllegalArgumentException("Trace command cannot be empty");
            }
            if (fileName.contains(" ")) {
                // spaces could mean they are passing arbitrary arguments to
                // strace or procmon; disallow for now
                throw new IllegalArgumentException("No spaces allowed in filename");
            }

            this.fileName = fileName;
            // TODO How to sanitize this?
            this.traceCommand = traceCommand;
        }

        public Builder traceSubprocesses() {
            traceSubprocesses = true;
            return this;
        }

        // For now, can only filter with the strace flag
        // "-e trace=open,execve,readlink,fstat,stat,lstat"
        public Builder filterCalls() {
            filterCalls = true;
            return this;
        }

        public Tracer build() {
            return new Tracer(this);
        }
    }

    private Tracer(Builder builder) {
        String os = System.getProperty("os.name");
        args = new ArrayList<>();
        if (os.startsWith("Linux")) {
            args.add("strace");
            args.add("-o");
            args.add(builder.fileName);
            if (builder.filterCalls) {
                args.add("-f");
            }
            if (builder.traceSubprocesses) {
                args.addAll(Arrays.asList(FILTER_ARGS));
            }
        } else if (os.startsWith("Windows")) {
            args.add(0, "cmd");
            args.add(1, "/c");
            // TODO Actually use traceSubprocesses/filterCalls on Windows somehow
        } else {
            throw new UnsupportedOperationException("Unsupported OS");
        }
        args.addAll(builder.traceCommand);

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
        String os = System.getProperty("os.name");
        if (os.startsWith("Linux")) {
            Process proc = rt.exec((String[]) args.toArray(new String[args.size()]));
            Tracer.StreamGobbler outputGobbler = new Tracer.StreamGobbler(proc.getInputStream());
            Tracer.logger.debug("waiting for build to terminate");
            outputGobbler.start();
            proc.waitFor();
        } else if (os.startsWith("Windows")) {
            // turn on Process Monitor
            Process proc1 = rt.exec("cmd /c start_trace.bat");
            proc1.waitFor();

            // run actual command to trace
            Process proc = rt.exec((String[]) args.toArray(new String[args.size()]));
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
