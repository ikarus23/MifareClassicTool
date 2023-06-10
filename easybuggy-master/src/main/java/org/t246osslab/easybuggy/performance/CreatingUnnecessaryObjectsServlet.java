package org.t246osslab.easybuggy.performance;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.math.NumberUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/createobjects" })
public class CreatingUnnecessaryObjectsServlet extends AbstractServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Locale locale = req.getLocale();
        String strNumber = req.getParameter("number");
        int number = NumberUtils.toInt(strNumber, -1);
        StringBuilder bodyHtml = new StringBuilder();
        bodyHtml.append("<form action=\"createobjects\" method=\"post\">");
        bodyHtml.append(getMsg("msg.calc.sym.natural.numbers", locale));
        bodyHtml.append("<br><br>n = ");
        if (number > 0) {
            bodyHtml.append("<input type=\"text\" name=\"number\" size=\"9\" maxlength=\"9\" value=" + strNumber + ">");
        } else {
            bodyHtml.append("<input type=\"text\" name=\"number\" size=\"9\" maxlength=\"9\">");
        }
        bodyHtml.append("<br><br>");
        if (number > 0) {
            switch (number) {
            case 1:
                break;
            case 2:
                bodyHtml.append("1 + 2 = ");
                break;
            case 3:
                bodyHtml.append("1 + 2 + 3 = ");
                break;
            case 4:
                bodyHtml.append("1 + 2 + 3 + 4 = ");
                break;
            case 5:
                bodyHtml.append("1 + 2 + 3 + 4 + 5 = ");
                break;
            default:
                bodyHtml.append("1 + 2 + 3 + ... + " + number + " = ");
                bodyHtml.append("\\(\\begin{eqnarray}\\sum_{ k = 1 }^{ " + number + " } k\\end{eqnarray}\\) = ");
            }
        } else {
            bodyHtml.append("1 + 2 + 3 + ... + n = ");
            bodyHtml.append("\\(\\begin{eqnarray}\\sum_{ k = 1 }^{ n } k\\end{eqnarray}\\) = ");
        }
        if (number >= 1) {
            long start = System.nanoTime();
            bodyHtml.append(calcSum1(number));
            log.info("{} ms", (System.nanoTime() - start) / 1000000f);
        }
        bodyHtml.append("<br><br>");
        bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.calculate", locale) + "\">");
        bodyHtml.append("<br><br>");
        bodyHtml.append(getInfoMsg("msg.note.createobjects", locale));
        bodyHtml.append("</form>");

        responseToClient(req, res, getMsg("title.createobjects.page", locale), bodyHtml.toString());
    }

    private Long calcSum1(int number) {
        Long sum = 0L;
        for (long i = 1; i <= number; i++) {
            sum += i;
        }
        return sum;
    }
/*
    private long calcSum2(int number) {
        long sum = 0L;
        for (int i = 1; i <= number; i++) {
            sum += i;
        }
        return sum;
    }

    private long calcSum3(int number) {
        return (long) number * (number + 1) / 2;
    }
*/
}
