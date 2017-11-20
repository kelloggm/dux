package org.dux.cli;

/**
 * A standardized interface for printing dux debugging/verbose messages.
 */
public class DuxVerbosePrinter {

    public static boolean DEBUG;

    public static void debugPrint(String s) {
        if (DEBUG) {
            System.err.println("[DUX]: " + s);
            System.err.flush();
        }
    }
}
