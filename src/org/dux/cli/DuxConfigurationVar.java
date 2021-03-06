package org.dux.cli;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

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
       Path absolutePath = Paths.get(value).toAbsolutePath().normalize();
       if (appendWithPathSeparator) {
           String current = pb.environment().get(name);
           if (current != null) {
               pb.environment().put(name, absolutePath.toString() + System.getProperty("path.separator") + current);
           } else {
               pb.environment().put(name, absolutePath.toString());
           }
       } else {
           pb.environment().put(name, absolutePath.toString());
       }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DuxConfigurationVar that = (DuxConfigurationVar) o;

        if (appendWithPathSeparator != that.appendWithPathSeparator) return false;
        if (!name.equals(that.name)) return false;
        return value.equals(that.value);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + (appendWithPathSeparator ? 1 : 0);
        return result;
    }
}
