package org.t246osslab.easybuggy.troubles;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/deadlock" })
public class DeadlockServlet extends AbstractServlet {

    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    private boolean switchFlag = true;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Locale locale = req.getLocale();
        StringBuilder bodyHtml = new StringBuilder();
        try {
            if (req.getSession().getAttribute("dlpinit") == null) {
                req.getSession().setAttribute("dlpinit", "true");
            } else {
                todoRemove();
            }
            
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            long[] threadIds = bean.findDeadlockedThreads();
            if (threadIds != null) {
                bodyHtml.append(getMsg("msg.dead.lock.detected", locale));
                bodyHtml.append("<br><br>");
                bodyHtml.append("<table class=\"table table-striped table-bordered table-hover\" style=\"font-size:small;\">");
                ThreadInfo[] infos = bean.getThreadInfo(threadIds);
                for (ThreadInfo info : infos) {
                    bodyHtml.append("<tr><td>" + info.toString() + "</td></tr>");
                }
                bodyHtml.append("</table>");
            } else {
                bodyHtml.append(getMsg("msg.dead.lock.not.occur", locale));
                bodyHtml.append("<br><br>");
            }
            bodyHtml.append(getInfoMsg("msg.note.deadlock", locale));
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
            bodyHtml.append(getErrMsg("msg.unknown.exception.occur", new String[] { e.getMessage() }, locale));
        } finally {
            responseToClient(req, res, getMsg("title.deadlock.page", locale), bodyHtml.toString());
        }
    }

    private void todoRemove() {
        switchFlag = !switchFlag;
        if (switchFlag) {
            lock12();
        } else {
            lock21();
        }
    }

    private void lock12() {
        synchronized (lock1) {
            sleep();
            synchronized (lock2) {
                sleep();
            }
        }
    }

    private void lock21() {
        synchronized (lock2) {
            sleep();
            synchronized (lock1) {
                sleep();
            }
        }
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("Exception occurs: ", e);
        }
    }
}
