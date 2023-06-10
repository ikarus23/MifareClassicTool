package org.t246osslab.easybuggy.core.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet Filter for authentication
 */
@WebFilter(urlPatterns = { "/*" })
public class AuthenticationFilter implements Filter {

    /**
     * Intercept unauthenticated requests for specific URLs and redirect to login page.
     *
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {
        
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String target = request.getRequestURI();
        
        if (target.startsWith("/admins") || "/uid/serverinfo.jsp".equals(target)) {
            /* Login (authentication) is needed to access admin pages (under /admins). */
            
            String loginType = request.getParameter("logintype");
            String queryString = request.getQueryString();
            if (queryString == null) {
                queryString = "";
            } else {
                /* Remove "logintype" parameter from query string.
                    (* "logintype" specifies a login servlet) */
                queryString = queryString.replace("logintype=" + loginType + "&", "");
                queryString = queryString.replace("&logintype=" + loginType, "");
                queryString = queryString.replace("logintype=" + loginType, "");
                if (queryString.length() > 0) {
                    queryString = "?" + queryString;
                }
            }
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("authNMsg") == null
                    || !"authenticated".equals(session.getAttribute("authNMsg"))) {
                /* Not authenticated yet */
                session = request.getSession(true);
                session.setAttribute("target", target);
                if (loginType == null) {
                    response.sendRedirect(response.encodeRedirectURL("/login" + queryString));
                } else if ("sessionfixation".equals(loginType)) {
                    response.sendRedirect(response.encodeRedirectURL("/" + loginType + "/login" + queryString));
                } else {
                    response.sendRedirect("/" + loginType + "/login" + queryString);
                }
                return;
            }
        }
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        // Do nothing
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // Do nothing
    }
}
