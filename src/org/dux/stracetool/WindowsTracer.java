package org.dux.stracetool;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Kernel32;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WindowsTracer extends Tracer {
    private List<String> args;

    // Windows only; already incorporated into args list on Linux
    private String fileName;
    private boolean traceSubprocesses;
    private boolean filterCalls;

    public WindowsTracer(Builder builder) {
        args = new ArrayList<>();
        fileName = builder.getFileName();
        traceSubprocesses = builder.isTraceSubprocesses();
        filterCalls = builder.isFilterCalls();
        args.addAll(builder.getTraceCommand());
    }

    public void trace() throws IOException, InterruptedException {
        Tracer.logger.debug("beginning a trace, getting runtime");
        Runtime rt = Runtime.getRuntime();
        Tracer.logger.debug("runtime acquired, executing program");

        // turn on Process Monitor
        Process proc1 = rt.exec("cmd /c src\\org\\dux\\stracetool\\start_trace.bat");
        proc1.waitFor();

        // run actual command to trace
        Process proc = rt.exec((String[]) args.toArray(new String[args.size()]));
        Tracer.StreamGobbler outputGobbler = new Tracer.StreamGobbler(proc.getInputStream());
        Tracer.logger.debug("waiting for build to terminate");
        outputGobbler.start();
        // TODO Replace with Java 9/10 Process.pid() once those become more mainstream
        int myPid = getPid(proc);
        proc.waitFor();

        // turn off Process Monitor
        Process proc2 = rt.exec("cmd /c src\\org\\dux\\stracetool\\end_trace.bat");
        proc2.waitFor();

        // need to manually filter after the trace if on Windows -- SLOW
        try {
            Set<Integer> parentPids = new HashSet<>();
            if (traceSubprocesses) {
                parentPids = findParentPids(myPid, "strace.csv");
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
                if (parentPids.contains(Integer.parseInt(parts[6])) ||
                        Integer.parseInt(parts[2]) == myPid) {
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
                        // TODO: what to do about readlink?
                    }
                    out.println(line);
                }
            }
            fr.close();
            File f = new File("strace.csv");
            f.delete();
        } catch (IOException ioe) {
            // won't happen; we just created the file we are opening
            Tracer.logger.debug("Tracer.java just created file strace.csv " +
                    "in end_trace.bat but now cannot open it...");
            ioe.printStackTrace();
        }
    }

    /**
     * Returns the given ProcMon line split on "," or null if it is a line to
     * skip (e.g. if it is a call from the Process Monitor executable itself,
     * or is the "schema" line in the Process Monitor file).
     *
     * @param line A CSV line of output from Process Monitor.
     * @return A String[] of the given Process Monitor output line split on ",".
     */
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
        return parts;
    }

    /**
     * Returns a set of pids that could be valid parent process pids for the
     * process event data generated in ProcMon CSV log filename.
     *
     * @param startingPid The PID for which to find parent PIDs (root process).
     * @param filename The Process Monitor CSV log file name to scan for parent PIDs.
     * @return A set of valid parent PIDs for all subprocesses of the given process.
     * @throws IOException if filename is invalid.
     */
    private Set<Integer> findParentPids(int startingPid, String filename)
            throws IOException {
        // parse out pairs of pids and parent pids from filename
        Set<Integer> parentPids = new HashSet<>();
        parentPids.add(startingPid);
        List<int[]> pairs = new ArrayList<int[]>();
        FileReader fr = new FileReader(filename);
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
            start_size = parentPids.size();
            for (int[] pair : pairs) {
                if (parentPids.contains(pair[1])) {
                    parentPids.add(pair[0]);
                }
            }
            end_size = parentPids.size();
        }
        return parentPids;
    }

    /**
     * Returns the process ID of the given Process (WINDOWS-DEPENDENT!!).
     *
     * @param proc The process to find the PID of.
     * @return The PID of the given process.
     */
    private int getPid(Process proc) {
        // EXTREMELY BAD; use JNA/Reflection Windows-dependent hack to get pid
        // https://stackoverflow.com/questions/4750470/how-to-get-pid-of
        // -process-ive-just-started-within-java-program
        int pid;
        try {
            Field f = proc.getClass().getDeclaredField("handle");
            f.setAccessible(true);
            long handl = f.getLong(proc);
            Kernel32 kernel = Kernel32.INSTANCE;
            WinNT.HANDLE hand = new WinNT.HANDLE();
            hand.setPointer(Pointer.createConstant(handl));
            pid = kernel.GetProcessId(hand);
            f.setAccessible(false);
            return pid;
        } catch (IllegalAccessException iae) {
            Tracer.logger.debug("Windows pid access hack didn't work...");
        } catch (NoSuchFieldException nsfe) {
            Tracer.logger.debug("Windows pid access hack didn't work...");
        }
        return -1;
    }
}
