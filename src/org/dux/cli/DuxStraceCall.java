package org.dux.cli;

import java.util.Arrays;

/**
 * Object that stores information parsed from strace calls
 */
public class DuxStraceCall {
    public final String call;
    public final String[] args;
    public final int returnValue;

    public DuxStraceCall(String call, String[] args, int returnValue) {
	this.call = call;
	this.args = Arrays.<String>copyOf(args, args.length);
	this.returnValue = returnValue;
    }

    @Override
    public String toString() {
	return "DuxStraceCall{"
	    + "call=\"" + call + "\", "
	    + "args=\"" + Arrays.toString(args) + "\", "
	    + "returnValue=\"" + returnValue + "\"}";
    }
}
