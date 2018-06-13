package org.dux.stracetool;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.List;

/**
 * A tracer object can be used to trace system calls of a specified executable
 * on Linux or Windows.
 *
 * The typical usage is to create a Tracer with the Tracer.Builder class (will
 * dispatch to either WindowsTracer or LinuxTracer depending on the current
 * OS) and then call .trace() on the tracer to begin tracing a desired executable
 * and logging its system calls to a specified output file.
 *
 * Additional options, such as filtering system calls, tracing subprocesses, etc.
 * can be configured in the builder.
 */

public abstract class Tracer {
    public static Logger logger;
    static {
        logger = (Logger) LoggerFactory.getLogger(Tracer.class);
        logger.setLevel(Level.INFO);
    }

    protected Tracer() {
        // hide this constructor
    }

    public static class Builder {
        // Required parameters
        private final String fileName;
        private final List<String> traceCommand;

        // Optional parameters
        private boolean traceSubprocesses = false;
        private boolean filterCalls = false;

        // The output file will be a CSV regardless of what you name it.
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
            // don't need to sanitize trace command because users could just
            // run this command themselves without passing it through dux
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
            String os = System.getProperty("os.name");
            if (os.startsWith("Windows")) {
                return new WindowsTracer(this);
            } else if (os.startsWith("Linux")) {
                return new LinuxTracer(this);
            } else {
                throw new UnsupportedOperationException("Unsupported OS");
            }
        }

        public String getFileName() {
            return fileName;
        }

        public List<String> getTraceCommand() {
            return traceCommand;
        }

        public boolean isTraceSubprocesses() {
            return traceSubprocesses;
        }

        public boolean isFilterCalls() {
            return filterCalls;
        }
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

    public abstract void trace() throws IOException, InterruptedException;
}
