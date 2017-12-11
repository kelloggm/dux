package org.dux.cli;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.dux.backingstore.DuxBackingStore;

/**
 * An object representing a Dux configuration file.
 * These files are stored as binary objects, and so
 * must implement java.io.Serializable.
 * <p>
 * Each DuxConfiguration contains the information
 * about dependencies needed to build a project.
 * <p>
 * DuxConfiguration_s also include metadata, such
 * as the project name (optional), and information
 * about when it was created.
 */
public class DuxConfiguration implements Serializable {
    @Nullable
    final String projectName;
    final List<DuxConfigurationEntry> entries;
    final List<DuxConfigurationLink> links;
    final List<DuxConfigurationVar> vars;

    public DuxConfiguration(@Nullable final String projectName) {
        this.projectName = projectName;
        this.entries = new ArrayList<>();
        this.links = new ArrayList<>();
        this.vars = new ArrayList<>();
    }

    /**
     * Adds {@code entry} to this configuration
     */
    public void add(final DuxConfigurationEntry entry) {
        entries.add(entry);
    }

    /**
     * Adds {@code entry} to this configuration
     */
    public void addLink(final DuxConfigurationLink entry) {
        links.add(entry);
    }

    /**
     * Adds {@code entry} to this configuration
     */
    public void addVar(final DuxConfigurationVar entry) {
         vars.add(entry);
    }


    /**
     * Sends all configuration entry to the backing store. Does not filter.
     *
     * @param store a backing store, such as a google cloud storage bucket
     * @return whether all entries were successfully stored
     */
    public boolean sendToBackingStore(DuxBackingStore store) {
        boolean allSucceeded = true;
        for (DuxConfigurationEntry entry : entries) {
            allSucceeded &= entry.sendToBackingStore(store);
        }
        return allSucceeded;
    }

    /**
     * Iterator over underlying entries
     */
    public Iterable<DuxConfigurationEntry> entries() {
        return entries;
    }

    public Iterable<DuxConfigurationLink> links() { return links; }

    public Iterable<DuxConfigurationVar> vars() { return vars; }

    @Override
    public String toString() {
        String result = "DuxConfiguration{" +
                "projectName='" + projectName + '\'' +
                ", entries={";

        for (DuxConfigurationEntry entry : entries) {
            result += entry + ", \n";
        }
        result = result.substring(0, result.length() - 1);
        result += "}, links={";

        for (DuxConfigurationLink link : links) {
            result += link + ", \n";
        }
        result = result.substring(0, result.length() - 1);
        result += "}, vars={";

        for (DuxConfigurationVar var : vars) {
            result += var + ", \n";
        }
        result = result.substring(0, result.length() - 1);
        result += "}";
        return result;
    }
}
