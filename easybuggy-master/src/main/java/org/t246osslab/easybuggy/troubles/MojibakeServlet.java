package org.t246osslab.easybuggy.troubles;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.WordUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

// EncodingFilter excludes /mojibake. 
@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/mojibake" })
public class MojibakeServlet extends AbstractServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        req.setCharacterEncoding("Shift_JIS");
        res.setContentType("text/html; charset=UTF-8");
        try {
            String string = req.getParameter("string");
            Locale locale = req.getLocale();

            StringBuilder bodyHtml = new StringBuilder();

            bodyHtml.append("<form action=\"mojibake\" method=\"post\">");
            bodyHtml.append(getMsg("description.capitalize.string", locale));
            bodyHtml.append("<br><br>");
            bodyHtml.append(getMsg("label.string", locale) + ": ");
            bodyHtml.append("<input type=\"text\" name=\"string\" size=\"100\" maxlength=\"100\">");
            bodyHtml.append("<br><br>");
            bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.submit", locale) + "\">");
            bodyHtml.append("<br><br>");

            if (string != null && !"".equals(string)) {
                // Capitalize the given string
                String capitalizeName = WordUtils.capitalize(string);
                bodyHtml.append(getMsg("label.capitalized.string", locale) + " : " + encodeForHTML(capitalizeName));
            } else {
                bodyHtml.append(getMsg("msg.enter.string", locale));
            }
            bodyHtml.append("<br><br>");
            bodyHtml.append(getInfoMsg("msg.note.mojibake", locale));
            bodyHtml.append("</form>");

            responseToClient(req, res, getMsg("title.mojibake.page", locale), bodyHtml.toString());

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }
}
