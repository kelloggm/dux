package org.dux.stracetool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LinuxTracer extends Tracer {
    private static final String[] FILTER_ARGS = {
            "-e", "trace=open,execve,readlink,fstat,stat,lstat"
    };

    private List<String> args;

    public LinuxTracer(Builder builder) {
        args = new ArrayList<>();
        args.add("strace");
        args.add("-o");
        args.add(builder.getFileName());
        if (builder.isTraceSubprocesses()) {
            args.add("-f");
        }
        if (builder.isFilterCalls()) {
            args.addAll(Arrays.asList(FILTER_ARGS));
        }
        args.addAll(builder.getTraceCommand());
    }

    public void trace() throws IOException, InterruptedException {
        Tracer.logger.debug("beginning a trace, getting runtime");
        Runtime rt = Runtime.getRuntime();
        Tracer.logger.debug("runtime acquired, executing program");
        Process proc = rt.exec((String[]) args.toArray(new String[args.size()]));
        Tracer.StreamGobbler outputGobbler = new Tracer.StreamGobbler(proc.getInputStream());
        Tracer.logger.debug("waiting for build to terminate");
        outputGobbler.start();
        proc.waitFor();
    }
}
