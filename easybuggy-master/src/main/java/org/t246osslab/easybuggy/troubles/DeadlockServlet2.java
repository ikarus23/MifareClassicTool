package org.t246osslab.easybuggy.troubles;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.dao.DBClient;
import org.t246osslab.easybuggy.core.model.User;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;
import org.t246osslab.easybuggy.core.utils.Closer;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/deadlock2" })
public class DeadlockServlet2 extends AbstractServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Locale locale = req.getLocale();
        StringBuilder bodyHtml = new StringBuilder();
        String updateResult = "";
        ArrayList<User> users;
        try {
            String order = getOrder(req);
            if ("POST".equals(req.getMethod())) {
                users = new ArrayList<User>();
                for (int j = 0;; j++) {
                    String uid = req.getParameter("uid_" + (j + 1));
                    if (uid == null) {
                        break;
                    }
                    User user = new User();
                    user.setUserId(uid);
                    user.setName(req.getParameter(uid + "_name"));
                    user.setPhone(req.getParameter(uid + "_phone"));
                    user.setMail(req.getParameter(uid + "_mail"));
                    users.add(user);
                }
                updateResult = updateUsers(users, locale);
            } else {
                users = selectUsers(order);
            }
            createHTMLUserTable(locale, bodyHtml, users, order, updateResult);

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
            bodyHtml.append(
                    getErrMsg("msg.unknown.exception.occur", new String[] { e.getMessage() }, locale));
            bodyHtml.append(e.getLocalizedMessage());
        } finally {
            responseToClient(req, res, getMsg("title.xxe.page", locale), bodyHtml.toString());
        }
    }

    private String getOrder(HttpServletRequest req) {
        String order = req.getParameter("order");
        if ("asc".equals(order)) {
            order = "desc";
        } else {
            order = "asc";
        }
        return order;
    }

    private void createHTMLUserTable(Locale locale, StringBuilder bodyHtml, ArrayList<User> users, String order,
            String updateResult) {

        bodyHtml.append("<form action=\"deadlock2\" method=\"post\">");
        bodyHtml.append(getMsg("msg.update.users", locale));
        bodyHtml.append("<br><br>");
        bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.update", locale) + "\">");
        bodyHtml.append("<br><br>");
        bodyHtml.append(
                "<table class=\"table table-striped table-bordered table-hover\" style=\"font-size:small;\"><th>");
        bodyHtml.append("<a href=\"/deadlock2?order=" + order + "\">" + getMsg("label.user.id", locale));
        if ("desc".equals(order)) {
            bodyHtml.append(" <span class=\"glyphicon glyphicon-triangle-bottom\"></span>");
        } else {
            bodyHtml.append(" <span class=\"glyphicon glyphicon-triangle-top\"></span>");
        }
        bodyHtml.append("</a></th><th>");
        bodyHtml.append(getMsg("label.name", locale) + "</th><th>");
        bodyHtml.append(getMsg("label.phone", locale) + "</th><th>");
        bodyHtml.append(getMsg("label.mail", locale) + "</th>");
        int rownum = 1;
        for (User user : users) {
            bodyHtml.append("<tr><td><input type=\"hidden\" name=\"uid_" + rownum + "\" value=\"" + user.getUserId()
                    + "\"></input>" + user.getUserId() + "</td>");
            bodyHtml.append("<td><input type=\"text\" name=\"" + user.getUserId() + "_name\" value=\"" + user.getName()
                    + "\"></input></td>");
            bodyHtml.append("<td><input type=\"text\" name=\"" + user.getUserId() + "_phone\" value=\""
                    + user.getPhone() + "\"></input></td>");
            bodyHtml.append("<td><input type=\"text\" name=\"" + user.getUserId() + "_mail\" value=\"" + user.getMail()
                    + "\"></input></td></tr>");
            rownum++;
        }
        bodyHtml.append("</table>");
        bodyHtml.append(updateResult);
        bodyHtml.append(getInfoMsg("msg.note.deadlock2", locale));
        bodyHtml.append("</form>");
    }

    private ArrayList<User> selectUsers(String order) {

        Statement stmt = null;
        Connection conn = null;
        ResultSet rs = null;
        ArrayList<User> users = new ArrayList<User>();
        try {
            conn = DBClient.getConnection();
            conn.setAutoCommit(true);

            stmt = conn.createStatement();
            rs = stmt.executeQuery("select * from users where ispublic = 'true' order by id " + ("desc".equals(order) ? "desc" : "asc"));
            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getString("id"));
                user.setName(rs.getString("name"));
                user.setPhone(rs.getString("phone"));
                user.setMail(rs.getString("mail"));
                users.add(user);
            }
        } catch (SQLException e) {
            log.error("SQLException occurs: ", e);
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        } finally {
            Closer.close(rs);
            Closer.close(stmt);
            Closer.close(conn);
        }
        return users;
    }

    private String updateUsers(ArrayList<User> users, Locale locale) {

        PreparedStatement stmt = null;
        Connection conn = null;
        int executeUpdate = 0;
        String resultMessage;
        try {
            conn = DBClient.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.prepareStatement("Update users set name = ?, phone = ?, mail = ? where id = ?");
            for (User user : users) {
                stmt.setString(1, user.getName());
                stmt.setString(2, user.getPhone());
                stmt.setString(3, user.getMail());
                stmt.setString(4, user.getUserId());
                executeUpdate = executeUpdate + stmt.executeUpdate();
                log.info(user.getUserId() +" is updated.");
                Thread.sleep(500);
            }
            conn.commit();
            resultMessage = getMsg("msg.update.records", new Object[] { executeUpdate }, locale) + "<br><br>";

        } catch (SQLTransactionRollbackException e) {
            resultMessage = getErrMsg("msg.deadlock.occurs", locale);
            log.error("SQLTransactionRollbackException occurs: ", e);
            rollbak(conn);
        } catch (SQLException e) {
            resultMessage = getErrMsg("msg.unknown.exception.occur", new String[] { e.getMessage() }, locale);
            log.error("SQLException occurs: ", e);
            rollbak(conn);
        } catch (Exception e) {
            resultMessage = getErrMsg("msg.unknown.exception.occur", new String[] { e.getMessage() }, locale);
            log.error("Exception occurs: ", e);
            rollbak(conn);
        } finally {
            Closer.close(stmt);
            Closer.close(conn);
        }
        return resultMessage;
    }

    private void rollbak(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                log.error("SQLException occurs: ", e1);
            }
        }
    }
}
