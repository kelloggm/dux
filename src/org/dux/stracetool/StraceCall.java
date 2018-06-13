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

    /**
     * Factory method to return a new StraceCall object populated from the given
     * system call, arguments, and return value. Typical usage is for a line of
     * output from strace (on Linux) for a system call with a return value.
     * @param call The system call.
     * @param args The arguments to the system call.
     * @param returnValue The return value of the system call.
     * @return A new StraceCall object constructed from the given parameters.
     */
    public static StraceCall newLinuxInstanceWithReturn(String call, String[] args, int returnValue) {
        return new StraceCall(call, args, returnValue);
    }

    /**
     * Same as {@code StraceCall.newLinuxInstanceWithReturn} except used for
     * strace calls that do not have a return value.
     *
     * Factory method to return a new StraceCall object populated from the given
     * system call and arguments. Typical usage is for a line of output from
     * strace (on Linux) for a system call with no return value.
     * @param call The system call.
     * @param args The arguments to the system call.
     * @return A new StraceCall object constructed from the given parameters.
     */
    public static StraceCall newLinuxInstanceNoReturn(String call, String args[]) {
        return new StraceCall(call, args);
    }

    /**
     * Factory method to return a new StraceCall object populated from the given
     * system call, arguments, and return message. Typical usage is for a line
     * of output from Process Monitor (on Windows).
     * @param call The system call.
     * @param args The arguments to the system call.
     * @param returnMessage The return message of the system call.
     * @return A new StraceCall object constructed from the given parameters.
     */
    public static StraceCall newWindowsInstance(String call, String[] args, String returnMessage) {
        return new StraceCall(call, args, returnMessage);
    }

    private StraceCall(String call, String[] args, int returnValue) {
        this.call = call;
        this.args = Arrays.<String>copyOf(args, args.length);
        this.knownReturn = true;
        this.returnValue = returnValue;
        this.returnMessage = null;
    }

    private StraceCall(String call, String[] args) {
        this.call = call;
        this.args = Arrays.<String>copyOf(args, args.length);
        this.knownReturn = false;
        this.returnValue = 0;
        this.returnMessage = null;
    }

    private StraceCall(String call, String[] args, String returnMessage) {
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
