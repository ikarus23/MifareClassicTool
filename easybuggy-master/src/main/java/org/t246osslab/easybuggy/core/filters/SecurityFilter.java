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

/**
 * Servlet Filter for security
 */
@WebFilter(urlPatterns = { "/*" })
public class SecurityFilter implements Filter {
	
    /**
     * Prevent several security vulnerabilities.
     *
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
		String target = request.getRequestURI();

        /* Prevent clickjacking if target is not /admins/clickjacking ... */
        if (!target.startsWith("/admins/clickjacking")) {
            response.addHeader("X-FRAME-OPTIONS", "DENY");
        }
        /* Prevent Content-Type sniffing */
        response.addHeader("X-Content-Type-Options", "nosniff");
        
        /* Prevent XSS if target is not /xss ... */
        if (!target.startsWith("/xss")) {
            response.addHeader("X-XSS-Protection", "1; mode=block");
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
