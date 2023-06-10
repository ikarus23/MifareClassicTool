package org.t246osslab.easybuggy.errors;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

/**
 * This servlet causes a JNI error.
 */
@WebServlet(urlPatterns = { "/jnicall" })
@SuppressWarnings("serial")
public class UnsatisfiedLinkErrorServlet extends AbstractServlet {

    private static native NetworkInterface getByName0(String name) throws SocketException;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        getByName0("");
    }
}
