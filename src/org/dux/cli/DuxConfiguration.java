package org.dux.cli;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An object representing a Dux configuration file.
 * These files are stored as binary objects, and so
 * must implement java.io.Serializable.
 *
 * Each DuxConfiguration contains the information
 * about dependencies needed to build a project.
 *
 * DuxConfiguration_s also include metadata, such
 * as the project name (optional), and information
 * about when it was created.
 */
public class DuxConfiguration implements Serializable {
    @Nullable final String projectName;
    final ZonedDateTime creationTime;
    final List<DuxConfigurationEntry> entries;

    public DuxConfiguration(@Nullable String projectName) {
        this.projectName = projectName;
        this.creationTime = ZonedDateTime.now();
        this.entries = new ArrayList<>();
    }

    public void add(DuxConfigurationEntry entry) {
        entries.add(entry);
    }
}
