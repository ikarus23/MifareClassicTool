package org.t246osslab.easybuggy.performance;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/slowre" })
public class SlowRegularExpressionServlet extends AbstractServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        try {
            String word = req.getParameter("word");
            Locale locale = req.getLocale();

            StringBuilder bodyHtml = new StringBuilder();

            bodyHtml.append("<form action=\"slowre\" method=\"post\">");
            bodyHtml.append(getMsg("description.test.regular.expression", locale));
            bodyHtml.append("<br><br>");
            bodyHtml.append("<img src=\"images/regular-expression.png\">");
            bodyHtml.append("<br><br>");
            bodyHtml.append(getMsg("label.string", locale) + ": ");
            bodyHtml.append("<input type=\"text\" name=\"word\" size=\"50\" maxlength=\"50\">");
            bodyHtml.append("<br><br>");
            bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.submit", locale) + "\">");
            bodyHtml.append("<br><br>");

            if (!StringUtils.isBlank(word)) {
                log.info("Start Date: {}", new Date());
                Pattern compile = Pattern.compile("^([a-z0-9]+[-]{0,1}){1,100}$");
                Matcher matcher = compile.matcher(word);
                boolean matches = matcher.matches();
                log.info("End Date: {}", new Date());
                if (matches) {
                    bodyHtml.append(getMsg("msg.match.regular.expression", locale));
                } else {
                    bodyHtml.append(getMsg("msg.not.match.regular.expression", locale));
                }
            } else {
                bodyHtml.append(getMsg("msg.enter.string", locale));
            }
            bodyHtml.append("<br><br>");
            bodyHtml.append(getInfoMsg("msg.note.slowregex", locale));
            bodyHtml.append("</form>");

            responseToClient(req, res, getMsg("title.slowregex.page", locale), bodyHtml.toString());

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }
}
