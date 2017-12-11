package org.dux.cli;

import java.io.Serializable;

/**
 * Data class representing an environment variable that Dux believes is relevant to a build.
 */
public class DuxConfigurationVar implements Serializable {
    private final String name;
    private final String value;
    private final boolean appendWithPathSeparator;

    /**
     * @param name the name of the environment variable, e.g. PATH
     * @param value the value to include in the variable
     * @param appendWithPathSeparator should this value be appended to the variable with a path separator?
     */
    public DuxConfigurationVar(String name, String value, boolean appendWithPathSeparator) {
        this.name = name;
        this.value = value;
        this.appendWithPathSeparator = appendWithPathSeparator;
    }

    @Override
    public String toString() {
        return "DuxConfigurationVar{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", appendWithPathSeparator=" + appendWithPathSeparator +
                '}';
    }

    /**
     * Sets this environment variable in the environment of a process under construction.
     */
    public void set(ProcessBuilder pb) {
       if (appendWithPathSeparator) {
           String current = pb.environment().get(name);
           if (current != null) {
               pb.environment().put(name, value + System.getProperty("path.separator") + current);
           } else {
               pb.environment().put(name, value);
           }
       } else {
           pb.environment().put(name, value);
       }
    }
}
