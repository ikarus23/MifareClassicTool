package org.t246osslab.easybuggy.core.utils;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to safely close all Closeable objects.
 */
public final class Closer {

    private static final Logger log = LoggerFactory.getLogger(Closer.class);

    // squid:S1118: Utility classes should not have public constructors
    private Closer() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Close a Connection object.
     * 
     * @param conn Connection object.
     */
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("IOException occurs: ", e);
            }
        }
    }

    /**
     * Close a Statement object.
     * 
     * @param stmt Statement object.
     */
    public static void close(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.error("IOException occurs: ", e);
            }
        }
    }

    /**
     * Close a ResultSet object.
     * 
     * @param rs ResultSet object.
     */
    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("IOException occurs: ", e);
            }
        }
    }

    /**
     * Close all Closeable objects.
     * 
     * @param closeables Closeable objects.
     */
    public static void close(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                try {
                    if (closeable != null) {
                        closeable.close();
                    }
                } catch (IOException e) {
                    log.error("IOException occurs: ", e);
                }
            }
        }
    }
    
// for jdk 7 or later
//    /**
//     * Close all Closeable objects.
//     *
//     * @param closeables Closeable objects.
//     */
//    public static void close(AutoCloseable... closeables) {
//        if (closeables != null) {
//            for (AutoCloseable closeable : closeables) {
//                try {
//                    if(closeable != null){
//                        closeable.close();
//                    }
//                } catch (IOException e) {
//                    log.error("IOException occurs: ", e);
//                } catch (Exception e) {
//                    log.error("Exception occurs: ", e);
//                }
//            }
//        }
//    }
}
