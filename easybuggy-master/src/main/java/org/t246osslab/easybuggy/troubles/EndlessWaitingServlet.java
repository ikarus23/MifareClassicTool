package org.t246osslab.easybuggy.troubles;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.math.NumberUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;
import org.t246osslab.easybuggy.core.utils.Closer;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/endlesswaiting" })
public class EndlessWaitingServlet extends AbstractServlet {

    private static final int MAX_COUNT = 100000;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        try {
            String strCount = req.getParameter("count");
            int count = NumberUtils.toInt(strCount, 0);
            Locale locale = req.getLocale();

            StringBuilder bodyHtml = new StringBuilder();
            bodyHtml.append("<form action=\"endlesswaiting\" method=\"post\">");
            bodyHtml.append(getMsg("description.endless.waiting", locale));
            bodyHtml.append("<br><br>");
            bodyHtml.append(getMsg("label.character.count", locale) + ": ");
            bodyHtml.append("<input type=\"text\" name=\"count\" size=\"5\" maxlength=\"5\">");
            bodyHtml.append("<br><br>");
            bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.submit", locale) + "\">");
            bodyHtml.append("<br><br>");

            if (count > 0) {
                /* create a batch file in the temp directory */
                File batFile = createBatchFile(count, req.getServletContext().getAttribute("javax.servlet.context.tempdir").toString());

                if (batFile == null) {
                    bodyHtml.append(getMsg("msg.cant.create.batch", locale));
                } else {
                    /* execte the batch */
                    ProcessBuilder pb = new ProcessBuilder(batFile.getAbsolutePath());
                    Process process = pb.start();
                    process.waitFor();
                    bodyHtml.append(getMsg("msg.executed.batch", locale) + batFile.getAbsolutePath());
                    bodyHtml.append("<br><br>");
                    bodyHtml.append(getMsg("label.execution.result", locale));
                    bodyHtml.append("<br><br>");
                    bodyHtml.append(printInputStream(process.getInputStream()));
                    bodyHtml.append(printInputStream(process.getErrorStream()));
                }
            } else {
                bodyHtml.append(getMsg("msg.enter.positive.number", locale));
                bodyHtml.append("<br>");
            }
            bodyHtml.append("<br>");
            bodyHtml.append(getInfoMsg("msg.note.endlesswaiting", locale));
            bodyHtml.append("</form>");
            responseToClient(req, res, getMsg("title.endlesswaiting.page", locale), bodyHtml.toString());

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }

    private File createBatchFile(int count, String tmpdir) throws IOException {
        BufferedWriter buffwriter = null;
        FileWriter fileWriter = null;
        File batFile = null;
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String batFileName;
            String firstLine;
            if (osName.toLowerCase().startsWith("windows")) {
                batFileName = "test.bat";
                firstLine = "@echo off";
            } else {
                batFileName = "test.sh";
                firstLine = "#!/bin/sh";
            }

            batFile = new File(tmpdir, batFileName);
            if (!batFile.setExecutable(true)) {
                log.debug("batFile.setExecutable(true) returns false.");
            }
            fileWriter = new FileWriter(batFile);
            buffwriter = new BufferedWriter(fileWriter);
            buffwriter.write(firstLine);
            buffwriter.newLine();

            for (int i = 0; i < count && i < MAX_COUNT; i++) {
                if (i % 100 == 0) {
                    buffwriter.newLine();
                    buffwriter.write("echo ");
                }
                buffwriter.write(String.valueOf(i % 10));
            }
            buffwriter.close();
            fileWriter.close();
            if (!osName.toLowerCase().startsWith("windows")) {
                Runtime runtime = Runtime.getRuntime();
                runtime.exec("chmod 777 " + batFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        } finally {
            Closer.close(fileWriter, buffwriter);
        }
        return batFile;
    }

    private String printInputStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        try {
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line + "<br>");
            }
        } finally {
            Closer.close(br);
            Closer.close(is);
        }
        return sb.toString();
    }
}
