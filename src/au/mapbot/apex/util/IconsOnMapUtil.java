/*
 * IconsOnMapUtil
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;

import au.mapbot.apex.classes.ImageMatching;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IconsOnMapUtil {

    public BufferedImage getScaledBufferedImage(BufferedImage originalImage, int width, int height) {
        // Get scaled instance of the image
        Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        // Create a new BufferedImage to draw the scaled image onto
        BufferedImage scaledBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Draw the scaled image onto the BufferedImage
        Graphics2D g2d = scaledBufferedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        return scaledBufferedImage;
    }

    // Convert BufferedImage to Grayscale Mat
    public Mat bufferedImageToGrayMat(BufferedImage bi) {
        Mat mat;
        int type = bi.getType();

        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC1);
            BufferedImage grayImage = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            grayImage.getGraphics().drawImage(bi, 0, 0, null);
            byte[] data = ((DataBufferByte) grayImage.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        } else {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC1);
            byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        }
        return mat;
    }

    public Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat;
        int type = bi.getType();

        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC4);
            int[] data = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
            byte[] bytes = new byte[data.length * 4];
            for (int i = 0; i < data.length; i++) {
                bytes[i*4] = (byte) (data[i]);
                bytes[i*4 + 1] = (byte) (data[i] >> 8);
                bytes[i*4 + 2] = (byte) (data[i] >> 16);
                bytes[i*4 + 3] = (byte) (data[i] >> 24);
            }
            mat.put(0, 0, bytes);
        } else {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
            byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        }
        return mat;
    }

    public java.util.List<ImageMatching> findInMapNew(Mat largeImage, Mat templateImage, Mat maskMat, BufferedImage allowedLocations, double threshold) throws IOException {
        // Ensure both images are of the same type
        // Check if images have 4 channels (transparency)
        if (largeImage.channels() == 4) {
            // Split channels and use only the BGR channels, ignoring the alpha channel
            java.util.List<Mat> channels = new ArrayList<>();
            Core.split(largeImage, channels);
            largeImage = new Mat();
            Core.merge(channels.subList(0, 3), largeImage); // Merge only the first 3 channels
        }

        if (templateImage.channels() == 4) {
            // Split channels and use only the BGR channels, ignoring the alpha channel
            java.util.List<Mat> channels = new ArrayList<>();
            Core.split(templateImage, channels);
            templateImage = new Mat();
            Core.merge(channels.subList(0, 3), templateImage); // Merge only the first 3 channels
        }

        // Convert images to grayscale
        if (largeImage.channels() == 1) {
            //image is already grey
//            gray = extendedInputImage.clone(); // Directly use the single-channel image
        } else {
            Imgproc.cvtColor(largeImage, largeImage, Imgproc.COLOR_RGB2GRAY);
        }

        if (templateImage.channels() == 1) {
            //image is already grey
//            gray = extendedInputImage.clone(); // Directly use the single-channel image
        } else {
            Imgproc.cvtColor(templateImage, templateImage, Imgproc.COLOR_RGB2GRAY);
        }

        Mat largeImageIn = new Mat();
        Mat templateImageIn = new Mat();
        largeImage.convertTo(largeImageIn, CvType.CV_32F);
        templateImage.convertTo(templateImageIn, CvType.CV_32F);

        Mat result = new Mat();
        int resultCols = largeImageIn.cols() - templateImageIn.cols() + 1;
        int resultRows = largeImageIn.rows() - templateImageIn.rows() + 1;
        result.create(resultRows, resultCols, CvType.CV_32F);

        Imgproc.matchTemplate(largeImageIn, templateImageIn, result, Imgproc.TM_CCOEFF_NORMED, maskMat);
//        Imgproc.matchTemplate(largeImageIn, templateImageIn, result, Imgproc.TM_SQDIFF_NORMED);

        Mat mask = new Mat();
        Core.inRange(result, new Scalar(threshold), new Scalar(1.0), mask);
//        Core.inRange(result, new Scalar(0.0), new Scalar(0.3), mask);

        java.util.List<ImageMatching> matchLocations = new ArrayList<>();
        for (int y = 0; y < result.rows(); y++) {
            for (int x = 0; x < result.cols(); x++) {
                if (mask.get(y, x)[0] > 0) {
                    if (isAllowedLocation(new org.opencv.core.Point(x,y),allowedLocations, templateImage)){
                        ImageMatching match = new ImageMatching();
                        match.setScore(result.get(y, x)[0]);

                        //need to scale everything up from size as window can be resized and need to standardise
                        double scaleUp = (double)2048/(double)largeImage.width();
                        match.setPoint(new org.opencv.core.Point(x*scaleUp, y*scaleUp));

                        if (addPoint(match, matchLocations)){
                            matchLocations.add(match);
                            System.out.println("Score: " + match.getScore() + " at location (" + match.getPoint().x + ", " +  match.getPoint().y + ")");
                        }
                    }
                }
            }
        }

        try{
            largeImage.release();
            templateImage.release();
            largeImageIn.release();
            templateImageIn.release();
            mask.release();
            result.release();
        } catch (Exception e) {
            //
        }

        return matchLocations;
    }
    public boolean isAllowedLocation(final Point p, BufferedImage mapBW, Mat templateImage){
        int xValue = Double.valueOf(p.x).intValue() + templateImage.width()/2;
        int yValue = Double.valueOf(p.y).intValue() + templateImage.height()/2;

        if (xValue > mapBW.getWidth() || yValue > mapBW.getHeight() || xValue < 0 || yValue < 0){
            return false;
        }

        Color wePixelColor = new Color(mapBW.getRGB(xValue, yValue));
        if (wePixelColor.equals(Color.BLACK)) {
            return true;
        }
        return false;
    }

    public boolean addPoint(ImageMatching newPoint, List<ImageMatching> oldPoints) {
        for (ImageMatching p : oldPoints) {
            if (Math.abs(p.getPoint().x - newPoint.getPoint().x) <= 40 && Math.abs(p.getPoint().y - newPoint.getPoint().y) <= 40) {
                if (newPoint.getScore() > p.getScore()){
                    oldPoints.remove(p);
                    return true;
                }
                return false; // There is a point within the 50-unit range
            }
        }
        return true; // Point added successfully
    }
}
