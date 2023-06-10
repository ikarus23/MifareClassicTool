package org.t246osslab.easybuggy.exceptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/iase" })
public class IllegalStateExceptionServlet extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        List<String> alphabet = new ArrayList<String>(Arrays.asList("a", "b, c"));
        for (final Iterator<String> itr = alphabet.iterator(); itr.hasNext();) {
            itr.remove();
        }
    }
}
