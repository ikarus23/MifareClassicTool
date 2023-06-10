package org.t246osslab.easybuggy.vulnerabilities;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.t246osslab.easybuggy.core.servlets.DefaultLoginServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/openredirect/login" })
public class OpenRedirectServlet extends DefaultLoginServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        req.setAttribute("login.page.note", "msg.note.open.redirect");
        super.doGet(req, res);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        Locale locale = req.getLocale();
        String userid = req.getParameter("userid");
        String password = req.getParameter("password");
        String loginQueryString = req.getParameter("loginquerystring");
        if (loginQueryString == null) {
            loginQueryString = "";
        } else {
            loginQueryString = "?" + loginQueryString;
        }
        
        HttpSession session = req.getSession(true);
        if (isAccountLocked(userid)) {
            session.setAttribute("authNMsg", getErrMsg("msg.authentication.fail", locale));
            res.sendRedirect("/openredirect/login" + loginQueryString);
        } else if (authUser(userid, password)) {
            /* Reset account lock count */
            resetAccountLock(userid);

            session.setAttribute("authNMsg", "authenticated");
            session.setAttribute("userid", userid);
            
            String gotoUrl = req.getParameter("goto");
            if (gotoUrl != null) {
                res.sendRedirect(gotoUrl);
            } else {
                String target = (String) session.getAttribute("target");
                if (target == null) {
                    res.sendRedirect("/admins/main");
                } else {
                    session.removeAttribute("target");
                    res.sendRedirect(target);
                }
            }
        } else {
            /* account lock count +1 */
            incrementLoginFailedCount(userid);
            
            session.setAttribute("authNMsg", getErrMsg("msg.authentication.fail", locale));
            res.sendRedirect("/openredirect/login" + loginQueryString);
        }
    }
}
