package org.t246osslab.easybuggy.exceptions;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.t246osslab.easybuggy.core.servlets.AbstractServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/imoe" })
public class ImagingOpExceptionServlet extends AbstractServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        BufferedImage img = new BufferedImage(1, 40000, BufferedImage.TYPE_INT_RGB);
        AffineTransformOp flipAtop = new AffineTransformOp(AffineTransform.getScaleInstance(1, 1),
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        flipAtop.filter(img, null);
    }
}
