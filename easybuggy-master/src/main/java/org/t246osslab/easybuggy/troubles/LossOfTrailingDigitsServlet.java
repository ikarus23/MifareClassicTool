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
@WebServlet(urlPatterns = { "/lotd" })
public class LossOfTrailingDigitsServlet extends AbstractServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        boolean isValid = true;
        Locale locale = req.getLocale();
        String strNumber = req.getParameter("number");
        double number = NumberUtils.toDouble(strNumber, Double.NaN);
        try {
            if (Double.isNaN(number) || number <= -1 || 1 <= number) {
                isValid = false;
            }

            StringBuilder bodyHtml = new StringBuilder();
            bodyHtml.append("<form action=\"lotd\" method=\"post\">");
            bodyHtml.append(getMsg("msg.enter.decimal.value", locale));
            bodyHtml.append("<br><br>");
            if (!Double.isNaN(number) && isValid) {
                bodyHtml.append("<input type=\"text\" name=\"number\" size=\"18\" maxlength=\"18\" value=" + strNumber + ">");
            } else {
                bodyHtml.append("<input type=\"text\" name=\"number\" size=\"18\" maxlength=\"18\">");
            }
            bodyHtml.append(" + 1 = ");
            if (!Double.isNaN(number) && isValid) {
                bodyHtml.append(String.valueOf(number + 1));
            }
            bodyHtml.append("<br><br>");
            bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.calculate", locale) + "\">");
            bodyHtml.append("<br><br>");
            bodyHtml.append(getInfoMsg("msg.note.lossoftrailingdigits", locale));
            bodyHtml.append("</form>");
            responseToClient(req, res, getMsg("title.lossoftrailingdigits.page", locale), bodyHtml.toString());

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }
}
