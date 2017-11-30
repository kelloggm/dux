package org.dux.cli;

import com.google.common.hash.HashCode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.dux.backingstore.DuxBackingStore;

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

    public DuxConfigurationEntry(@Nullable final String displayName, final HashCode hashCode,
                                 final boolean isRelativePath, final File path) {
        this.displayName = displayName;
        this.hashCode = hashCode;
        this.isRelativePath = isRelativePath;
        this.path = path;
    }

    /**
     * Sends this entry to a backing store, such as a google cloud storage bucket
     * @param store the backing store
     * @return whether the store succeeded
     */
    public boolean sendToBackingStore(DuxBackingStore store) {
        return store.storeFile(this.hashCode.toString(), this.path.toString());
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
