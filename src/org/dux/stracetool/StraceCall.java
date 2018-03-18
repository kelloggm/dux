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

    // Linux constructor 1
    public StraceCall(String call, String[] args, int returnValue) {
        this.call = call;
        this.args = Arrays.<String>copyOf(args, args.length);
        this.knownReturn = true;
        this.returnValue = returnValue;
        this.returnMessage = null;
    }

    // Linux constructor 2
    public StraceCall(String call, String[] args) {
        this.call = call;
        this.args = Arrays.<String>copyOf(args, args.length);
        this.knownReturn = false;
        this.returnValue = 0;
        this.returnMessage = null;
    }

    // Windows constructor
    public StraceCall(String call, String[] args, String returnMessage) {
        this.call = call;
        this.args = Arrays.<String>copyOf(args, args.length);
        this.knownReturn = true;
        this.returnMessage = returnMessage;
        // put a best approximation of return message into return value
        if (returnMessage.equalsIgnoreCase("SUCCESS")) {
            this.returnValue = 0;
        } else {
            this.returnValue = -1;
        }
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

    public boolean isOpen() {
        return this.call.equals("open") || this.call.equals("CreateFile");
    }

    public boolean isExec() {
        return this.call.matches("exec.*") || this.call.equals("Process Create");
    }

    public boolean isReadLink() {
        return this.call.equals("readlink") || this.call.equals("CreateFile");
    }

    public boolean isStat() {
        return this.call.matches(".*stat") || this.call.equals("CreateFile");
    }

    // TODO: how to deal with readlink? Will be two separate createfile calls...
}
