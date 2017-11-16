package org.dux.cli;

import com.google.common.hash.HashCode;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.Serializable;

/**
 * An entry in a Dux configuration file.
 *
 * One of these should be created for each
 * dependency. Each contains a hash (used
 * to identify the dependency), an expected
 * location (either relative or absolute),
 * and an optional display name.
 */
public class DuxConfigurationEntry implements Serializable {
    @Nullable final String displayName;
    final HashCode hashCode;
    final boolean isRelativePath;
    final File path;

    public DuxConfigurationEntry(@Nullable String displayName, HashCode hashCode, boolean isRelativePath, File path) {
        this.displayName = displayName;
        this.hashCode = hashCode;
        this.isRelativePath = isRelativePath;
        this.path = path;
    }

    @Override
    public String toString() {
        return "DuxConfigurationEntry{" +
                "displayName='" + displayName + '\'' +
                ", hashCode=" + hashCode +
                ", isRelativePath=" + isRelativePath +
                ", path=" + path +
                '}';
    }
}
