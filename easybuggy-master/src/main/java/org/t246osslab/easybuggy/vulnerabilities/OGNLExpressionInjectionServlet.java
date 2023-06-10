package org.t246osslab.easybuggy.vulnerabilities;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/ognleijc" })
public class OGNLExpressionInjectionServlet extends AbstractServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Locale locale = req.getLocale();
        StringBuilder bodyHtml = new StringBuilder();
        Object value = null;
        String errMessage = "";
        OgnlContext ctx = new OgnlContext();
        String expression = req.getParameter("expression");
        if (!StringUtils.isBlank(expression)) {
            try {
                Object expr = Ognl.parseExpression(expression.replaceAll("Math\\.", "@Math@"));
                value = Ognl.getValue(expr, ctx);
            } catch (OgnlException e) {
                if (e.getReason() != null) {
                    errMessage = e.getReason().getMessage();
                }
                log.debug("OgnlException occurs: ", e);
            } catch (Exception e) {
                log.debug("Exception occurs: ", e);
            } catch (Error e) {
                log.debug("Error occurs: ", e);
            }
        }

        bodyHtml.append("<form action=\"ognleijc\" method=\"post\">");
        bodyHtml.append(getMsg("msg.enter.math.expression", locale));
        bodyHtml.append("<br><br>");
        if (expression == null) {
            bodyHtml.append("<input type=\"text\" name=\"expression\" size=\"80\" maxlength=\"300\">");
        } else {
            bodyHtml.append("<input type=\"text\" name=\"expression\" size=\"80\" maxlength=\"300\" value=\""
                    + encodeForHTML(expression) + "\">");
        }
        bodyHtml.append(" = ");
        if (value != null && NumberUtils.isNumber(value.toString())) {
            bodyHtml.append(value);
        }
        bodyHtml.append("<br><br>");
        bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.calculate", locale) + "\">");
        bodyHtml.append("<br><br>");
        if (value == null && expression != null) {
            bodyHtml.append(getErrMsg("msg.invalid.expression", new String[] { errMessage }, locale));
        }
        bodyHtml.append(getInfoMsg("msg.note.commandinjection", locale));
        bodyHtml.append("</form>");

        responseToClient(req, res, getMsg("title.commandinjection.page", locale), bodyHtml.toString());
    }
}
