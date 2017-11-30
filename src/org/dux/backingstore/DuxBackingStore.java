package org.dux.backingstore;

/**
 * Represents a backing store for a dux instance.
 *
 * A dux backing store needs to be able to store and retrieve files, indexed by a hash of that file.
 * Implementations of this interface should use a concrete API to interface with some external server.
 */
public interface DuxBackingStore {
    /**
     * Fetches the file hashed by key from server, and places it at target
     * @param key a hash into a dux server
     * @param target the path to which the downloaded file should be saved
     * @return whether the fetch succeeded
     */
    boolean fetchFile(String key, String target);

    /**
     * Stores the file located at filePath under the key on server
     * @param key a key into a dux server. Must be a hash of filePath
     * @param filePath the path from which the file should be read
     * @return whether the store succeeded
     */
    boolean storeFile(String key, String filePath);
}
