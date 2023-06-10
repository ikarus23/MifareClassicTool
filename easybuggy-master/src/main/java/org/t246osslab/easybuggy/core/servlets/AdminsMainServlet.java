package org.t246osslab.easybuggy.core.servlets;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/admins/main" })
public class AdminsMainServlet extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Locale locale = req.getLocale();
        StringBuilder bodyHtml = new StringBuilder();
        bodyHtml.append(getMsg("msg.admin.page.top", locale));
        bodyHtml.append("<br><br><ul>");
        bodyHtml.append("<li><a href=\"").append(res.encodeURL("/uid/serverinfo.jsp")).append("\">");
        bodyHtml.append(getMsg("title.serverinfo.page", locale)).append("</a></li>");
        bodyHtml.append("<li><a href=\"").append(res.encodeURL("/admins/csrf")).append("\">");
        bodyHtml.append(getMsg("title.csrf.page", locale)).append("</a></li>");
        bodyHtml.append("<li><a href=\"").append(res.encodeURL("/admins/clickjacking")).append("\">");
        bodyHtml.append(getMsg("title.clickjacking.page", locale)).append("</a></li>");
        bodyHtml.append("</ul>");
        responseToClient(req, res, getMsg("title.adminmain.page", locale), bodyHtml.toString());
    }
}
