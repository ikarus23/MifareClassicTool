package org.t246osslab.easybuggy.core.utils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
/**
 * A utility class for sending e-mail message with attachment.
 *
 */
public class EmailUtils {

    private static final Logger log = LoggerFactory.getLogger(EmailUtils.class);

    // squid:S1118: Utility classes should not have public constructors
    private EmailUtils() {
        throw new IllegalAccessError("Utility class");
    }
    
    /**
     * Sends an e-mail message from a SMTP host with a list of attached files.
     * 
     * @param subject Mail subject
     * @param message Mail content
     * @param attachedFiles Attached files
     */
    public static void sendEmailWithAttachment(String subject, String message, List<File> attachedFiles)
            throws MessagingException {
        // sets SMTP server properties
        Properties properties = new Properties();
        properties.put("mail.smtp.host", ApplicationUtils.getSmtpHost());
        properties.put("mail.smtp.port", ApplicationUtils.getSmtpPort());
        properties.put("mail.smtp.auth", ApplicationUtils.getSmtpAuth());
        properties.put("mail.smtp.starttls.enable", ApplicationUtils.getSmtpStarttlsEnable());
        properties.put("mail.user", ApplicationUtils.getSmtpUser());
        properties.put("mail.password", ApplicationUtils.getSmtpPass());
 
        // creates a new session with an authenticator
        Authenticator auth = null;
        if (!StringUtils.isBlank(ApplicationUtils.getSmtpUser()) && !StringUtils.isBlank(ApplicationUtils.getSmtpPass())) {
            auth = new Authenticator() {
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(ApplicationUtils.getSmtpUser(), ApplicationUtils.getSmtpPass());
                }
            };
        }
        Session session = Session.getInstance(properties, auth);

        // creates a new e-mail message
        Message msg = new MimeMessage(session);
        if (!StringUtils.isBlank(ApplicationUtils.getSmtpUser())){
            msg.setFrom(new InternetAddress(ApplicationUtils.getSmtpUser()));
        }
        InternetAddress[] toAddresses = { new InternetAddress(ApplicationUtils.getAdminAddress()) };
        msg.setRecipients(Message.RecipientType.TO, toAddresses);
        ((MimeMessage)msg).setSubject(subject,"UTF-8");
        msg.setSentDate(new Date());
        msg.setHeader("Content-Transfer-Encoding", "7bit"); 
 
        // creates message part
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(message, "text/html;charset=UTF-8");
 
        // creates multi-part
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
 
        // adds attachments
        if (attachedFiles != null && !attachedFiles.isEmpty()) {
            for (File aFile : attachedFiles) {
                MimeBodyPart attachPart = new MimeBodyPart();
                 try {
                    attachPart.attachFile(aFile);
                } catch (IOException e) {
                    log.error("IOException occurs: ", e);
                }
                 multipart.addBodyPart(attachPart);
            }
        }
 
        // sets the multi-part as e-mail's content
        msg.setContent(multipart);
 
        // sends the e-mail
        Transport.send(msg);
    }


    /**
     * Validate the given string as E-mail address.
     * 
     * @param mailAddress Mail address
     */
    public static boolean isValidEmailAddress(String mailAddress) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(mailAddress);
            emailAddr.validate();
        } catch (AddressException e) {
            log.debug("Mail address is invalid: " + mailAddress, e);
            result = false;
        }
        return result;
    }
}