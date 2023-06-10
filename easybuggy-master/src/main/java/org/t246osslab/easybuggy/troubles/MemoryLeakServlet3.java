package org.t246osslab.easybuggy.troubles;

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.Deflater;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/memoryleak3" })
public class MemoryLeakServlet3 extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StringBuilder bodyHtml = new StringBuilder();
        Locale locale = req.getLocale();
        TimeZone tz = TimeZone.getDefault();
        bodyHtml.append("<table class=\"table table-striped table-bordered table-hover\" style=\"font-size:small;\">");
        bodyHtml.append("<tr><td>" + getMsg("label.timezone.id", req.getLocale()) + "</td>");
        bodyHtml.append("<td>" + tz.getID() + "</td></tr>");
        bodyHtml.append("<tr><td>" + getMsg("label.timezone.name", req.getLocale()) + "</td>");
        bodyHtml.append("<td>" + tz.getDisplayName() + "</td></tr>");
        bodyHtml.append("<tr><td>" + getMsg("label.timezone.offset", req.getLocale()) + "</td>");
        bodyHtml.append("<td>" + tz.getRawOffset() + "</td></tr>");
        bodyHtml.append("</table>");
        try {
            toDoRemove();
            
            bodyHtml.append(getInfoMsg("msg.note.memoryleak3", req.getLocale()));

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
            bodyHtml.append(getErrMsg("msg.unknown.exception.occur", new String[] { e.getMessage() }, locale));
        } finally {
            responseToClient(req, res, getMsg("title.memoryleak3.page", locale), bodyHtml.toString());
        }
    }

    private void toDoRemove() {
        String inputString = "inputString";
        byte[] input = inputString.getBytes();
        byte[] output = new byte[100];
        for (int i = 0; i < 1000; i++) {
            Deflater compresser = new Deflater();
            compresser.setInput(input);
            compresser.deflate(output);
        }
    }
}
