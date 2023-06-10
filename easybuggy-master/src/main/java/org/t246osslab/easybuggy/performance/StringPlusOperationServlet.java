package org.t246osslab.easybuggy.performance;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.math.NumberUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/strplusopr" })
public class StringPlusOperationServlet extends AbstractServlet {

    private static final int MAX_LENGTH = 1000000;
    private static final String[] ALL_NUMBERS = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
    private static final String[] ALL_UPPER_CHARACTERS = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
            "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };
    private static final String[] ALL_LOWER_CHARACTERS = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
            "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z" };
    private static final String[] ALL_SIGNS = { "!", "#", "$", "%", "&", "(", ")", "*", "+", ",", "-", ".", "/", ":",
            ";", "<", "=", ">", "?", "@", "[", "]", "^", "_", "{", "|", "}" };

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        try {
            String strLength = req.getParameter("length");
            int length = NumberUtils.toInt(strLength, 0);
            String[] characters = req.getParameterValues("characters");
            Locale locale = req.getLocale();

            StringBuilder bodyHtml = new StringBuilder();
            bodyHtml.append("<form action=\"strplusopr\" method=\"post\">");
            bodyHtml.append(getMsg("description.random.string.generator", locale));
            bodyHtml.append("<br><br>");
            bodyHtml.append(getMsg("label.character.count", locale) + ": ");
            bodyHtml.append("<br>");
            if (length > 0) {
                bodyHtml.append("<input type=\"text\" name=\"length\" size=\"6\" maxlength=\"6\" value=\"" + length
                        + "\">");
            } else {
                bodyHtml.append("<input type=\"text\" name=\"length\" size=\"6\" maxlength=\"6\">");
            }
            bodyHtml.append("<br><br>");
            bodyHtml.append("<p>" + getMsg("label.available.characters", locale) + "</p>");

            appendCheckBox(characters, locale, bodyHtml, ALL_NUMBERS, "label.numbers");
            appendCheckBox(characters, locale, bodyHtml, ALL_UPPER_CHARACTERS, "label.uppercase.characters");
            appendCheckBox(characters, locale, bodyHtml, ALL_LOWER_CHARACTERS, "label.lowercase.characters");
            appendCheckBox(characters, locale, bodyHtml, ALL_SIGNS, "label.signs");

            bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.submit", locale) + "\">");
            bodyHtml.append("<br><br>");

            if (length > 0) {
                // StringBuilder builder = new StringBuilder();
                String s = "";
                if (characters != null) {
                    java.util.Random rand = new java.util.Random();
                    log.info("Start Date: {}", new Date());
                    for (int i = 0; i < length && i < MAX_LENGTH; i++) {
                        s = s + characters[rand.nextInt(characters.length)];
                        // builder.append(characters[rand.nextInt(characters.length)]);
                    }
                    log.info("End Date: {}", new Date());
                }
                bodyHtml.append(getMsg("label.execution.result", locale));
                bodyHtml.append("<br><br>");
                // bodyHtml.append(encodeForHTML(builder.toString()));
                bodyHtml.append(encodeForHTML(s));
            } else {
                bodyHtml.append(getMsg("msg.enter.positive.number", locale));
            }
            bodyHtml.append("<br><br>");
            bodyHtml.append(getInfoMsg("msg.note.strplusopr", locale));
            bodyHtml.append("</form>");
            responseToClient(req, res, getMsg("title.strplusopr.page", locale), bodyHtml.toString());

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }

    private void appendCheckBox(String[] characters, Locale locale, StringBuilder bodyHtml, String[] allCharacters,
            String label) {
        bodyHtml.append("<p>" + getMsg(label, locale) + "</p>");
        bodyHtml.append("<p>");
        for (String allCharacter : allCharacters) {
            bodyHtml.append("<input type=\"checkbox\" name=\"characters\" value=\"");
            bodyHtml.append(allCharacter);
            if (characters == null || Arrays.asList(characters).contains(allCharacter)) {
                bodyHtml.append("\" checked=\"checked\">");
            } else {
                bodyHtml.append("\">");
            }
            bodyHtml.append(allCharacter);
            bodyHtml.append(" ");
        }
        bodyHtml.append("</p>");
    }
}
