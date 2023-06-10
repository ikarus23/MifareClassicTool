package org.t246osslab.easybuggy.exceptions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/uoe" })
public class UnsupportedOperationExceptionServlet extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        List<String> alphabet = Arrays.asList("a", "b", "c");
        Iterator<String> i = alphabet.iterator();
        while(i.hasNext()){
            String name = i.next();
            if(!"a".equals(name)){
                i.remove();
            }
        }
    }
}
