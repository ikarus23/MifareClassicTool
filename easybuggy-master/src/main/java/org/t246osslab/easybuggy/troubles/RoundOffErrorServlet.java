package org.t246osslab.easybuggy.troubles;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.math.NumberUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/roe" })
public class RoundOffErrorServlet extends AbstractServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            Locale locale = req.getLocale();
            String strNumber = req.getParameter("number");
            int number = NumberUtils.toInt(strNumber, -1);

            StringBuilder bodyHtml = new StringBuilder();
            bodyHtml.append("<form action=\"roe\" method=\"post\">");
            bodyHtml.append(getMsg("msg.enter.positive.number", locale));
            bodyHtml.append("<br><br>");
            if (1 <= number && number <= 9) {
                bodyHtml.append("<input type=\"text\" name=\"number\" size=\"1\" maxlength=\"1\" value=" + strNumber + ">");
            } else {
                bodyHtml.append("<input type=\"text\" name=\"number\" size=\"1\" maxlength=\"1\">");
            }
            bodyHtml.append(" - 0.9 = ");
            if (1 <= number && number <= 9) {
                bodyHtml.append(String.valueOf(number - 0.9));
            }
            bodyHtml.append("<br><br>");
            bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.calculate", locale) + "\">");
            bodyHtml.append("<br><br>");
            bodyHtml.append(getInfoMsg("msg.note.roundofferror", locale));
            bodyHtml.append("</form>");
            responseToClient(req, res, getMsg("title.roundofferror.page", locale), bodyHtml.toString());

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }
}
