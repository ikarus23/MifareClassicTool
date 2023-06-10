package org.t246osslab.easybuggy.vulnerabilities;

import java.io.IOException;
import java.util.Locale;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/codeijc" })
public class CodeInjectionServlet extends AbstractServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        try {
            String jsonString = req.getParameter("jsonString");
            Locale locale = req.getLocale();

            StringBuilder bodyHtml = new StringBuilder();

            bodyHtml.append("<form action=\"codeijc\" method=\"post\">");
            bodyHtml.append(getMsg("description.parse.json", locale));
            bodyHtml.append("<br><br>");
            bodyHtml.append(getMsg("label.json.string", locale) + ": ");
            bodyHtml.append("<textarea name=\"jsonString\" cols=\"80\" rows=\"15\">");
            if (!StringUtils.isBlank(jsonString)) {
                bodyHtml.append(encodeForHTML(jsonString));
            }
            bodyHtml.append("</textarea>");
            bodyHtml.append("<br><br>");
            bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.submit", locale) + "\">");
            bodyHtml.append("<br><br>");

            if (!StringUtils.isBlank(jsonString)) {
                jsonString = jsonString.replaceAll(" ", "");
                jsonString = jsonString.replaceAll("\r\n", "");
                jsonString = jsonString.replaceAll("\n", "");
                parseJson(jsonString, locale, bodyHtml);
            } else {
                bodyHtml.append(getMsg("msg.enter.json.string", locale));
                bodyHtml.append("<br><br>");
            }
            bodyHtml.append(getInfoMsg("msg.note.codeinjection", locale));
            bodyHtml.append("</form>");

            responseToClient(req, res, getMsg("title.codeinjection.page", locale), bodyHtml.toString());
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }

    private void parseJson(String jsonString, Locale locale, StringBuilder bodyHtml) {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine scriptEngine = manager.getEngineByName("JavaScript");
            scriptEngine.eval("JSON.parse('" + jsonString + "')");
            bodyHtml.append(getMsg("msg.valid.json", locale));
            bodyHtml.append("<br><br>");
        } catch (ScriptException e) {
            bodyHtml.append(getErrMsg("msg.invalid.json", new String[] { encodeForHTML(e.getMessage()) }, locale));
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
            bodyHtml.append(getErrMsg("msg.invalid.json", new String[] { encodeForHTML(e.getMessage()) }, locale));
        }
    }
}
