package com.luxshare.fastsp.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;


/**
 * The type Io util.
 *
 * @author Luxshare
 * @version version
 */
public final class IoUtil {

    private IoUtil() {
    }

    /**
     * Close silently.
     *
     * @param closeable the closeable
     */
    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException var2) {
                var2.printStackTrace();
            }
        }
    }

    /**
     * Is file exist boolean.
     *
     * @param path the path
     * @return the boolean
     */
    public static boolean isFileExist(String path) {
        File file = new File(path);
        return file.exists();
    }
}
