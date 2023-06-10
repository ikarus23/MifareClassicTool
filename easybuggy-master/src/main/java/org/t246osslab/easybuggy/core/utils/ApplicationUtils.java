package org.t246osslab.easybuggy.core.utils;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to provide application properties.
 */
public final class ApplicationUtils {

    private static final Logger log = LoggerFactory.getLogger(ApplicationUtils.class);

    // default database url: derby in-memory
    private static String databaseURL = "jdbc:derby:memory:demo;create=true";

    // default database url: org.apache.derby.jdbc.EmbeddedDriver
    private static String databaseDriver = "org.apache.derby.jdbc.EmbeddedDriver";

    // default account lock time: 3600000 (1 hour)
    private static long accountLockTime = 3600000;

    // default account lock limit count: 10
    private static int accountLockCount = 10;

    // default SMTP host: null
    private static String smtpHost = null;

    // default SMTP port: null
    private static String smtpPort = null;

    // default SMTP auth: false
    private static String smtpAuth = "false";

    // default SMTP starttls enable: false
    private static String smtpStarttlsEnable = "false";

    // default SMTP user: null
    private static String smtpUser = null;

    // default SMTP password: null
    private static String smtpPass = null;

    // default administrator's mail address: null
    private static String adminAddress = null;

    static {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("application");
            databaseURL = getProperty(bundle, "database.url", databaseURL);
            databaseDriver = getProperty(bundle, "database.driver", databaseDriver);
            accountLockTime = getProperty(bundle, "account.lock.time", accountLockTime);
            accountLockCount = getProperty(bundle, "account.lock.count", accountLockCount);
            smtpHost = getProperty(bundle, "mail.smtp.host", smtpHost);
            smtpPort = getProperty(bundle, "mail.smtp.port", smtpPort);
            smtpAuth = getProperty(bundle, "mail.smtp.auth", smtpAuth);
            smtpStarttlsEnable = getProperty(bundle, "mail.smtp.starttls.enable", smtpStarttlsEnable);
            smtpUser = getProperty(bundle, "mail.user", smtpUser);
            smtpPass = getProperty(bundle, "mail.password", smtpPass);
            adminAddress = getProperty(bundle, "mail.admin.address", adminAddress);
        } catch (MissingResourceException e) {
            log.error("MissingResourceException occurs: ", e);
        }
    }

    // squid:S1118: Utility classes should not have public constructors
    private ApplicationUtils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Return a Database URL of EasyBuggy.
     * 
     * @return Database URL of EasyBuggy
     */
    public static String getDatabaseURL() {
        return databaseURL;
    }

    /**
     * Return a Database driver of EasyBuggy.
     * 
     * @return Database driver of EasyBuggy
     */
    public static String getDatabaseDriver() {
        return databaseDriver;
    }

    /**
     * Return the account lock time.
     * 
     * @return Account lock time
     */
    public static long getAccountLockTime() {
        return accountLockTime;
    }

    /**
     * Return the account lock count.
     * 
     * @return Account lock count
     */
    public static int getAccountLockCount() {
        return accountLockCount;
    }

    /**
     * Return the SMTP host.
     * 
     * @return SMTP host
     */
    public static String getSmtpHost() {
        return smtpHost;
    }

    /**
     * Return the SMTP port.
     * 
     * @return SMTP port
     */
    public static String getSmtpPort() {
        return smtpPort;
    }

    /**
     * Return the SMTP auth.
     * 
     * @return SMTP auth
     */
    public static String getSmtpAuth() {
        return smtpAuth;
    }

    /**
     * Return the SMTP start TLS enable.
     * 
     * @return SMTP start TLS enable
     */
    public static String getSmtpStarttlsEnable() {
        return smtpStarttlsEnable;
    }

    /**
     * Return the SMTP user.
     * 
     * @return SMTP user
     */
    public static String getSmtpUser() {
        return smtpUser;
    }

    /**
     * Return the SMTP password.
     * 
     * @return SMTP password
     */
    public static String getSmtpPass() {
        return smtpPass;
    }

    /**
     * Return the Administrator's mail address
     * 
     * @return Administrator's mail address
     */
    public static String getAdminAddress() {
        return adminAddress;
    }

    private static String getProperty(ResourceBundle bundle, String key, String defaultValue) {
        try {
            return getProperty(bundle, key);
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
        return defaultValue;
    }
    
    private static int getProperty(ResourceBundle bundle, String key, int defaultValue) {
        try {
            return Integer.parseInt(getProperty(bundle, key));
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
        return defaultValue;
    }

    private static long getProperty(ResourceBundle bundle, String key, long defaultValue) {
        try {
            return Long.parseLong(getProperty(bundle, key));
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
        return defaultValue;
    }

    private static String getProperty(ResourceBundle bundle, String key) {
        return System.getProperty(key) != null ? System.getProperty(key) : bundle.getString(key);
    }
}
