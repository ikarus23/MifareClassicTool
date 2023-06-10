package org.t246osslab.easybuggy.core.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.t246osslab.easybuggy.core.utils.ApplicationUtils;
import org.t246osslab.easybuggy.core.utils.Closer;

/**
 * Database client to provide database connections.
 */
public final class DBClient {

    private static final Logger log = LoggerFactory.getLogger(DBClient.class);

    static {
        Statement stmt = null;
        Connection conn= null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();

            // create a user table and insert sample users
            createUsersTable(stmt);

        } catch (SQLException e) {
            log.error("SQLException occurs: ", e);
        } finally {
            Closer.close(stmt);
            Closer.close(conn);
        }
    }

    // squid:S1118: Utility classes should not have public constructors
    private DBClient() {
        throw new IllegalAccessError("This class should not be instantiated.");
    }
    
    /**
     * Returns a database connection to connect a database.
     * 
     * @return A database connection
     */
    public static Connection getConnection() throws SQLException {
        final String dbDriver = ApplicationUtils.getDatabaseDriver();
        final String dbUrl = ApplicationUtils.getDatabaseURL();
        if (!StringUtils.isBlank(dbDriver)) {
            try {
                Class.forName(dbDriver);
            } catch (ClassNotFoundException e) {
                log.error("ClassNotFoundException occurs: ", e);
            }
        }
        return DriverManager.getConnection(dbUrl);
    }
    
    private static void createUsersTable(Statement stmt) throws SQLException {
        try {
            stmt.executeUpdate("drop table users");
        } catch (SQLException e) {
            // ignore exception if existing the table
            log.debug("SQLException occurs: ", e);
        }
        // create users table
        stmt.executeUpdate("create table users (id varchar(10) primary key, name varchar(30), password varchar(30), " +
                "secret varchar(100), ispublic varchar(5), phone varchar(20), mail varchar(100))");

        // insert private (invisible) user records
        stmt.executeUpdate("insert into users values ('admin','admin','password','" + RandomStringUtils.randomNumeric(10) + "','false', '', '')");
        stmt.executeUpdate("insert into users values ('admin02','admin02','pas2w0rd','" + RandomStringUtils.randomNumeric(10) + "','false', '', '')");
        stmt.executeUpdate("insert into users values ('admin03','admin03','pa33word','" + RandomStringUtils.randomNumeric(10) + "','false', '', '')");
        stmt.executeUpdate("insert into users values ('admin04','admin04','pathwood','" + RandomStringUtils.randomNumeric(10) + "','false', '', '')");
        
        // insert public (test) user records
        stmt.executeUpdate("insert into users values ('user00','Mark','password','" + RandomStringUtils.randomNumeric(10) + "','true', '', '')");
        stmt.executeUpdate("insert into users values ('user01','David','pa32w0rd','" + RandomStringUtils.randomNumeric(10) + "','true', '', '')");
        stmt.executeUpdate("insert into users values ('user02','Peter','pa23word','" + RandomStringUtils.randomNumeric(10) + "','true', '', '')");
        stmt.executeUpdate("insert into users values ('user03','James','patwired','" + RandomStringUtils.randomNumeric(10) + "','true', '', '')");
        stmt.executeUpdate("insert into users values ('user04','Benjamin','password','" + RandomStringUtils.randomNumeric(10) + "','true', '', '')");
        stmt.executeUpdate("insert into users values ('user05','Eric','pas2w0rd','" + RandomStringUtils.randomNumeric(10) + "','true', '', '')");
        stmt.executeUpdate("insert into users values ('user06','Sharon','pa3world','" + RandomStringUtils.randomNumeric(10) + "','true', '', '')");
        stmt.executeUpdate("insert into users values ('user07','Pamela','pathwood','" + RandomStringUtils.randomNumeric(10) + "','true', '', '')");
        stmt.executeUpdate("insert into users values ('user08','Jacqueline','password','" + RandomStringUtils.randomNumeric(10) + "','true', '', '')");
        stmt.executeUpdate("insert into users values ('user09','Michelle','pas2w0rd','" + RandomStringUtils.randomNumeric(10) + "','true', '', '')");
    }
}
