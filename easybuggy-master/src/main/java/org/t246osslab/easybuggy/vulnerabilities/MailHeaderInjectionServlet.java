package org.t246osslab.easybuggy.vulnerabilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.lang.StringUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;
import org.t246osslab.easybuggy.core.utils.Closer;
import org.t246osslab.easybuggy.core.utils.EmailUtils;
import org.t246osslab.easybuggy.core.utils.MultiPartFileUtils;

/**
 * A servlet that takes message details from user and send it as a new mail through an SMTP server.
 * The mail may contain a attachment which is the file uploaded from client.
 */
@SuppressWarnings("serial")
@WebServlet("/mailheaderijct")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, // 2MB
        maxFileSize = 1024 * 1024 * 10, // 10MB
        maxRequestSize = 1024 * 1024 * 50) // 50MB
public class MailHeaderInjectionServlet extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Locale locale = req.getLocale();
        StringBuilder bodyHtml = new StringBuilder();
        bodyHtml.append(getMsg("description.send.mail", locale));
        bodyHtml.append("<br><br>");
        bodyHtml.append("<form action=\"mailheaderijct\" method=\"post\" enctype=\"multipart/form-data\">");
        bodyHtml.append("<table class=\"table table-bordered\" style=\"font-size:small;\">");
        bodyHtml.append("<tr>");
        bodyHtml.append("<td>" + getMsg("label.your.name", locale) + ":&nbsp;<br><br></td>");
        bodyHtml.append("<td><input type=\"text\" name=\"name\" size=\"50\"/><br><br></td>");
        bodyHtml.append("</tr>");
        bodyHtml.append("<tr>");
        bodyHtml.append("<td>" + getMsg("label.your.mail", locale) + ":&nbsp;<br><br></td>");
        bodyHtml.append("<td><input type=\"text\" name=\"mail\" size=\"50\"/><br><br></td>");
        bodyHtml.append("</tr>");
        bodyHtml.append("<tr>");
        bodyHtml.append("<td>" + getMsg("label.subject", locale) + ":&nbsp;<br><br></td>");
        bodyHtml.append("<td><input type=\"text\" name=\"subject\" size=\"50\"/><br><br></td>");
        bodyHtml.append("</tr>");
        bodyHtml.append("<tr>");
        bodyHtml.append("<td>" + getMsg("label.content", locale) + ":&nbsp;<br><br></td>");
        bodyHtml.append("<td><textarea rows=\"10\" cols=\"39\" name=\"content\"></textarea> <br><br></td>");
        bodyHtml.append("</tr>");
        bodyHtml.append("<tr>");
        bodyHtml.append("<td>" + getMsg("label.attach.file", locale) + ":&nbsp;<br><br></td>");
        bodyHtml.append("<td><input type=\"file\" name=\"file\" size=\"50\" /><br></td>");
        bodyHtml.append("</tr>");
        bodyHtml.append("<tr>");
        bodyHtml.append("<td colspan=\"2\" align=\"center\"><input type=\"submit\" value=\""
                + getMsg("label.submit", locale) + "\"/></td>");
        bodyHtml.append("</tr>");
        bodyHtml.append("</table>");
        bodyHtml.append("<br>");
        if (req.getAttribute("message") != null) {
            bodyHtml.append(req.getAttribute("message") + "<br><br>");
            req.setAttribute("message", null);
        }
        bodyHtml.append(getInfoMsg("msg.note.mailheaderinjection", locale));
        bodyHtml.append("</form>");
        responseToClient(req, res, getMsg("title.mailheaderinjection.page", locale), bodyHtml.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        String resultMessage = "";
        Locale locale = req.getLocale();
        List<File> uploadedFiles = saveUploadedFiles(req);

        String name = req.getParameter("name");
        String mail = req.getParameter("mail");
        String subject = req.getParameter("subject");
        String content = req.getParameter("content");
        if (StringUtils.isBlank(subject) || StringUtils.isBlank(content)) {
            resultMessage = getMsg("msg.mail.is.empty", locale);
            req.setAttribute("message", resultMessage);
            doGet(req, res);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getMsg("label.name", locale)).append(": ").append(name).append("<br>");
        sb.append(getMsg("label.mail", locale)).append(": ").append(mail).append("<br>").append("<br>");
        sb.append(getMsg("label.content", locale)).append(": ").append(content).append("<br>");
        try {
            EmailUtils.sendEmailWithAttachment(subject, sb.toString(), uploadedFiles);
            resultMessage = getMsg("msg.sent.mail", locale);
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
            resultMessage = getErrMsg("msg.unknown.exception.occur", new String[]{e.getMessage()}, locale);
        } finally {
            deleteUploadFiles(uploadedFiles);
            req.setAttribute("message", resultMessage);
            doGet(req, res);
        }
    }

    /**
     * Saves files uploaded from the client and return a list of these files which will be attached
     * to the mail message.
     */
    private List<File> saveUploadedFiles(HttpServletRequest request)
            throws IOException, ServletException {
        List<File> listFiles = new ArrayList<File>();
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            Collection<Part> multiparts = request.getParts();
            if (!multiparts.isEmpty()) {
                for (Part part : request.getParts()) {
                    // creates a file to be saved
                    String fileName = MultiPartFileUtils.getFileName(part);
                    if (StringUtils.isBlank(fileName)) {
                        // not attachment part, continue
                        continue;
                    }

                    File saveFile = new File(fileName);
                    log.debug("Uploaded file is saved on: " + saveFile.getAbsolutePath());
                    FileOutputStream outputStream = null;
                    InputStream inputStream = null;
                    try {
                        outputStream = new FileOutputStream(saveFile);
                        // saves uploaded file
                        inputStream = part.getInputStream();
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    } catch (Exception e) {
                        log.error("Exception occurs: ", e);
                    } finally {
                        Closer.close(outputStream);
                        Closer.close(inputStream);
                    }
                    listFiles.add(saveFile);
                }
            }
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
        return listFiles;
    }

    /**
     * Deletes all uploaded files, should be called after the e-mail was sent.
     */
    private void deleteUploadFiles(List<File> listFiles) {
        if (listFiles != null && !listFiles.isEmpty()) {
            for (File aFile : listFiles) {
                if (!aFile.delete()) {
                    log.debug("Cannot remove file: " + aFile);
                }
            }
        }
    }
}
