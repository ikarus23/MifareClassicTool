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
import org.t246osslab.easybuggy.core.utils.EmailUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/admins/clickjacking" })
public class ClickJackingServlet extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Locale locale = req.getLocale();

        StringBuilder bodyHtml = new StringBuilder();
        bodyHtml.append("<form action=\"/admins/clickjacking\" method=\"post\">");
        bodyHtml.append(getMsg("msg.enter.mail", locale));
        bodyHtml.append("<br><br>");
        bodyHtml.append(getMsg("label.mail", locale) + ": ");
        bodyHtml.append("<input type=\"text\" name=\"mail\" size=\"30\" maxlength=\"30\">");
        bodyHtml.append("<br><br>");
        bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.submit", locale) + "\">");
        bodyHtml.append("<br><br>");
        String errorMessage = (String) req.getAttribute("errorMessage");
        if (errorMessage != null) {
            bodyHtml.append(errorMessage);
        }
        bodyHtml.append(getInfoMsg("msg.note.clickjacking", locale));
        bodyHtml.append("</form>");
        responseToClient(req, res, getMsg("title.clickjacking.page", locale), bodyHtml.toString());
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
        if (userid == null) {
            res.sendRedirect("/");
            return;
        }
        String mail = StringUtils.trim(req.getParameter("mail"));
        if (!StringUtils.isBlank(mail) && EmailUtils.isValidEmailAddress(mail)) {
            try {
                DefaultClientAttribute entryAttribute = new DefaultClientAttribute("mail", encodeForLDAP(mail.trim()));
                ClientModification clientModification = new ClientModification();
                clientModification.setAttribute(entryAttribute);
                clientModification.setOperation(ModificationOperation.REPLACE_ATTRIBUTE);
                ModifyRequestImpl modifyRequest = new ModifyRequestImpl(1);
                modifyRequest.setName(new LdapDN("uid=" + encodeForLDAP(userid.trim()) + ",ou=people,dc=t246osslab,dc=org"));
                modifyRequest.addModification(clientModification);
                EmbeddedADS.getAdminSession().modify(modifyRequest);

                StringBuilder bodyHtml = new StringBuilder();
                bodyHtml.append("<form>");
                bodyHtml.append(getMsg("msg.mail.changed", locale));
                bodyHtml.append("<br><br>");
                bodyHtml.append("<a href=\"/admins/main\">" + getMsg("label.goto.admin.page", locale) + "</a>");
                bodyHtml.append("</form>");
                responseToClient(req, res, getMsg("title.clickjacking.page", locale), bodyHtml.toString());
            } catch (Exception e) {
                log.error("Exception occurs: ", e);
                req.setAttribute("errorMessage", getErrMsg("msg.mail.change.failed", locale));
                doGet(req, res);
            }
        } else {
            req.setAttribute("errorMessage", getErrMsg("msg.mail.format.is.invalid", locale));
            doGet(req, res);
        }
    }
}
