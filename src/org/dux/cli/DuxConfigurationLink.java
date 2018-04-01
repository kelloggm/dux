package org.dux.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Data class representing a symbolic link.
 */
public class DuxConfigurationLink implements Serializable {

    private static Logger LOGGER = (Logger) LoggerFactory.getLogger(DuxConfigurationLink.class);


    public Path getLink() {
        return link;
    }

    public Path getTarget() {
        return target;
    }

    private final Path link, target;

    public DuxConfigurationLink(Path link, Path target) {
        this.link = link;
        this.target = target;
    }

    /**
     * Creates this link on the filesystem
     */
    public void create() {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException x) {
            LOGGER.error("cannot create link: {}", x);
        } catch (UnsupportedOperationException x) {
            // Some file systems do not support symbolic links.
            LOGGER.error("this filesystem does not support symbolic links: {}", x);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DuxConfigurationLink that = (DuxConfigurationLink) o;

        if (!getLink().equals(that.getLink())) return false;
        return getTarget().equals(that.getTarget());

    }

    @Override
    public int hashCode() {
        int result = getLink().hashCode();
        result = 31 * result + getTarget().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DuxConfigurationLink{" +
                "link=" + link +
                ", target=" + target +
                '}';
    }
}
