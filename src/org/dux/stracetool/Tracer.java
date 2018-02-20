package org.dux.stracetool;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;

import java.lang.management.ManagementFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

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

    // Windows only; already incorporated into above args list on Linux
    private String fileName;
    private boolean traceSubprocesses;
    private boolean filterCalls;

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
            if (builder.traceSubprocesses) {
                args.add("-f");
            }
            if (builder.filterCalls) {
                args.addAll(Arrays.asList(FILTER_ARGS));
            }
        } else if (os.startsWith("Windows")) {
            args.add("cmd");
            args.add("/c");
            fileName = builder.fileName;
            traceSubprocesses = builder.traceSubprocesses;
            filterCalls = builder.filterCalls;
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
            Process proc1 = rt.exec("cmd /c src\\org\\dux\\stracetool\\start_trace.bat");
            proc1.waitFor();

            // run actual command to trace
            Process proc = rt.exec((String[]) args.toArray(new String[args.size()]));
            Tracer.StreamGobbler outputGobbler = new Tracer.StreamGobbler(proc.getInputStream());
            Tracer.logger.debug("waiting for build to terminate");
            outputGobbler.start();
            proc.waitFor();

            // turn off Process Monitor
            Process proc2 = rt.exec("cmd /c src\\org\\dux\\stracetool\\end_trace.bat");
            proc2.waitFor();

            // EXTREMELY BAD; replace with Java 9 Process.getPid() once Java 9 becomes more mainstream
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int jvmPid = Integer.parseInt(name.split("@")[0]);

            // need to manually filter after the trace if on Windows -- SLOW
            try {
                Set<Integer> parent_pids = new HashSet<>();
                parent_pids.add(jvmPid);

                if (traceSubprocesses) {
                    // parse out pairs of pids and parent pids
                    List<int[]> pairs = new ArrayList<int[]>();
                    FileReader fr = new FileReader("strace.csv");
                    BufferedReader br = new BufferedReader(fr);
                    while (br.ready()) {
                        String[] parts = skipBadLine(br.readLine());
                        if (parts == null) {
                            // got a bad line
                            continue;
                        }
                        int pid = Integer.parseInt(parts[2]);
                        int parent_pid = Integer.parseInt(parts[parts.length - 1]);
                        int[] pair = {pid, parent_pid};
                        pairs.add(pair);
                    }
                    fr.close();

                    // find pids we need to track with fixed-point algorithm
                    int start_size = 0;
                    int end_size = 1;
                    while (start_size != end_size) {
                        start_size = parent_pids.size();
                        for (int[] pair : pairs) {
                            if (parent_pids.contains(pair[1])) {
                                parent_pids.add(pair[0]);
                            }
                        }
                        end_size = parent_pids.size();
                    }
                }


                FileReader fr = new FileReader("strace.csv");
                BufferedReader br = new BufferedReader(fr);
                PrintStream out = new PrintStream(new File(fileName));
                while (br.ready()) {
                    String line = br.readLine();
                    String[] parts = skipBadLine(line);
                    if (parts == null) {
                        continue;
                    }
                    if (parent_pids.contains(Integer.parseInt(parts[6]))) {
                        if (filterCalls) {
                            // NOTE: files, symbolic links, and hard links seem
                            // to all be created or read through this procedure:
                            // QueryDirectory, CreateFile, QueryBasicInformationFile,
                            // CloseFile, CreateFile, QueryStandardInformationFile,
                            // ReadFile, CloseFile

                            // So tracing any of these calls would be sufficient
                            String call = parts[3];
                            if (!(call.equals("CreateFile") || call.equals("Process Create"))) {
                                continue;
                            }
                        }
                        out.println(line);
                    }
                }
                fr.close();
                File f = new File("strace.csv");
                f.delete();
            } catch (IOException ioe) {
                // won't happen; we just created the file we are opening
                System.out.println("You were wrong...");
                ioe.printStackTrace();
            }
        } else {
            throw new UnsupportedOperationException("Unsupported OS");
        }
    }

    // Returns the given ProcMon line split on "," or null if it is a line to skip
    private String[] skipBadLine(String line) {
        line = line.substring(1, line.length() - 1); // strip quotes

        // Process Monitor outputs csv
        String[] parts = line.split("\",\"");
        // e.g. "1:41:00.4573350 PM","Explorer.EXE","7572","RegQueryKey",
        // "HKCU\Software\Classes","SUCCESS","2422"

        char timestampFirstChar = parts[0].charAt(0);
        if (timestampFirstChar >= '9' || timestampFirstChar <= '0') {
            // lines should begin with a timestamp in quotes, e.g. "1:41:00.4572969 PM"
            // if not, ignore this line -- it is the schema
            return null;
        }

        if (parts[1].equalsIgnoreCase("Procmon.exe") ||
            parts[1].equalsIgnoreCase("Procmon64.exe")) {
            // ignore calls made by Process Monitor itself
            return null;
        }

        // TODO filter out conhost.exe/cmd.exe?

        return parts;
    }
}
