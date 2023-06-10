package org.t246osslab.easybuggy.core.servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.t246osslab.easybuggy.core.utils.Closer;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

@SuppressWarnings("serial")
public abstract class AbstractServlet extends HttpServlet {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Send an HTTP response to the client.
     *
     * @param req HTTP servlet request.
     * @param res HTTP servlet response.
     * @param htmlTitle Title of HTML page.
     * @param htmlBody Body of HTML page.
     */
    protected void responseToClient(HttpServletRequest req, HttpServletResponse res, String htmlTitle, String htmlBody) {
        PrintWriter writer = null;
        HttpSession session = req.getSession();
        String userid = (String) session.getAttribute("userid");
        Locale locale = req.getLocale();
        try {
            writer = res.getWriter();
            writer.write("<HTML>");
            writer.write("<HEAD>");
            if (htmlTitle != null) {
                writer.write("<TITLE>" + htmlTitle + "</TITLE>");
            }
            writer.write("<link rel=\"icon\" type=\"image/vnd.microsoft.icon\" href=\"/images/favicon.ico\">");
            writer.write("<link rel=\"stylesheet\" " +
                    "href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\" " +
                    "integrity=\"sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u\" " +
                    "crossorigin=\"anonymous\">");
            writer.write("<link rel=\"stylesheet\" " +
                    "href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css\" " +
                    "integrity=\"sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp\" " +
                    "crossorigin=\"anonymous\">");
            writer.write("<script " +
                    "src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js\" " +
                    "integrity=\"sha384-3ceskX3iaEnIogmQchP8opvBy3Mi7Ce34nWjpBIwVTHfGYWQS9jwHDVRnpKKHJg7\" " +
                    "crossorigin=\"anonymous\"></script>");
            writer.write("<script " +
                    "src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\" " +
                    "integrity=\"sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa\" " +
                    "crossorigin=\"anonymous\"></script>");
            writer.write("<script " +
                    "src=\"https://cdn.rawgit.com/google/code-prettify/master/loader/run_prettify.js\" " +
                    "integrity=\"sha384-3+mjTIH6k3li4tycpEniAI83863YpLyJGB/hdI15inFZcAQK3IeMdXSgnoPkTzHn\" " +
                    "crossorigin=\"anonymous\"></script>");
            writer.write("<script " +
                    "src=\"https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.0/MathJax.js?config=TeX-AMS_CHTML\" " +
                    "integrity=\"sha384-crwIf/BuaWM9rM65iM+dWFldgQ1Un8jWZMuh3puxb8TOY9+linwLoI7ZHZT+aekW\" " +
                    "crossorigin=\"anonymous\"></script>");

            writer.write("</HEAD>");
            writer.write("<BODY STYLE=\"margin-left:20px;margin-right:20px;\">");
            writer.write("<table style=\"width:100%;\">");
            writer.write("<tr><td>");
            writer.write("<h2>");
            writer.write("<span class=\"glyphicon glyphicon-globe\"></span>&nbsp;");
            if (htmlTitle != null) {
                writer.write(htmlTitle);
            }
            writer.write("</h2>");
            writer.write("</td>");
            if (userid != null && req.getServletPath().startsWith("/admins")) {
                writer.write("<td align=\"right\">");
                writer.write(getMsg("label.login.user.id", locale) + ": " + userid);
                writer.write("<br>");
                writer.write("<a href=\"/logout\">" + getMsg("label.logout", locale) + "</a>");
                writer.write("</td>");
            } else {
                writer.write("<td align=\"right\">");
                writer.write("<a href=\"/\">" + getMsg("label.go.to.main", locale) + "</a>");
                writer.write("</td>");
            }
            writer.write("</tr>");
            writer.write("</table>");
            writer.write("<hr style=\"margin-top:0px\">");
            writer.write(htmlBody);
            writer.write("</BODY>");
            writer.write("</HTML>");

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        } finally {
            Closer.close(writer);
        }
    }

    /**
     * Return a message for a given property key.
     *
     * @return A message for a given property key
     */
    protected String getMsg(String propertyKey, Locale locale) {
        return getMsg(propertyKey, null, locale);
    }

    /**
     * Return an information message for a given property key.
     *
     * @return An information message for a given property key
     */
    protected String getInfoMsg(String propertyKey, Locale locale) {
        return getInfoMsg(propertyKey, null, locale);
    }

    /**
     * Return an error message for a given property key.
     *
     * @return An error message for a given property key
     */
    protected String getErrMsg(String propertyKey, Locale locale) {
        return getErrMsg(propertyKey, null, locale);
    }

    /**
     * Return a message for a given property key, replaced with placeholders.
     *
     * @return A message for a given property key, replaced with placeholders
     */
    protected String getMsg(String propertyKey, Object[] placeholders, Locale locale) {
        String propertyValue = null;
        try {
            propertyValue = ResourceBundle.getBundle("messages", locale).getString(propertyKey);
            if (placeholders != null) {
                propertyValue = MessageFormat.format(propertyValue, placeholders);
            }
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
        return propertyValue;
    }

    /**
     * Return an information message for a given property key, replaced with placeholders.
     *
     * @return An information message for a given property key, replaced with placeholders
     */
    protected String getInfoMsg(String propertyKey, Object[] placeholders, Locale locale) {
        return "<div class=\"alert alert-info\" role=\"alert\"><span class=\"glyphicon glyphicon-info-sign\"></span>&nbsp; "
                + getMsg(propertyKey, placeholders, locale) + "</div>";
    }

    /**
     * Return an error message for a given property key, replaced with placeholders.
     *
     * @return An error message for a given property key, replaced with placeholders
     */
    protected String getErrMsg(String propertyKey, Object[] placeholders, Locale locale) {
        return "<div class=\"alert alert-danger\" role=\"alert\"><span class=\"glyphicon glyphicon-warning-sign\"></span>&nbsp; "
                + getMsg(propertyKey, placeholders, locale) + "</div>";
    }

    /**
     * Encode data for use in HTML using HTML entity encoding
     * Note that this method just call <code>ESAPI.encoder().encodeForHTML(String)</code>.
     *
     * @param input the text to encode for HTML
     * @return input encoded for HTML
     */
    protected String encodeForHTML(String input) {
        return ESAPI.encoder().encodeForHTML(input);
    }

    /**
     * Encode data for use in LDAP queries.
     * Note that this method just call <code>ESAPI.encoder().encodeForLDAP((String)</code>.
     *
     * @param input the text to encode for LDAP
     * @return input encoded for use in LDAP
     */
    protected String encodeForLDAP(String input) {
        return ESAPI.encoder().encodeForLDAP(input);
    }
}
