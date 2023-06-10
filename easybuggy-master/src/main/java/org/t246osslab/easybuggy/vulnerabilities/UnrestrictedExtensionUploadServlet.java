package org.t246osslab.easybuggy.vulnerabilities;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.lang.StringUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;
import org.t246osslab.easybuggy.core.utils.MultiPartFileUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/ureupload" })
// 2MB, 10MB, 50MB
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, maxFileSize = 1024 * 1024 * 10, maxRequestSize = 1024 * 1024 * 50)
public class UnrestrictedExtensionUploadServlet extends AbstractServlet {

    // Name of the directory where uploaded files is saved
    private static final String SAVE_DIR = "uploadFiles";
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Locale locale = req.getLocale();

        StringBuilder bodyHtml = new StringBuilder();
        bodyHtml.append("<form method=\"post\" action=\"ureupload\" enctype=\"multipart/form-data\">");
        bodyHtml.append(getMsg("msg.convert.grayscale", locale));
        bodyHtml.append("<br><br>");
        bodyHtml.append("<input type=\"file\" name=\"file\" size=\"60\" /><br>");
        bodyHtml.append(getMsg("msg.select.upload.file", locale));
        bodyHtml.append("<br><br>");
        bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.upload", locale) + "\" />");
        bodyHtml.append("<br><br>");
        if (req.getAttribute("errorMessage") != null) {
            bodyHtml.append(req.getAttribute("errorMessage"));
        }
        bodyHtml.append(getInfoMsg("msg.note.unrestrictedextupload", locale));
        bodyHtml.append("</form>");
        responseToClient(req, res, getMsg("title.unrestrictedextupload.page", locale), bodyHtml.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Locale locale = req.getLocale();

        // Get absolute path of the web application
        String appPath = req.getServletContext().getRealPath("");

        // Create a directory to save the uploaded file if it does not exists
        String savePath = appPath + File.separator + SAVE_DIR;
        File fileSaveDir = new File(savePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdir();
        }

        // Save the file
        Part filePart;
        try {
            filePart = req.getPart("file");
        } catch (Exception e) {
            req.setAttribute("errorMessage", getErrMsg("msg.max.file.size.exceed", locale));
            doGet(req, res);
            return;
        }
        try {
            String fileName = MultiPartFileUtils.getFileName(filePart);
            if (StringUtils.isBlank(fileName)) {
                doGet(req, res);
                return;
            }
            boolean isConverted = MultiPartFileUtils.writeFile(filePart, savePath, fileName);

            if (!isConverted) {
                isConverted = convert2GrayScale(new File(savePath + File.separator + fileName).getAbsolutePath());
            }

            StringBuilder bodyHtml = new StringBuilder();
            if (isConverted) {
                bodyHtml.append(getMsg("msg.convert.grayscale.complete", locale));
                bodyHtml.append("<br><br>");
                bodyHtml.append("<img src=\"" + SAVE_DIR + "/" + fileName + "\">");
                bodyHtml.append("<br><br>");
            } else {
                bodyHtml.append(getErrMsg("msg.convert.grayscale.fail", locale));
            }
            bodyHtml.append("<INPUT type=\"button\" onClick='history.back();' value=\""
                    + getMsg("label.history.back", locale) + "\">");
            responseToClient(req, res, getMsg("title.unrestrictedextupload.page", locale), bodyHtml.toString());

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }

    // Convert color image into gray scale image.
    private boolean convert2GrayScale(String fileName) throws IOException {
        boolean isConverted = false;
        try {
            // Convert the file into gray scale image.
            BufferedImage image = ImageIO.read(new File(fileName));
            if (image == null) {
                log.warn("Cannot read upload file as image file, file name: " + fileName);
                return false;
            }

            // convert to gray scale
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int p = image.getRGB(x, y);
                    int a = (p >> 24) & 0xff;
                    int r = (p >> 16) & 0xff;
                    int g = (p >> 8) & 0xff;
                    int b = p & 0xff;

                    // calculate average
                    int avg = (r + g + b) / 3;

                    // replace RGB value with avg
                    p = (a << 24) | (avg << 16) | (avg << 8) | avg;

                    image.setRGB(x, y, p);
                }
            }
            // Output the image
            ImageIO.write(image, "png", new File(fileName));
            isConverted = true;
        } catch (Exception e) {
            // Log and ignore the exception
            log.warn("Exception occurs: ", e);
        }
        return isConverted;
    }
}
