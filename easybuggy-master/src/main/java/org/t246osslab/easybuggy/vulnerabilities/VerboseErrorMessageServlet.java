package org.t246osslab.easybuggy.vulnerabilities;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.t246osslab.easybuggy.core.dao.EmbeddedADS;
import org.t246osslab.easybuggy.core.servlets.DefaultLoginServlet;
import org.t246osslab.easybuggy.core.utils.ApplicationUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/verbosemsg/login" })
public class VerboseErrorMessageServlet extends DefaultLoginServlet {
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        req.setAttribute("login.page.note", "msg.note.verbose.errror.message");
        super.doGet(req, res);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        Locale locale = req.getLocale();
        String userid = req.getParameter("userid");
        String password = req.getParameter("password");

        HttpSession session = req.getSession(true);
        if (isAccountLocked(userid)) {
            session.setAttribute("authNMsg", getErrMsg("msg.account.locked",
                    new String[]{String.valueOf(ApplicationUtils.getAccountLockCount())}, locale));
        } else if (!isExistUser(userid)) {
            session.setAttribute("authNMsg", getErrMsg("msg.user.not.exist", locale));
        } else if (!password.matches("[0-9a-z]{8}")) {
            session.setAttribute("authNMsg", getErrMsg("msg.low.alphnum8", locale));
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
            session.setAttribute("authNMsg", getErrMsg("msg.password.not.match", locale));
        }
        /* account lock count +1 */
        incrementLoginFailedCount(userid);
        doGet(req, res);
    }
    
    private boolean isExistUser(String username) {

        ExprNode filter;
        EntryFilteringCursor cursor = null;
        try {
            filter = FilterParser.parse("(uid=" + encodeForLDAP(username.trim()) + ")");
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
}
