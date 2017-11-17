package org.dux.cli;

import java.util.Arrays;

/**
 * Object that stores information parsed from strace calls
 */
public class DuxStraceCall {
    public final String call;
    public final String[] args;
    public final boolean knownReturn;
    public final int returnValue;

    public DuxStraceCall(String call, String[] args, int returnValue) {
        this.call = call;
        this.args = Arrays.<String>copyOf(args, args.length);
	this.knownReturn = true;
        this.returnValue = returnValue;
    }

    public DuxStraceCall(String call, String[] args) {
	this.call = call;
        this.args = Arrays.<String>copyOf(args, args.length);
	this.knownReturn = false;
        this.returnValue = 0;
    }

    @Override
    public String toString() {
        return "DuxStraceCall{"
                + "call=\"" + call + "\", "
                + "args=\"" + Arrays.toString(args) + "\", "
	        + "knownReturn=" + knownReturn + ", "
                + "returnValue=" + returnValue + "}";
    }
}
