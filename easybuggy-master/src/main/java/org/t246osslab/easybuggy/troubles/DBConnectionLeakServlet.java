package org.t246osslab.easybuggy.troubles;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.t246osslab.easybuggy.core.dao.DBClient;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;
import org.t246osslab.easybuggy.core.utils.ApplicationUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/dbconnectionleak" })
public class DBConnectionLeakServlet extends AbstractServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Locale locale = req.getLocale();
        StringBuilder bodyHtml = new StringBuilder();
        try {
            final String dbUrl = ApplicationUtils.getDatabaseURL();
            final String dbDriver = ApplicationUtils.getDatabaseDriver();

            if (!StringUtils.isBlank(dbDriver)) {
                loadDbDriver(dbDriver);
            }
            bodyHtml.append(selectUsers(locale));
            if (StringUtils.isBlank(dbUrl) || dbUrl.startsWith("jdbc:derby:memory:")) {
                bodyHtml.append(getInfoMsg("msg.note.not.use.ext.db", locale));
            } else {
                bodyHtml.append(getInfoMsg("msg.note.db.connection.leak.occur", locale));
            }

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
            bodyHtml.append(getErrMsg("msg.unknown.exception.occur", new String[]{e.getMessage()}, locale));
            bodyHtml.append(e.getLocalizedMessage());
        } finally {
            responseToClient(req, res, getMsg("title.dbconnectionleak.page", locale), bodyHtml.toString());
        }
    }

    private void loadDbDriver(final String dbDriver) {
        try {
            Class.forName(dbDriver);
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }
    
    private String selectUsers(Locale locale) {
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String result = getErrMsg("msg.error.user.not.exist", locale);
        try {
            conn = DBClient.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select id, name, phone, mail from users where ispublic = 'true'");
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append("<tr><td>" + rs.getString("id") + "</td><td>" + rs.getString("name") + "</td><td>"
                        + rs.getString("phone") + "</td><td>" + rs.getString("mail") + "</td></tr>");
            }
            if (sb.length() > 0) {
                result = "<table class=\"table table-striped table-bordered table-hover\" style=\"font-size:small;\"><th>"
                        + getMsg("label.user.id", locale) + "</th><th>"
                        + getMsg("label.name", locale) + "</th><th>"
                        + getMsg("label.phone", locale) + "</th><th>"
                        + getMsg("label.mail", locale) + "</th>" + sb.toString() + "</table>";
            }
        } catch (Exception e) {
            result = getErrMsg("msg.db.access.error.occur", locale);
            log.error("Exception occurs: ", e);
            /* A DB connection leaks because the following lines are commented out.
        } finally {
            Closer.close(rs);
            Closer.close(stmt);
            Closer.close(conn);
            */
        }
        return result;
    }
}
