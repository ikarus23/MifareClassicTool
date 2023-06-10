package org.t246osslab.easybuggy.troubles;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/netsocketleak" })
public class NetworkSocketLeakServlet extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        HttpURLConnection connection;
        URL url;
        StringBuilder bodyHtml = new StringBuilder();
        Locale locale = req.getLocale();
        try {
            String pingURL = req.getParameter("pingurl");
            if (pingURL == null) {
                pingURL = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + "/ping";
            }
            url = new URL(pingURL);

            long start = System.currentTimeMillis();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            long end = System.currentTimeMillis();
            
            bodyHtml.append("<p>"+getMsg("description.response.time", req.getLocale())+"</p>");
            bodyHtml.append("<table class=\"table table-striped table-bordered table-hover\" style=\"font-size:small;\">");
            bodyHtml.append("<tr><td>" + getMsg("label.ping.url", locale) + "</td>");
            bodyHtml.append("<td>" + pingURL + "</td></tr>");
            bodyHtml.append("<tr><td>" + getMsg("label.response.code", req.getLocale()) + "</td>");
            bodyHtml.append("<td>" + responseCode + "</td></tr>");
            bodyHtml.append("<tr><td>" + getMsg("label.response.time", locale) + "</td>");
            bodyHtml.append("<td>" + (end - start) + "</td></tr>");
            bodyHtml.append("</table>");

            bodyHtml.append(getInfoMsg("msg.note.netsocketleak", req.getLocale()));
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
            bodyHtml.append(getErrMsg("msg.unknown.exception.occur", new String[] { e.getMessage() }, locale));
        } finally {
            responseToClient(req, res, getMsg("title.netsocketleak.page", locale), bodyHtml.toString());
        }
    }
}
