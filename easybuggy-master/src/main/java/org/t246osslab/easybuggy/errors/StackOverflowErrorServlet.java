package org.t246osslab.easybuggy.errors;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@WebServlet(urlPatterns = { "/sofe" })
@SuppressWarnings("serial")
public class StackOverflowErrorServlet extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        new S().toString();
    }

    public class S {
        @Override
        public String toString() {
            return "" + this;
        }
    }
}
