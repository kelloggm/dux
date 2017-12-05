package org.dux.cli;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A reader for Dux configuration files.
 *
 * To get a DuxConfiguration object:
 * {@code DuxConfiguration config = DuxConfigurationIO.read(config_file)},
 * where {@code config_file} is a String that contains a path to a file with
 * Dux configuration code (i.e. ends in {@code .dux}).
 */
public class DuxConfigurationIO {
    public static @Nullable DuxConfiguration read(final String filePath) {
        final DuxConfiguration config;
        try {
            FileInputStream fileIn = new FileInputStream(filePath);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            config = (DuxConfiguration) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return null;
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
            return null;
        }
        return config;
    }

    public static void write(final String filePath, final DuxConfiguration config) {
        try {
            FileOutputStream fileOut =
                    new FileOutputStream(filePath);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(config);
            out.close();
            fileOut.close();
            DuxCLI.logger.debug("saved configuration to {}", filePath);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }
}
