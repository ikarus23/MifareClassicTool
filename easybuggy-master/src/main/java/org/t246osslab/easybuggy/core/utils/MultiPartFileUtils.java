package org.t246osslab.easybuggy.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Part;
import java.io.*;

/**
 * Utility class to handle multi part files.
 */
public final class MultiPartFileUtils {

    private static final Logger log = LoggerFactory.getLogger(MultiPartFileUtils.class);

    // squid:S1118: Utility classes should not have public constructors
    private MultiPartFileUtils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Write uploaded file to the given path.
     *
     * @param part A part or form item that was received within a <code>multipart/form-data</code> POST request.
     * @param savePath Path to save an uploaded file.
     * @param fileName The uploaded file name.
     */
    public static boolean writeFile(Part part, String savePath, String fileName) throws IOException {
        boolean isConverted = false;
        OutputStream out = null;
        InputStream in = null;
        try {
            out = new FileOutputStream(savePath + File.separator + fileName);
            in = part.getInputStream();
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
        } catch (FileNotFoundException e) {
            // Ignore because file already exists (converted and Windows locked the file)
            log.debug("Exception occurs: ", e);
            isConverted = true;
        } finally {
            Closer.close(out, in);
        }
        return isConverted;
    }


    /**
     * Retrieves file name of a upload part from its HTTP header
     *
     * @param part A part or form item that was received within a <code>multipart/form-data</code> POST request.
     */
    public static String getFileName(final Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
}
