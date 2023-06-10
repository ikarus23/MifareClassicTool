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

/**
 * Servlet Filter for encoding
 */
@WebFilter(urlPatterns = { "/*" })
public class EncodingFilter implements Filter {

    /**
     * Set the encoding to use for requests.
     * "Shift_JIS" is intentionally set to the request to /mojibake.
     * 
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        if (!"/mojibake".equals(request.getRequestURI())) {
            /* Set the default character encoding and content type to UTF-8 (except under /mojibake) */
            req.setCharacterEncoding("UTF-8");
            res.setContentType("text/html; charset=UTF-8");
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
