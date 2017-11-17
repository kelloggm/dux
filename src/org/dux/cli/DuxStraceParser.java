package org.dux.cli;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * An object responsible for reading in an strace dump line by line and
 * extracting the calls.
 */
public class DuxStraceParser {
    // regex corresponds to "call(args) = return"
    private static final String CALL_REGEX = "\\p{Alpha}\\p{Alnum}*\\s*\\(.*\\)\\s*\\=\\s*\\d+";

    public static List<DuxStraceCall> parse(String path)
            throws IOException, FileNotFoundException {

        ArrayList<DuxStraceCall> calls = new ArrayList<>();

        try (FileReader fr = new FileReader(path);
             BufferedReader br = new BufferedReader(fr)) {
            while (br.ready()) {
                String line = br.readLine();
                DuxStraceCall call = parseLine(line);
                if (call == null) {
                    continue;
                }

                calls.add(call);
            }
        }

        return calls;
    }

    // expects args to be in the form "arg1, arg2, {possible, struct, literal...}, ..., argn)"
    private static String[] parseStraceArgs(String rawArgs) {
	int length = rawArgs.length();

	// handle the zero-args case right away (not sure it's possible in strace though)
	// zero-args is whitespace followed by close paren
	if (rawArgs.matches("\\s*\\)")) {
	    return new String[0];
	}

	ArrayList<String> args = new ArrayList<>();
	int argStart = 0;
	int nestingDepth = 0;
	for (int i = 0; i < length; i++) {
	    char c = rawArgs.charAt(i);
	    if (c == '{') { // struct literal start
		nestingDepth++;
	    }
	    if (c == '}') {
		nestingDepth--;
	    }
	    // ignore commas inside struct literals, otherwise delimit arguments at commas
	    // close paren ==> end of args, so delimit there in all cases
	    if ((c == ',' && nestingDepth == 0) || c == ')') {
		String arg = rawArgs.substring(argStart, i);
		args.add(arg.trim());
		argStart = i + 1;
	    }
	}

	return args.toArray(new String[args.size()]);
    }

    private static @Nullable DuxStraceCall parseLine(String line) {
        if (!line.matches(CALL_REGEX)) {
            return null;
        }

        // before the call, we may have "[PID ####]" so get rid of it
        String sanitized = line;
        if (line.startsWith("[pid")) {
            int close = line.indexOf(']');
            sanitized = line.substring(close + 1);
        }

        String[] values = sanitized.split("\\=");
        assert (values.length == 2);

        String lhs = values[0].trim();
        String rhs = values[1].trim();

        // split LHS on open parenthesis: the call is to the left, the args are to the right
	String[] callTokens = lhs.split("\\(");
        assert (callTokens.length == 2);
        String call = callTokens[0].trim();
	String rawArgs = callTokens[1].trim(); // leave close paren
	String[] args = parseStraceArgs(rawArgs);

        // there may be an errno after the return value; split on whitespace to ignore
        String rawReturn = rhs.split("\\s")[0];
	if (rawReturn.equals("?")) {
	    return new DuxStraceCall(call, args);
	}

	int returnValue = Integer.parseInt(rawReturn);
        return new DuxStraceCall(call, args, returnValue);
    }
}
