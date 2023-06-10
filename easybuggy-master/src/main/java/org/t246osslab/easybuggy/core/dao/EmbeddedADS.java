package org.t246osslab.easybuggy.core.dao;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded Apache Directory Server.
 */
public final class EmbeddedADS {

    private static final String ROOT_PARTITION_NAME = "t246osslab";

    private static final String ROOT_DN = "dc=t246osslab,dc=org";

    private static final String PEOPLE_CONTAINER_DN = "ou=people," + ROOT_DN;

    private static final Logger log = LoggerFactory.getLogger(EmbeddedADS.class);

    /** The directory service */
    private static DirectoryService service;

    /*
     * Create an instance of EmbeddedADS and initialize it.
     */
    static {
        try {
            service = new DefaultDirectoryService();

            // Disable the ChangeLog system
            service.getChangeLog().setEnabled(false);
            service.setDenormalizeOpAttrsEnabled(true);

            // Add system partition
            Partition systemPartition;
            systemPartition = addPartition("system", ServerDNConstants.SYSTEM_DN);
            service.setSystemPartition(systemPartition);

            // Add root partition
            Partition t246osslabPartition = addPartition(ROOT_PARTITION_NAME, ROOT_DN);

            // Start up the service
            service.startup();

            // Add the root entry if it does not exist
            addRootEntry(t246osslabPartition);

            // Add the people entries
            LdapDN peopleDn = new LdapDN(PEOPLE_CONTAINER_DN);
            if (!service.getAdminSession().exists(peopleDn)) {
                ServerEntry e = service.newEntry(peopleDn);
                e.add("objectClass", "organizationalUnit");
                e.add("ou", "people");
                service.getAdminSession().add(e);
            }

            // Add sample users
            addUser("admin", "password", RandomStringUtils.randomNumeric(10));
            addUser("admin2", "pas2w0rd", RandomStringUtils.randomNumeric(10));
            addUser("admin3", "pa33word", RandomStringUtils.randomNumeric(10));
            addUser("admin4", "pathwood", RandomStringUtils.randomNumeric(10));
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }

    private static void addRootEntry(Partition t246osslabPartition) throws Exception {
        try {
            service.getAdminSession().lookup(t246osslabPartition.getSuffixDn());
        } catch (Exception e) {
            log.debug("Exception occurs: ", e);
            LdapDN dnBar = new LdapDN(ROOT_DN);
            ServerEntry entryBar = service.newEntry(dnBar);
            entryBar.add("objectClass", "dcObject", "organization");
            entryBar.add("o", ROOT_PARTITION_NAME);
            entryBar.add("dc", ROOT_PARTITION_NAME);
            service.getAdminSession().add(entryBar);
        }
    }
 
    // squid:S1118: Utility classes should not have public constructors
    private EmbeddedADS() {
        throw new IllegalAccessError("This class should not be instantiated.");
    }
    
    /**
     * Returns the admin session to connect Embedded Apache Directory Server.
     * 
     * @return The admin session
     */
    public static CoreSession getAdminSession() throws Exception{
        return service.getAdminSession();
    }

    // Add a partition to the server
    private static Partition addPartition(String partitionId, String partitionDn) throws Exception {
        // Create a new partition named
        Partition partition = new JdbmPartition();
        partition.setId(partitionId);
        partition.setSuffix(partitionDn);
        service.addPartition(partition);
        return partition;
    }

    // Add a user to the server
    private static void addUser(String username, String passwd, String secretNumber) throws Exception {
        LdapDN dn = new LdapDN("uid=" + username + "," + PEOPLE_CONTAINER_DN);
        if (!service.getAdminSession().exists(dn)) {
            ServerEntry e = service.newEntry(dn);
            e.add("objectClass", "person", "inetOrgPerson");
            e.add("uid", username);
            e.add("displayName", username);
            e.add("userPassword", passwd.getBytes());
            e.add("employeeNumber", secretNumber);
            e.add("sn", "Not use");
            e.add("cn", "Not use");
            e.add("givenName", username);
            service.getAdminSession().add(e);
        }
    }
}
