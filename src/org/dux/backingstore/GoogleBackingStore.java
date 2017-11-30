package org.dux.backingstore;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.dux.cli.DuxVerbosePrinter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 *  An implementation of {@link DuxBackingStore} that uses Google Cloud Storage as the backing store.
 */
public class GoogleBackingStore implements DuxBackingStore {

    /**
     * The default storage instance available, based on the credentials available to the running application.
     */
    private final Storage storage = StorageOptions.getDefaultInstance().getService();

    /**
     * The name of the bucket in Google Cloud Storage that this instance will interact with.
     */
    private final String BUCKET_NAME;

    /**
     * Creates a backing store instance
     * @param bucketName the name of a bucket in Google Cloud Storage. This instance will store and retreive files
     *                   from this bucket.
     */
    GoogleBackingStore(String bucketName) {
        BUCKET_NAME = bucketName;
    }

    /**
     * Fetches the file hashed by key from server, and places it at target
     *
     * Much of this implementation is copied verbatim from the Google Cloud Storage
     * documentation at https://cloud.google.com/storage/docs/object-basics
     *
     * @param key    a hash into a dux server
     * @param server the name of the Google Cloud Storage bucket to read from. Credentials must
     *               be available to access the requested bucket - see /credentials/README.
     * @param target the path to which the downloaded file should be saved
     * @return whether the fetch succeeded
     */
    @Override
    public boolean fetchFile(String key, String server, String target) {
        DuxVerbosePrinter.debugPrint("Fetching hash " + key + " from Google Cloud Storage");
        if (!server.equals(BUCKET_NAME)) {
            DuxVerbosePrinter.debugPrint("only supports one bucket for prototype. Please pass " + BUCKET_NAME + " as the server parameter.");
            return false;
        }
        Blob blob = storage.get(BlobId.of(key, server, null));
        if (blob == null) {
            DuxVerbosePrinter.debugPrint("GCS reports it could not find the file");
            return false;
        }
        PrintStream writeTo;
        try {
            writeTo = new PrintStream(new FileOutputStream(target));
        } catch (IOException ioe) {
            DuxVerbosePrinter.debugPrint("target file " + target + " could not be opened for writing");
            return false;
        }
        if (blob.getSize() < 1_000_000) {
            // Blob is small read all its content in one request
            byte[] content = blob.getContent();
            try {
                writeTo.write(content);
            } catch (IOException ioe) {
                DuxVerbosePrinter.debugPrint("failed to write to target file " + target);
                return false;
            }
        } else {
            // When Blob size is big or unknown use the blob's channel reader.
            try (ReadChannel reader = blob.reader()) {
                WritableByteChannel channel = Channels.newChannel(writeTo);
                ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);
                try {
                    while (reader.read(bytes) > 0) {
                        bytes.flip();
                        channel.write(bytes);
                        bytes.clear();
                    }
                } catch (IOException ioe) {
                    DuxVerbosePrinter.debugPrint("failed to write (large) to target file " + target);
                    return false;
                }
            }
        }
        writeTo.close();
        return true;
    }

    /**
     * Stores the file located at filePath under the key on server
     *
     * @param key      a key into a dux server. Must be a hash of filePath
     * @param server the name of the Google Cloud Storage bucket to read from. Credentials must
     *               be available to access the requested bucket - see /credentials/README.
     * @param filePath the path from which the file should be read
     * @return whether the store succeeded
     */
    @Override
    public boolean storeFile(String key, String server, String filePath) {
        if (!server.equals(BUCKET_NAME)) {
            DuxVerbosePrinter.debugPrint("only supports one bucket for prototype. Please pass " + BUCKET_NAME + " as the server parameter.");
            return false;
        }
        DuxVerbosePrinter.debugPrint("Storing file " + filePath + " in Google Cloud Storage");
        try {
            InputStream fileInputStream = new FileInputStream(filePath);
            Bucket bucket = storage.get(server);
            bucket.create(key, fileInputStream);
            DuxVerbosePrinter.debugPrint("file stored successfully");
            return true;
        } catch (IOException ioe) {
            DuxVerbosePrinter.debugPrint("IOException occured while trying to read file");
            return false;
        }
    }
}