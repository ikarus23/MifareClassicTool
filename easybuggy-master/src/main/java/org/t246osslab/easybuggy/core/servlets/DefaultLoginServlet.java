package org.t246osslab.easybuggy.core.servlets;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.t246osslab.easybuggy.core.dao.EmbeddedADS;
import org.t246osslab.easybuggy.core.model.User;
import org.t246osslab.easybuggy.core.utils.ApplicationUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/login" })
public class DefaultLoginServlet extends AbstractServlet {
    
    /* User's login history using in-memory account locking */
    private static ConcurrentHashMap<String, User> userLoginHistory = new ConcurrentHashMap<String, User>();
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        Locale locale = req.getLocale();
        StringBuilder bodyHtml = new StringBuilder();

        bodyHtml.append("<p>" + getMsg("msg.need.admin.privilege", locale) + "</p>");
        bodyHtml.append("<form method=\"POST\" action=\"" + req.getRequestURI() + "\">");
        bodyHtml.append("<table width=\"400px\" height=\"150px\">");
        bodyHtml.append("<tr>");
        bodyHtml.append("<td>" + getMsg("label.user.id", locale) + " :&nbsp;</td>");
        bodyHtml.append("<td><input type=\"text\" name=\"userid\" size=\"20\"></td>");
        bodyHtml.append("</tr>");
        bodyHtml.append("<tr>");
        bodyHtml.append("<td>" + getMsg("label.password", locale) + " :&nbsp;</td>");
        bodyHtml.append("<td><input type=\"password\" name=\"password\" size=\"20\" autocomplete=\"off\"></td>");
        bodyHtml.append("</tr>");
        bodyHtml.append("<tr>");
        bodyHtml.append("<td></td>");
        bodyHtml.append("<td><input type=\"submit\" value=\"" + getMsg("label.login", locale) + "\"></td>");
        bodyHtml.append("</tr>");
        bodyHtml.append("</table>");
        String queryString = req.getQueryString();
        if (queryString != null) {
            bodyHtml.append("<input type=\"hidden\" name=\"loginquerystring\" value=\""
                    + encodeForHTML(queryString) + "\">");
        }
        Enumeration<?> paramNames = req.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            String[] paramValues = req.getParameterValues(paramName);
            for (String paramValue : paramValues) {
                bodyHtml.append("<input type=\"hidden\" name=\"" + encodeForHTML(paramName)
                        + "\" value=\"" + encodeForHTML(paramValue) + "\">");
            }
        }

        HttpSession session = req.getSession(true);
        String authNMsg = (String) session.getAttribute("authNMsg");
        if (authNMsg != null && !"authenticated".equals(authNMsg)) {
            bodyHtml.append(authNMsg);
            session.setAttribute("authNMsg", null);
        }
        if (req.getAttribute("login.page.note") != null) {
            bodyHtml.append(getInfoMsg((String) req.getAttribute("login.page.note"), locale));
        }
        bodyHtml.append("</form>");
        responseToClient(req, res, getMsg("title.login.page", locale), bodyHtml.toString());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        Locale locale = req.getLocale();
        String userid = StringUtils.trim(req.getParameter("userid"));
        String password = StringUtils.trim(req.getParameter("password"));

        HttpSession session = req.getSession(true);
        if (isAccountLocked(userid)) {
            session.setAttribute("authNMsg", getErrMsg("msg.authentication.fail", locale));
        } else if (authUser(userid, password)) {
            /* Reset account lock count */
            resetAccountLock(userid);

            session.setAttribute("authNMsg", "authenticated");
            session.setAttribute("userid", userid);
            
            String target = (String) session.getAttribute("target");
            if (target == null) {
                res.sendRedirect("/admins/main");
            } else {
                session.removeAttribute("target");
                res.sendRedirect(target);
            }
            return;
        } else {
            /* account lock count +1 */
            session.setAttribute("authNMsg", getErrMsg("msg.authentication.fail", locale));
        }
        incrementLoginFailedCount(userid);
        doGet(req, res);
    }

    protected void incrementLoginFailedCount(String userid) {
        User admin = getUser(userid);
        admin.setLoginFailedCount(admin.getLoginFailedCount() + 1);
        admin.setLastLoginFailedTime(new Date());
    }

    protected void resetAccountLock(String userid) {
        User admin = getUser(userid);
        admin.setLoginFailedCount(0);
        admin.setLastLoginFailedTime(null);
    }

    protected boolean isAccountLocked(String userid) {
        User admin = userLoginHistory.get(userid);
        return (admin != null
                && admin.getLoginFailedCount() >= ApplicationUtils.getAccountLockCount()
                && (new Date().getTime() - admin.getLastLoginFailedTime().getTime() < ApplicationUtils
                        .getAccountLockTime()));
    }

    protected boolean authUser(String uid, String password) {
        
        if (uid == null || password == null) {
            return false;
        }
        ExprNode filter;
        EntryFilteringCursor cursor = null;
        try {
            filter = FilterParser.parse("(&(uid=" + encodeForLDAP(uid.trim())
                    + ")(userPassword=" + encodeForLDAP(password.trim()) + "))");
            cursor = EmbeddedADS.getAdminSession().search(new LdapDN("ou=people,dc=t246osslab,dc=org"),
                    SearchScope.SUBTREE, filter, AliasDerefMode.NEVER_DEREF_ALIASES, null);
            if (cursor.available()) {
                return true;
            }
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    log.error("Exception occurs: ", e);
                }
            }
        }
        return false;
    }

    private User getUser(String userid) {
        User admin = userLoginHistory.get(userid);
        if (admin == null) {
            User newAdmin = new User();
            newAdmin.setUserId(userid);
            admin = userLoginHistory.putIfAbsent(userid, newAdmin);
            if (admin == null) {
                admin = newAdmin;
            }
        }
        return admin;
    }

}
