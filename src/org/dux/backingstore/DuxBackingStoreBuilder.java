package org.dux.backingstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder for DuxBackingStore_s
 */
public class DuxBackingStoreBuilder {

    private static Logger LOGGER = (Logger) LoggerFactory.getLogger(DuxBackingStore.class);

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
                    LOGGER.error("tried to build a google backing store without specifying a bucket!");
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
