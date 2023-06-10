package org.t246osslab.easybuggy.vulnerabilities;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.t246osslab.easybuggy.core.dao.EmbeddedADS;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/admins/csrf" })
public class CSRFServlet extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Locale locale = req.getLocale();

        StringBuilder bodyHtml = new StringBuilder();
        bodyHtml.append("<form action=\"/admins/csrf\" method=\"post\">");
        bodyHtml.append(getMsg("msg.enter.passwd", locale));
        bodyHtml.append("<br><br>");
        bodyHtml.append(getMsg("label.password", locale) + ": ");
        bodyHtml.append("<input type=\"password\" name=\"password\" size=\"30\" maxlength=\"30\" autocomplete=\"off\">");
        bodyHtml.append("<br><br>");
        bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.submit", locale) + "\">");
        bodyHtml.append("<br><br>");
        String errorMessage = (String) req.getAttribute("errorMessage");
        if (errorMessage != null) {
            bodyHtml.append(errorMessage);
        }
        bodyHtml.append(getInfoMsg("msg.note.csrf", locale));
        bodyHtml.append("</form>");
        responseToClient(req, res, getMsg("title.csrf.page", locale), bodyHtml.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Locale locale = req.getLocale();
        HttpSession session = req.getSession();
        if (session == null) {
            res.sendRedirect("/");
            return;
        }
        String userid = (String) session.getAttribute("userid");
        String password = StringUtils.trim(req.getParameter("password"));
        if (!StringUtils.isBlank(userid) && !StringUtils.isBlank(password) && password.length() >= 8) {
            try {
                DefaultClientAttribute entryAttribute = new DefaultClientAttribute("userPassword", encodeForLDAP(password.trim()));
                ClientModification clientModification = new ClientModification();
                clientModification.setAttribute(entryAttribute);
                clientModification.setOperation(ModificationOperation.REPLACE_ATTRIBUTE);
                ModifyRequestImpl modifyRequest = new ModifyRequestImpl(1);
                modifyRequest.setName(new LdapDN("uid=" + encodeForLDAP(userid.trim()) + ",ou=people,dc=t246osslab,dc=org"));
                modifyRequest.addModification(clientModification);
                EmbeddedADS.getAdminSession().modify(modifyRequest);

                StringBuilder bodyHtml = new StringBuilder();
                bodyHtml.append("<form>");
                bodyHtml.append(getMsg("msg.passwd.changed", locale));
                bodyHtml.append("<br><br>");
                bodyHtml.append("<a href=\"/admins/main\">" + getMsg("label.goto.admin.page", locale) + "</a>");
                bodyHtml.append("</form>");
                responseToClient(req, res, getMsg("title.csrf.page", locale), bodyHtml.toString());
            } catch (Exception e) {
                log.error("Exception occurs: ", e);
                req.setAttribute("errorMessage", getErrMsg("msg.passwd.change.failed", locale));
                doGet(req, res);
            }
        } else {
            if (StringUtils.isBlank(password) || password.length() < 8) {
                req.setAttribute("errorMessage", getErrMsg("msg.passwd.is.too.short", locale));
            } else {
                req.setAttribute("errorMessage", getErrMsg("msg.unknown.exception.occur",
                        new String[] { "userid: " + userid }, locale));
            }
            doGet(req, res);
        }
    }
}
