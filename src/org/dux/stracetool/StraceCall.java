package org.dux.stracetool;

import java.util.Arrays;

/**
 * Object that stores information parsed from strace calls
 */
public class StraceCall {
    public final String call;
    public final String[] args;
    public final boolean knownReturn;
    public final int returnValue;        // for strace
    public final String returnMessage;   // for procmon

    public StraceCall(String call, String[] args, int returnValue) {
        this.call = call;
        this.args = Arrays.<String>copyOf(args, args.length);
        this.knownReturn = true;
        this.returnValue = returnValue;
        this.returnMessage = null;
    }

    public StraceCall(String call, String[] args, String returnMessage) {
        this.call = call;
        this.args = Arrays.<String>copyOf(args, args.length);
        this.knownReturn = true;
        this.returnValue = 0;
        this.returnMessage = returnMessage;
    }

    public StraceCall(String call, String[] args) {
        this.call = call;
        this.args = Arrays.<String>copyOf(args, args.length);
        this.knownReturn = false;
        this.returnValue = 0;
        this.returnMessage = null;
    }

    @Override
    public String toString() {
        return "StraceCall{"
                + "call=\"" + call + "\", "
                + "args=\"" + Arrays.toString(args) + "\", "
                + "knownReturn=" + knownReturn + ", "
                + "returnValue=" + returnValue + ", "
                + "returnMessage=\"" + returnMessage + "\"}";
    }
}
