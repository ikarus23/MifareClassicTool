package org.t246osslab.easybuggy.exceptions;

import java.io.IOException;
import java.util.Stack;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/ese" })
public class EmptyStackExceptionServlet extends AbstractServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Stack<String> stack = new Stack<String>();
        String tmp;
        while (null != (tmp = stack.pop())) {
            log.debug("Stack.pop(): " + tmp);
        }
    }
}
