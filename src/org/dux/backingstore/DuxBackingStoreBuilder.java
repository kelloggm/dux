package org.dux.backingstore;

import org.dux.cli.DuxVerbosePrinter;

/**
 * A builder for DuxBackingStore_s
 */
public class DuxBackingStoreBuilder {
    private String type = null;
    private String bucket = null;

    private final String GOOGLE_CLOUD_STORAGE = "google";
    private final String[] SUPPORTED_STORAGE_TYPES = new String[] {GOOGLE_CLOUD_STORAGE};

    public DuxBackingStoreBuilder type(String type) {
        this.type = type;
        return this;
    }

    public DuxBackingStoreBuilder bucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public DuxBackingStore build() {
        if (type == null) {
            throw new UnsupportedOperationException("cannot create a backing store with specifying the type");
        }
        switch (type) {
            case GOOGLE_CLOUD_STORAGE:
                if (bucket == null) {
                    DuxVerbosePrinter.debugPrint("tried to build a google backing store without specifying a bucket!");
                    return null;
                }
                return new GoogleBackingStore(bucket);
            default:
                String msg = "unsupported backing store type: " + type + ". Supported types: ";
                for (String supported : SUPPORTED_STORAGE_TYPES) {
                    msg += supported + ", ";
                }
                msg = msg.substring(0, msg.length() - 2);
                throw new UnsupportedOperationException(msg);
        }
    }
}
