/*
 * ImageCompare
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex;


import au.mapbot.apex.classes.DetectedCircle;
import au.mapbot.apex.classes.ImageMatching;
import au.mapbot.apex.classes.MapEnum;
import au.mapbot.apex.util.CircleUtil;
import au.mapbot.apex.util.ImageUtil;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageCompare
{
    public static void main( String[] args ) throws IOException{
//        Loader.load(opencv_java.class);
//
//        final ImageCompare imageCompare = new ImageCompare();
//        final Mat currentMap = ImageCompare.getMap("WorldsEdge");
//        final Mat minimap = ImageUtil.bufferedImage2Mat(ImageIO.read(Objects.requireNonNull(ImageCompare.class.getResourceAsStream("/maps/"+map+".png"))));
//
//        Imgcodecs.imread("src/main/resources/Output/minimap.png");
//        imageCompare.findInMap(minimap, currentMap);
    }

//    public static Mat getMap(final String map) throws IOException {
//        return ImageUtil.bufferedImage2Mat(ImageUtil.getImageFromResource("/maps/"+map+".png"));
//    }

    public static Mat getMap(final MapEnum map) throws IOException {
        return ImageUtil.bufferedImage2Mat(au.mapbot.apex.util.CustomMaps.load(map, true));
    }

    public ImageMatching findInMapClosestToLocation(final Mat minimap,
                                                    final Mat fullMap,
                                                    final int currentX,
                                                    final int currentY,
                                                    final int compassDirection,
                                                    final double xoffset,
                                                    final double yoffset) {

        //split image
        Mat minimapGrey = new Mat();
        Imgproc.cvtColor(minimap, minimapGrey, Imgproc.COLOR_RGB2GRAY);
//        Imgcodecs.imwrite("src/main/resources/Output/minimapCompare.png", minimapGrey);

        Mat fullMapGrey = new Mat();
        Imgproc.cvtColor(fullMap, fullMapGrey, Imgproc.COLOR_RGB2GRAY);

        final int match_method = Imgproc.TM_CCOEFF_NORMED;

        // / Create the result matrix
        int result_cols = fullMapGrey.cols() - minimapGrey.cols() + 1;
        int result_rows = fullMapGrey.rows() - minimapGrey.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

        // / Do the Matching and Normalize
        Imgproc.matchTemplate(fullMapGrey, minimapGrey, result, match_method);
        //Imgproc.threshold(result, result, 0.5, 1.0, Imgproc.THRESH_TOZERO);

        int currentBestX = 0, currentBestY = 0, currentBestDistance = 100;
        Double score = 0d;
        for (int i = 0; i < result_cols; i++) {
            for (int j = 0; j < result_rows; j++) {
                if (Arrays.stream(result.get(i, j)).max().getAsDouble() > 0.4) {
                    int detectedX = j + (minimap.width() / 2) + (int)xoffset;
                    int detectedY = i + (minimap.width() / 2) + (int)yoffset;

                    int distance = Math.abs(currentX - detectedX) + Math.abs(currentY - detectedY);

                    if ((((compassDirection >= 270 && compassDirection <= 359) || compassDirection <= 90)
                            && i + (minimap.width() / 2) + (int)yoffset <= currentY)
                    || ((compassDirection >= 0 && compassDirection <= 180)
                            && j + (minimap.width() / 2) + (int)xoffset >= currentX)
                    || ((compassDirection >= 90 && compassDirection <= 270)
                            && i + (minimap.width() / 2) + (int)yoffset >= currentY)
                    || ((compassDirection >= 180 && compassDirection <= 359)
                            && j + (minimap.width() / 2) + (int)xoffset <= currentX)){
                        if (distance < currentBestDistance) {
                            currentBestX = j;
                            currentBestY = i;
                            currentBestDistance = distance;
                            score = Arrays.stream(result.get(i, j)).max().getAsDouble();
                            System.out.println("Distance of possible: " + distance
                                    + " | lastX:" + currentX
                                    + " | lastY:" + currentY
                                    + " | detectX:" + detectedX
                                    + " | detectY:" + detectedY);
                        }
                    }
                    //System.out.println("Score: " + Arrays.stream(result.get(i, j)).max().getAsDouble() + " | x: " + i + " | y: " + j);
                }
            }
        }
        ImageMatching match = new ImageMatching();
        match.setPoint(new Point(currentBestX + (minimap.width()/2), currentBestY + (minimap.width()/2)));
        match.setScore(score);


        try{
            minimapGrey.release();
            fullMapGrey.release();
            result.release();
        }catch(Exception ex){
            //releasing resources manually
        }
        return match;
    }

//    public ImageMatching findInMap(final Mat minimap, final Mat fullMap, final Point currentLocation){
//        int additionalSize = 100;
//        Mat extendMap = new Mat(fullMap.rows() + (additionalSize*2), fullMap.cols() + (additionalSize*2), fullMap.type(), new Scalar(0, 0, 0));
//        Mat roi = new Mat(extendMap, new Rect(additionalSize, additionalSize, fullMap.cols(), fullMap.rows()));
//        fullMap.copyTo(roi);
//
//        int trimSize = 500;
//        int x = currentLocation.x - trimSize + additionalSize > 0 ? Double.valueOf(currentLocation.x + additionalSize - trimSize).intValue() : 0;
//        int y = currentLocation.y - trimSize + additionalSize > 0 ? Double.valueOf(currentLocation.y + additionalSize - trimSize).intValue() : 0;
//        int xw = x+1000;
//        int yw = y+1000;
//
//        if (x + additionalSize + trimSize >= extendMap.width()){
//            int offset = x + additionalSize + trimSize - extendMap.width();
//            x = x-offset;
//            xw = xw - offset;
//        }
//        if (y + additionalSize + trimSize >= extendMap.height()){
//            int offset = y + additionalSize + trimSize - extendMap.height();
//            y = y-offset;
//            yw = yw - offset;
//        }
//        Mat trim = extendMap.submat(x,xw,y,yw);
//        ImageMatching results = findInMap(minimap, trim);
//        results.getPoint().x = results.getPoint().x + x;
//        results.getPoint().y = results.getPoint().y + y;
//        return results;
//    }

//    public ImageMatching findInMapTargeted(final Mat minimap, final Mat fullMap) {
//        if (GameState.getUsedPoints().size() > 0){
//            return findInMap(minimap, fullMap, GameState.getUsedPoints().get(GameState.getUsedPoints().size()-1).getPoint());
//        }else{
//            return findInMap(minimap, fullMap);
//        }
//    }

    public double distance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
    }

    public ImageMatching findInMap(final Mat minimap, final Mat fullMap, final Point lastPoint, final boolean rotateMask, final boolean punish, final List<DetectedCircle> zones) {

        int additionalSize = 200;
        Mat extendMap = new Mat(fullMap.rows() + (additionalSize * 2), fullMap.cols() + (additionalSize * 2), fullMap.type(), new Scalar(0, 0, 0));
        Mat roi = new Mat(extendMap, new Rect(additionalSize, additionalSize, fullMap.cols(), fullMap.rows()));
        fullMap.copyTo(roi);

        Mat minimapGrey = new Mat();
        if (minimap.channels() == 1) {
            minimapGrey = minimap.clone(); // Directly use the single-channel image
        } else {
            Imgproc.cvtColor(minimap, minimapGrey, Imgproc.COLOR_RGB2GRAY);
        }
        //Imgproc.cvtColor(minimap, minimapGrey, Imgproc.COLOR_RGB2GRAY);
//        Imgcodecs.imwrite("src/main/resources/Output/minimapCompare.png", minimapGrey);

        Mat fullMapGrey = new Mat();
        Imgproc.cvtColor(extendMap, fullMapGrey, Imgproc.COLOR_RGB2GRAY);

        final int match_method = Imgproc.TM_CCOEFF_NORMED;

        // / Create the result matrix
        int result_cols = fullMapGrey.cols() - minimapGrey.cols() + 1;
        int result_rows = fullMapGrey.rows() - minimapGrey.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

        // / Do the Matching and Normalize
        if (!rotateMask) {
            Mat maskWhite = new Mat(minimapGrey.size(), CvType.CV_8UC1);
            Imgproc.threshold(minimapGrey, maskWhite, 210, 255, Imgproc.THRESH_BINARY);
            Core.bitwise_not(maskWhite, maskWhite);
            Imgproc.equalizeHist(minimapGrey, minimapGrey);
            Imgproc.equalizeHist(fullMapGrey, fullMapGrey);
            Imgproc.matchTemplate(fullMapGrey, minimapGrey, result, match_method, maskWhite);
        }else{
            Mat outputImage = new Mat(minimapGrey.size(), CvType.CV_8UC1);

            int channels = minimapGrey.channels();

            // Iterate over each pixel in the input image
            for (int row = 0; row < minimapGrey.rows(); row++) {
                for (int col = 0; col < minimapGrey.cols(); col++) {
                    double[] pixel = minimapGrey.get(row, col);

                    if (pixel.length < 3) {
                        // Handle case where the image is not in BGR format (e.g., grayscale)
                        if (pixel.length == 1) {
                            // Grayscale image
                            if (pixel[0] == 0) {
                                outputImage.put(row, col, 0); // Black
                            } else {
                                outputImage.put(row, col, 255); // White
                            }
                        } else {
                            // Unexpected format, set to white as a fallback
                            outputImage.put(row, col, 255);
                        }
                    } else {
                        // Handle BGR image
                        if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                            outputImage.put(row, col, 0); // Black
                        } else {
                            outputImage.put(row, col, 255); // White
                        }
                    }
                }
            }

            CircleUtil cu = new CircleUtil();
            //output testing
//            BufferedImage maskOut = cu.matToBufferedImage(outputImage);
//            BufferedImage minimapGreyOut = cu.matToBufferedImage(minimapGrey);

            SnapshotFromImage.writeImageToFile(cu.matToBufferedImage(outputImage), "minimap mask");
            Imgproc.equalizeHist(minimapGrey, minimapGrey);
            Imgproc.equalizeHist(fullMapGrey, fullMapGrey);
            Imgproc.matchTemplate(fullMapGrey, minimapGrey, result, match_method, outputImage);

            try{
                outputImage.release();
            } catch (Exception e) {
                //clean up Mat
            }
        }

        // adjust score to scale score down the further from previous point
        if (lastPoint != null) {
                for (int i = 0; i < result.width(); i++) {
                    for (int j = 0; j < result.height(); j++) {


                        double[] x = result.get(j, i);
                        double offsetScore = x[0];
                        if (lastPoint.x != 0 && lastPoint.y != 0) {
                            //grid points are for 0,0 of minimap image so need to offset by actual location offset below
                            Point p1 = new Point(lastPoint.x - (double) (minimap.width() / 2) + additionalSize, lastPoint.y - (double) (minimap.height() / 2) + additionalSize);
                            Point p2 = new Point(i, j);
                            double distance = distance(p1, p2);
                            if (punish) {
                                offsetScore = x[0] - (distance / 3800); //punish further away points
                            }
                            double closerGood = 0.1 - (distance / 1500); //reward closer points
                            if (closerGood > 0) {
                                offsetScore = offsetScore + closerGood;
                            }
                        }

                        //if you are within zones give more confidence (the more zones and later in game this stacks)
                        if (zones != null) {
                            for (DetectedCircle ring : zones) {
                                double distanceToZoneCenter = distance(new Point(i, j), ring.getCenter());
                                if (distanceToZoneCenter <= ring.getRadius()) {
                                    // Add 0.5 for each zone player is inside
                                    offsetScore += 0.05;
                                }
                            }
                        }
                        x = new double[]{offsetScore};
                        result.put(j, i, x);
                    }
                }

        }

        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        ImageMatching match = new ImageMatching();
        double x = mmr.maxLoc.x + (double)(minimap.width()/2) - additionalSize;
        double y = mmr.maxLoc.y + (double)(minimap.height()/2) - additionalSize;
        match.setPoint(new Point(x, y));
        match.setMat(minimap);

        if (x > 5 && x < 2043 && y > 5 && y < 2043){
            match.setScore(mmr.maxVal);
        }else{
            match.setScore(0d);
        }

        try{
            minimapGrey.release();
            fullMapGrey.release();
            result.release();
            extendMap.release();
            roi.release();
        }catch(Exception ex){
            //releasing resources manually
        }

        return match;
    }
//    public ImageMatching findInMap(final Mat minimap, final Mat fullMap) {
//        return  findInMap(minimap, fullMap, null);
//    }


    public ImageMatching findInMapBasic(final Mat imageToFind, final Mat largerImage) {
        Mat imageToFindGrey = new Mat();
        if (imageToFind.channels() == 1) {
            imageToFindGrey = imageToFind.clone(); // Directly use the single-channel image
        } else {
            Imgproc.cvtColor(imageToFind, imageToFindGrey, Imgproc.COLOR_RGB2GRAY);
        }
        //Imgproc.cvtColor(imageToFind, imageToFindGrey, Imgproc.COLOR_RGB2GRAY);

        Mat largerImageGrey = new Mat();
        if (largerImage.channels() == 1) {
            largerImageGrey = largerImage.clone(); // Directly use the single-channel image
        } else {
            Imgproc.cvtColor(largerImage, largerImageGrey, Imgproc.COLOR_RGB2GRAY);
        }
        //Imgproc.cvtColor(largerImage, largerImageGrey, Imgproc.COLOR_RGB2GRAY);

        final int match_method = Imgproc.TM_CCOEFF_NORMED;

        // / Create the result matrix
        int result_cols = largerImageGrey.cols() - imageToFindGrey.cols() + 1;
        int result_rows = largerImageGrey.rows() - imageToFindGrey.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

        // / Do the Matching and Normalize
        Imgproc.matchTemplate(largerImageGrey, imageToFindGrey, result, match_method);

        // / Localizing the best match with minMaxLoc
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        ImageMatching match = new ImageMatching();
        double x = mmr.maxLoc.x;
        double y = mmr.maxLoc.y;
        match.setPoint(new Point(x, y));
        match.setMat(imageToFind);
        match.setScore(mmr.maxVal);

        return match;
    }

    public List<Point> findMultipleTemplateInImage(BufferedImage largeImageInput, BufferedImage templateImageInput, double matchThreshold) throws IOException {

        Mat largeImage = ImageUtil.bufferedImage2Mat(largeImageInput);
        Mat templateImage = ImageUtil.bufferedImage2Mat(templateImageInput);

//        Mat largeImageClone = largeImage.clone();

        // Convert images to grayscale
        Mat largeImageGray = new Mat();
        Mat templateImageGray = new Mat();
        Imgproc.cvtColor(largeImage, largeImageGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(templateImage, templateImageGray, Imgproc.COLOR_BGR2GRAY);

        // Ensure both images are of the same type
        Mat largeImageIn = new Mat();
        Mat templateImageIn = new Mat();
        largeImageGray.convertTo(largeImageIn, CvType.CV_32F);
        templateImageGray.convertTo(templateImageIn, CvType.CV_32F);

        Mat result = new Mat();
        int resultCols = largeImageIn.cols() - templateImageIn.cols() + 1;
        int resultRows = largeImageIn.rows() - templateImageIn.rows() + 1;
        result.create(resultRows, resultCols, CvType.CV_32FC1);

        Imgproc.matchTemplate(largeImageIn, templateImageIn, result, Imgproc.TM_CCOEFF_NORMED, templateImageIn);

        Mat mask = new Mat();
        Core.inRange(result, new Scalar(matchThreshold), new Scalar(1.0), mask);

        List<Point> matchLocations = new ArrayList<>();
        for (int y = 0; y < result.rows(); y++) {
            for (int x = 0; x < result.cols(); x++) {
                if (mask.get(y, x)[0] > 0) {
                    matchLocations.add(new Point(x, y));
                    double score = result.get(y, x)[0];
                    System.out.println("Score: " + score + " at location (" + x + ", " + y + ")");
                }
            }
        }

        return matchLocations;
    }
}
