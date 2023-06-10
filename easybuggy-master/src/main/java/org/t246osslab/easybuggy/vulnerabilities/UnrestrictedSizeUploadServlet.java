package org.t246osslab.easybuggy.vulnerabilities;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.t246osslab.easybuggy.core.servlets.AbstractServlet;
import org.t246osslab.easybuggy.core.utils.MultiPartFileUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/ursupload" })
@MultipartConfig
public class UnrestrictedSizeUploadServlet extends AbstractServlet {

    // Name of the directory where uploaded files is saved
    private static final String SAVE_DIR = "uploadFiles";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Locale locale = req.getLocale();

        StringBuilder bodyHtml = new StringBuilder();
        bodyHtml.append("<form method=\"post\" action=\"ursupload\" enctype=\"multipart/form-data\">");
        bodyHtml.append(getMsg("msg.reverse.color", locale));
        bodyHtml.append("<br><br>");
        bodyHtml.append("<input type=\"file\" name=\"file\" size=\"60\" /><br>");
        bodyHtml.append(getMsg("msg.select.upload.file", locale));
        bodyHtml.append("<br><br>");
        bodyHtml.append("<input type=\"submit\" value=\"" + getMsg("label.upload", locale) + "\" />");
        bodyHtml.append("<br><br>");
        if (req.getAttribute("errorMessage") != null) {
            bodyHtml.append(req.getAttribute("errorMessage"));
        }
        bodyHtml.append(getInfoMsg("msg.note.unrestrictedsizeupload", locale));
        bodyHtml.append("</form>");
        responseToClient(req, res, getMsg("title.unrestrictedsizeupload.page", locale), bodyHtml.toString());
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

        try {
            // Save the file
            final Part filePart = req.getPart("file");
            String fileName = MultiPartFileUtils.getFileName(filePart);
            if (StringUtils.isBlank(fileName)) {
                doGet(req, res);
                return;
            } else if (!isImageFile(fileName)) {
                req.setAttribute("errorMessage", getErrMsg("msg.not.image.file", locale));
                doGet(req, res);
                return;
            }
            boolean isConverted = MultiPartFileUtils.writeFile(filePart, savePath, fileName);

            // Reverse the color of the upload image
            if (!isConverted) {
                isConverted = reverseColor(new File(savePath + File.separator + fileName).getAbsolutePath());
            }

            StringBuilder bodyHtml = new StringBuilder();
            if (isConverted) {
                bodyHtml.append(getMsg("msg.reverse.color.complete", locale));
                bodyHtml.append("<br><br>");
                bodyHtml.append("<img src=\"" + SAVE_DIR + "/" + fileName + "\">");
                bodyHtml.append("<br><br>");
            } else {
                bodyHtml.append(getErrMsg("msg.reverse.color.fail", locale));
            }
            bodyHtml.append("<INPUT type=\"button\" onClick='history.back();' value=\""
                    + getMsg("label.history.back", locale) + "\">");
            responseToClient(req, res, getMsg("title.unrestrictedsizeupload.page", locale), bodyHtml.toString());

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        }
    }

    private boolean isImageFile(String fileName) {
        return Arrays.asList("png", "gif", "jpg", "jpeg", "tif", "tiff", "bmp").contains(
                FilenameUtils.getExtension(fileName));
    }

    // Reverse the color of the image file
    private boolean reverseColor(String fileName) throws IOException {
        boolean isConverted = false;
        try {
            BufferedImage image = ImageIO.read(new File(fileName));
            WritableRaster raster = image.getRaster();
            int[] pixelBuffer = new int[raster.getNumDataElements()];
            for (int y = 0; y < raster.getHeight(); y++) {
                for (int x = 0; x < raster.getWidth(); x++) {
                    raster.getPixel(x, y, pixelBuffer);
                    pixelBuffer[0] = ~pixelBuffer[0];
                    pixelBuffer[1] = ~pixelBuffer[1];
                    pixelBuffer[2] = ~pixelBuffer[2];
                    raster.setPixel(x, y, pixelBuffer);
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
