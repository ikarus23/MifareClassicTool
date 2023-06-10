package org.t246osslab.easybuggy.vulnerabilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;
import org.t246osslab.easybuggy.core.utils.Closer;

@SuppressWarnings("serial")
@WebServlet("/nullbyteijct")
public class NullByteInjectionServlet extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        ServletOutputStream os = null;
        InputStream fis = null;
        Locale locale = req.getLocale();
        StringBuilder bodyHtml = new StringBuilder();
        bodyHtml.append("<p>" + getMsg("msg.download.file", locale) + "</p>");
        bodyHtml.append("<ul><li><a href=\"nullbyteijct?fileName=AdminGuide\">Admin Guide</a></li>");
        bodyHtml.append("<li><a href=\"nullbyteijct?fileName=DeveloperGuide\">Developer Guide</a></li></ul>");
        bodyHtml.append("<p>" + getInfoMsg("msg.note.nullbyteinjection", locale) + "</p>");
        try {
            String fileName = req.getParameter("fileName");
            if (StringUtils.isBlank(fileName)) {
                responseToClient(req, res, getMsg("title.nullbyteinjection.page", locale), bodyHtml.toString());
                return;
            } else {
                fileName = fileName + ".pdf";
            }

            // Get absolute path of the web application
            String appPath = getServletContext().getRealPath("");

            File file = new File(appPath + File.separator + "pdf" + File.separator + fileName);
            if (!file.exists()) {
                responseToClient(req, res, getMsg("title.nullbyteinjection.page", locale), bodyHtml.toString());
                return;
            }
            log.debug("File location on server::" + file.getAbsolutePath());
            ServletContext ctx = getServletContext();
            fis = new FileInputStream(file);
            String mimeType = ctx.getMimeType(file.getAbsolutePath());
            res.setContentType(mimeType != null ? mimeType : "application/octet-stream");
            res.setContentLength((int) file.length());
            res.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            os = res.getOutputStream();
            byte[] bufferData = new byte[1024];
            int read;
            while ((read = fis.read(bufferData)) != -1) {
                os.write(bufferData, 0, read);
            }
            os.flush();

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        } finally {
            Closer.close(os, fis);
        }
    }
}
