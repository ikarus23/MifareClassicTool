package org.t246osslab.easybuggy.exceptions;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/iae" })
public class IllegalArgumentExceptionServlet extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setAttribute("iae", new ArrayList<Object>(-1));
    }
}
