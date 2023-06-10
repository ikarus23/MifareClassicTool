package org.t246osslab.easybuggy.errors;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/oome4" })
public class OutOfMemoryErrorServlet4 extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Properties properties = System.getProperties();
        Random r = new Random();
        while (true) {
            properties.put(r.nextInt(), "value");
        }
    }
}
