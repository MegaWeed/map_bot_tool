/*
 * CircleUtil
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;

import au.mapbot.apex.Ocr;
import au.mapbot.apex.classes.DetectedCircle;
import au.mapbot.apex.classes.MapEnum;
import au.mapbot.apex.classes.SaveFile;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static au.mapbot.apex.classes.MapEnum.*;

public class CircleUtil {

    //0.92 found via trail error on olympus only
    public double scaleMulti = 1;
    public MapEnum currentMap = MapEnum.WORLDSEDGE;

//numbers come from minimap scaling in ProcessCapture
    public void setMap(MapEnum map) {
        currentMap = map;
        switch (map) {
            case KINGSCANYON:
                scaleMulti = 1;
                break;
            case OLYMPUS:
                scaleMulti = 1;
                break;
            case BROKENMOON:
                scaleMulti = 1; // TODO
                break;
            case STORMPOINT:
                scaleMulti =  1;
                break;
            case STORMPOINT_19:
                scaleMulti =  1;
                break;
            case WORLDSEDGE:
                scaleMulti = 1;
                break;
            case DISTRICT:
                scaleMulti = 1;
                break;
        }
    }

    public int getRing1Radius() {
        switch (currentMap){
            case OLYMPUS:
                return 500;
            case STORMPOINT:
            case STORMPOINT_19:
                return 614;
            case WORLDSEDGE:
                return 606;
            case BROKENMOON:
                return 760;
            case KINGSCANYON:
                return 550;
            case DISTRICT: //TODO
                return 550;
            default:
                return 500;
        }
    }

    public int getRing2Radius() {
        switch (currentMap){
            case OLYMPUS:
            case WORLDSEDGE:
                return 294;
            case STORMPOINT:
            case STORMPOINT_19:
                return 300;
            case BROKENMOON:
                return 308;
            case KINGSCANYON:
                return 326;
            case DISTRICT: //TODO
                return 326;
            default:
                return 0;
        }
    }

    public int getRing3Radius() {
        switch (currentMap){
            case OLYMPUS:
            case WORLDSEDGE:
                return 182;
            case STORMPOINT:
            case STORMPOINT_19:
                return 160;
            case BROKENMOON:
                return 190;
            case KINGSCANYON:
                return 200;
            case DISTRICT: //TODO
                return 200;
            default:
                return 0;
        }
    }

    public int getRing4Radius() {
        switch (currentMap){
            case OLYMPUS:
            case WORLDSEDGE:
                return 90;
            case STORMPOINT:
            case STORMPOINT_19:
                return 78;
            case BROKENMOON:
                return 94;
            case KINGSCANYON:
                return 100;
            case DISTRICT: //TODO
                return 100;
            default:
                return 0;
        }
    }

    public int getRing5Radius() {
        switch (currentMap){
            case OLYMPUS:
            case WORLDSEDGE:
            case BROKENMOON:
                return 46;
            case STORMPOINT:
            case STORMPOINT_19:
                return 38;
            case KINGSCANYON:
                return 50;
            case DISTRICT: //TODO
                return 50;
            default:
                return 0;
        }
    }


    public List<DetectedCircle> updateList(double currentX, double currentY, Mat minimap, List<DetectedCircle> newCircles, List<DetectedCircle> allCircles, BufferedImage currentMapLegalBWImage){

        for (DetectedCircle circle: newCircles){
            double halfMinimap = (double)minimap.width()/2;
            double relativeCenterX = currentX - halfMinimap + (circle.getCenter().x - circle.getExtendValue());
            double relativeCenterY = currentY - halfMinimap + (circle.getCenter().y - circle.getExtendValue());

            if (relativeCenterX >=0 && relativeCenterY >=0  && relativeCenterX < 2048 && relativeCenterY < 2048) {

                Color wePixelColor = new Color(currentMapLegalBWImage.getRGB(Double.valueOf(relativeCenterX).intValue(), Double.valueOf(relativeCenterY).intValue()));
                if (wePixelColor.equals(Color.BLACK)) {
                    allCircles.add(new DetectedCircle(new Point(relativeCenterX, relativeCenterY), circle.getRadius(), circle.getScore(), circle.getExtendValue(), circle.getZone(), circle.isClosing()));
                }
            }
        }
        allCircles.sort((c1, c2) -> Double.compare(c2.getScore(), c1.getScore()));

//        allCircles.sort((c1, c2) -> Double.compare(c2.getRadius(), c1.getRadius()));

        return allCircles;
    }

    public Point computeCentroid(List<DetectedCircle> points) {
        double centerX = 0;
        double centerY = 0;
        for (DetectedCircle p : points) {
            centerX += p.getCenter().x;
            centerY += p.getCenter().y;
        }
        return new Point(centerX / points.size(), centerY / points.size());
    }

    public List<DetectedCircle> removeOutliers(List<DetectedCircle> points, double threshold) {

        List<DetectedCircle> outPoints = new ArrayList<>(points);
        boolean done = false;
        while (!done && outPoints.size()> 2){
            done = true;
            double max = 0;
            int maxID = 0;
            Point centroid = computeCentroid(outPoints);
            for (int i = 0; i < outPoints.size(); i++){
                double howFar = distance(outPoints.get(i).getCenter(), centroid);
                if (howFar > max){
                    maxID = i;
                    max = howFar;
                }
            }
            if (max > threshold){
                outPoints.remove(maxID);
                done = false;
            }
        }
        return outPoints;
    }

    public double distance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
    }

    public DetectedCircle getZoneExact(List<DetectedCircle> allCircles, int zone, int radius, int currentRound) {
        List<DetectedCircle> zoneCircles = allCircles.stream().filter(x -> x.getZone() == zone).collect(Collectors.toList());

        List<DetectedCircle> mapReadZones = zoneCircles.stream().filter(s -> s.isPerfect()).collect(Collectors.toList());
        if (!mapReadZones.isEmpty()){
            mapReadZones = removeOutliers(mapReadZones, (double) radius / 4);

            if (mapReadZones.size() >= 1) {
                double totalX = 0.0;
                double totalY = 0.0;

                for (DetectedCircle circle : mapReadZones) {
                    totalX += circle.getCenter().x;
                    totalY += circle.getCenter().y;
                }
                DetectedCircle dc = new DetectedCircle(new Point(totalX / mapReadZones.size(), totalY / mapReadZones.size()), radius, 1, 1, zone, false);
                dc.setPerfect(true);
                return dc;
            }
        }

        zoneCircles = removeOutliers(zoneCircles, (double) radius / 4);
        if (zoneCircles.size() >= 3 ||
                (zoneCircles.size() > 0 && currentRound > zone)) {
            double totalX = 0.0;
            double totalY = 0.0;

            for (DetectedCircle circle : zoneCircles) {
                totalX += circle.getCenter().x;
                totalY += circle.getCenter().y;
            }
            return new DetectedCircle(new Point(totalX / zoneCircles.size(), totalY / zoneCircles.size()), radius, 1, 1, zone, false);
        }

        return null;
    }
    public DetectedCircle getZoneExtrapolate(List<DetectedCircle> allCircles, int zone, int target){

        List<DetectedCircle> zoneCircles = allCircles.stream().filter(x -> x.getZone() == zone).filter(x -> x.isClosing()).collect(Collectors.toList());

        zoneCircles.sort((c1, c2) -> Double.compare(c2.getCenter().x+c2.getCenter().y, c1.getCenter().x+c1.getCenter().y));
        Collections.reverse(zoneCircles);

        List<DetectedCircle> zoneCirclesScaled = new ArrayList<>();
        DetectedCircle previousCircle = null;
        if (zoneCircles.size()>1) { //need atleast 2 to extrapolate
            for (DetectedCircle circle : zoneCircles) {
                if (previousCircle != null){
                    if (distance(circle.getCenter(), previousCircle.getCenter()) < (double)target/3){
                        double xRatioChange = circle.getCenter().x / previousCircle.getCenter().x;
                        double yRatioChange = circle.getCenter().y / previousCircle.getCenter().y;
                        double radiusChange = circle.getRadius() - previousCircle.getRadius();

                        int howFarToGo = target - circle.getRadius();
                        double howManyTimesPreviousChange = (double) howFarToGo / radiusChange;
                        double newX = circle.getCenter().x + xRatioChange * howManyTimesPreviousChange;
                        double newY = circle.getCenter().y + yRatioChange * howManyTimesPreviousChange;

                        if (newX-target >=0 && newY-target >=0  && newX+target < 2048 && newY+target < 2048) {
                            zoneCirclesScaled.add(new DetectedCircle(
                                    new Point(newX, newY),
                                    target,
                                    circle.getScore(),
                                    circle.getExtendValue(),
                                    zone,
                                    circle.isClosing()
                            ));
                        }
                    }
                }
                previousCircle = circle;
            }

            zoneCirclesScaled = removeOutliers(zoneCirclesScaled, (double)target/3);


            if (zoneCirclesScaled.size()>0) {
                double totalX = 0.0;
                double totalY = 0.0;

                for (DetectedCircle circle : zoneCirclesScaled) {
                    totalX += circle.getCenter().x;
                    totalY += circle.getCenter().y;
                }

                return new DetectedCircle(new Point(totalX / zoneCirclesScaled.size(), totalY / zoneCirclesScaled.size()), target, 1, 1, zone, true);
            }
            return null;
        }
        return null;
    }


    public List<DetectedCircle> getCirclesInMinimap(@Nonnull final Mat minimap, int round, boolean closing){

        int minRadius = 100;
        int maxRadius = 500;
//        int variableDistance = 15;
        List<DetectedCircle> foundCircles = new ArrayList<>();
        if (closing){
            if (round == 1){ // ring 1 visible + closing
//                minRadius = getRing1Radius() + variableDistance;
//                maxRadius = getRing1Radius() * 2;
//                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true));
                minRadius = getRing1Radius() - 45;
                maxRadius = getRing1Radius() + 45;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 1, true, 40,15,10));
                //possible ring 2 shown if beacon
//                minRadius = getRing2Radius() - 20;
//                maxRadius = getRing2Radius() + 20;
//                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 2, true, 40,15,10));
            }else if(round == 2){ // ring 2 visible + closing
//                minRadius = getRing2Radius() + variableDistance;
//                maxRadius = getRing1Radius();
//                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true));
                minRadius = getRing2Radius() - 20;
                maxRadius = getRing2Radius() + 20;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true, 40,15,10));
            }else if(round == 3){ // ring 3 visible + closing
//                minRadius = getRing3Radius() + variableDistance;
//                maxRadius = getRing2Radius();
//                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true));
                minRadius = getRing3Radius() - 15;
                maxRadius = getRing3Radius() + 15;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true, 40,15,10));
            }else if(round == 4){ // ring 4 visible + closing
//                minRadius = getRing4Radius() + variableDistance;
//                maxRadius = getRing3Radius();
//                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true));
                minRadius = getRing4Radius() - 10;
                maxRadius = getRing4Radius() + 10;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true, 40,15,10));
            }else if(round == 5){ // ring 5 visible + closing
//                minRadius = getRing5Radius() + variableDistance;
//                maxRadius = getRing4Radius();
//                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true));
                minRadius = getRing5Radius() - 5;
                maxRadius = getRing5Radius() + 5;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true, 40,15,10));
            }else if(round == 6){ // final ring no actual zones shown
                return new ArrayList<>();
            }
        }else{
            if (round == 1){ // ring 1 visible
                minRadius = getRing1Radius() - 45;
                maxRadius = getRing1Radius() + 45;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 1, true, 40,15,10));
                //possible ring 2 shown if beacon
                minRadius = getRing2Radius() - 20;
                maxRadius = getRing2Radius() + 20;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 2, false, 40,15,10));
            }else if(round == 2){ // ring 1+2 visible
                minRadius = getRing1Radius() - 45;
                maxRadius = getRing1Radius() + 45;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 1, false, 40,15,10));
                minRadius = getRing2Radius() - 20;
                maxRadius = getRing2Radius() + 20;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 2, false, 40,15,10));
            }else if(round == 3){ // ring 2+3 visible
                minRadius = getRing2Radius() - 20;
                maxRadius = getRing2Radius() + 20;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 2, false, 40,15,10));
                minRadius = getRing3Radius() - 15;
                maxRadius = getRing3Radius() + 15;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 3, false, 40,15,10));
            }else if(round == 4){ // ring 3+4 visible
                minRadius = getRing3Radius() - 15;
                maxRadius = getRing3Radius() + 15;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 3, false, 40,15,10));
                minRadius = getRing4Radius() - 10;
                maxRadius = getRing4Radius() + 10;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 4, false, 40,15,10));
            }else if(round == 5){ // ring 4+5 visible
                minRadius = getRing4Radius() - 10;
                maxRadius = getRing4Radius() + 10;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 4, false, 40,15,10));
                minRadius = getRing5Radius() - 5;
                maxRadius = getRing5Radius() + 5;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 5, false, 40,15,10));
            }else if(round == 6){ // ring 5
                minRadius = getRing5Radius() - 5;
                maxRadius = getRing5Radius() + 5;
                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, 5, false, 40,15,10));
            }
        }
        return foundCircles;
    }

    private List<DetectedCircle> getCircles(Mat minimap, int minRadius, int maxRadius, int round, boolean closing, int canny, int p1, int p2) {
        //results seem to be much better, and runs much faster
        BufferedImage bi = matToBufferedImage(minimap);

        Mat minimapSmall = ImageUtil.resizeMat(minimap, minimap.width()/2, minimap.height()/2);

        List<DetectedCircle> circleList = new ArrayList<>();
        if (round == 1  || round == 2){
            circleList.addAll(getCircles(minimapSmall, (minRadius / 2), (maxRadius / 2), round, closing, canny, canny, p1, p2, 1000, 4));
        }else {
            circleList.addAll(getCircles(minimapSmall, (minRadius / 2), (maxRadius / 2), round, closing, canny, canny, p1, p2, 165, 4));
        }
//        circleList.addAll(getCircles(minimapSmall, minRadius/2, maxRadius/2, round, closing, 50,50,15,10));
        return circleList;
    }

    private List<DetectedCircle> getCircles(Mat minimap, int minRadius, int maxRadius, int round, boolean closing, int canny1, int canny2, int houghP1, int houghP2, double threshold, int dilate ){

//        System.out.println("Circles for round: " + round);
        //1. Minimap to extended image
        Mat extendedInputImage = minimap.clone();
        int offsetX = 0;
        int offsetY = 0;
        int newWidth = minimap.cols() + 10 * maxRadius;
        int newHeight = minimap.rows() + 10 * maxRadius;
        extendedInputImage = new Mat(newHeight, newWidth, minimap.type(), new Scalar(0, 0, 0));
        offsetX = maxRadius;
        offsetY = maxRadius;
        Mat roi = new Mat(extendedInputImage, new Rect(offsetX, offsetY, minimap.cols(), minimap.rows()));
        minimap.copyTo(roi);

        // 2. grey extended image
        Mat gray = new Mat();
        if (extendedInputImage.channels() == 1) {
            gray = extendedInputImage.clone(); // Directly use the single-channel image
        } else {
            Imgproc.cvtColor(extendedInputImage, gray, Imgproc.COLOR_RGB2GRAY);
        }
        //Imgproc.cvtColor(extendedInputImage, gray, Imgproc.COLOR_BGR2GRAY);

        // 3. hough Trough on extended grey image
        Mat circles = new Mat();
        Imgproc.HoughCircles(gray, circles, Imgproc.HOUGH_GRADIENT, 1, 2, houghP1, houghP2, minRadius, Double.valueOf(maxRadius*1.5).intValue());

        // 4. find edges, dont expand until after canny
        Mat circleEdges = new Mat();
        Mat x1 = minimap.clone();
        Mat x2 = new Mat();
        if (x1.channels() == 1) {
            x2 = x1.clone(); // Directly use the single-channel image
        } else {
            Imgproc.cvtColor(x1, x2, Imgproc.COLOR_BGR2GRAY);
            //Imgproc.cvtColor(extendedInputImage, gray, Imgproc.COLOR_RGB2GRAY);
        }

        Imgproc.Canny(x2, circleEdges, canny1, canny2);

        if(dilate>0) {
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(dilate, dilate));
            Imgproc.dilate(circleEdges, circleEdges, kernel);
        }

        Mat x3 = new Mat(newHeight, newWidth, circleEdges.type(), new Scalar(0, 0, 0));
        Mat roi2 = new Mat(x3, new Rect(offsetX, offsetY, minimap.cols(), minimap.rows()));
        circleEdges.copyTo(roi2);

        BufferedImage minimapBuff = matToBufferedImage(minimap);
        BufferedImage minimapExtendBuff = matToBufferedImage(extendedInputImage);
        BufferedImage minimapExtendGreyBuff = matToBufferedImage(gray);

        BufferedImage edgeMinimapBuff = matToBufferedImage(x1);
        BufferedImage edgeMinimapGrayBuff = matToBufferedImage(x2);
        BufferedImage edgeMinimapGrayCannyBuff = matToBufferedImage(circleEdges);
        BufferedImage edgeMinimapGrayCannyExtendBuff = matToBufferedImage(x3);


        List<DetectedCircle> circleList = new ArrayList<>();
        for (int i = 0; i < circles.cols(); i++) {
            double[] circleData = circles.get(0, i);
            double centerX = circleData[0];
            double centerY = circleData[1];
            double radius = circleData[2];

            Point center = new Point(Math.round(centerX), Math.round(centerY));

            double stepSize = 0.5;

            double startAngle = 0;
            double endAngle = 360;

            int maxConsecutiveWhite = 0;
            double currentWhite = 0;
            double currentBlack = 0;


            // Loop over angle from startAngle to endAngle
            for (double angle = startAngle; angle < endAngle; angle += stepSize) {
                int x = (int) (centerX + radius * Math.cos(Math.toRadians(angle)));
                int y = (int) (centerY + radius * Math.sin(Math.toRadians(angle)));

                // Check boundaries
                if (x >= maxRadius && x < minimap.cols() + maxRadius && y >= maxRadius && y < minimap.rows() + maxRadius) {
                    double pixelValue = x3.get(y, x)[0];

                    // Check if the pixel is an edge pixel
                    if (pixelValue == 255) {
                        currentWhite++;
                    } else {
                        currentBlack++;
                    }
                }
            }

            boolean perfect = false;
            double ratioWhiteToBlack = 0;
            if (currentWhite > 0) {
                if (currentBlack > 0) {
                    ratioWhiteToBlack = currentWhite / currentBlack;
                } else {
                    perfect = true;
                    ratioWhiteToBlack = currentWhite;
                }
            }

            DetectedCircle c = new DetectedCircle(new Point(center.x*2,center.y*2), Double.valueOf(radius).intValue()*2, ratioWhiteToBlack, maxRadius*2, round, closing);
            c.setPerfect(perfect);
            circleList.add(c);
        }

        circleList.sort((c1, c2) -> Double.compare(c2.getScore(), c1.getScore()));
        List<DetectedCircle> returnList = List.copyOf(circleList);
        if (threshold == 1000){
            returnList = circleList.stream()
                    .filter(DetectedCircle::isPerfect)
                    .filter(minR -> minR.getRadius() >= minRadius * 2)
                    .filter(maxR -> maxR.getRadius() <= maxRadius * 2)
                    .collect(Collectors.toList());
            if (returnList.isEmpty()){
                returnList = circleList.stream()
                        .filter(obj -> obj.getScore() >= 165)
                        .filter(minR -> minR.getRadius() >= minRadius * 2)
                        .filter(maxR -> maxR.getRadius() <= maxRadius * 2)
                        .collect(Collectors.toList());
            }
        }else {
            returnList = circleList.stream()
                    .filter(obj -> obj.getScore() >= threshold)
                    .filter(minR -> minR.getRadius() >= minRadius * 2)
                    .filter(maxR -> maxR.getRadius() <= maxRadius * 2)
                    .limit(1)
                    .collect(Collectors.toList());
        }
//        for (int i = 0; i < circleList.size(); i++) {
//            System.out.println("Center: " + circleList.get(i).getCenter().x + ", " + circleList.get(i).getCenter().y +
//                    " Radius: " + circleList.get(i).getRadius() + " score: " + circleList.get(i).getScore());
//        }

//        circleList = circleList.stream().filter(obj -> obj.getScore() >= scoreThreshold).collect(Collectors.toList());
        return returnList;
    }


    public List<DetectedCircle> getCirclesInFullMap(@Nonnull final Mat minimap, int round, boolean closing){

        int minRadius = 100;
        int maxRadius = 500;
//        int variableDistance = 15;
        List<DetectedCircle> foundCircles = new ArrayList<>();
        if (closing){
            if (round == 1){ // ring 1 visible + closing
//                minRadius = getRing1Radius() + variableDistance;
//                maxRadius = getRing1Radius() * 2;
//                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true));
                minRadius = getRing1Radius() - 20;
                maxRadius = getRing1Radius() + 20;
                foundCircles.addAll(getCirclesFromFullMap(minimap, minRadius, maxRadius, 1, true, 40,40,120,20,600,8));
                //possible ring 2 shown if beacon
//                minRadius = getRing2Radius() - 20;
//                maxRadius = getRing2Radius() + 20;
//                foundCircles.addAll(getCirclesFromFullMap(minimap, minRadius, maxRadius, 1, false, 40,40,120,20,600,8));
            }else if(round == 2){ // ring 2 visible + closing
//                minRadius = getRing2Radius() + variableDistance;
//                maxRadius = getRing1Radius();
//                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true));
                minRadius = getRing2Radius() - 20;
                maxRadius = getRing2Radius() + 20;
                foundCircles.addAll(getCirclesFromFullMap(minimap, minRadius, maxRadius, 2, true, 40,40,120,20,600,8));
            }else if(round == 3){ // ring 2 visible + closing
//                minRadius = getRing2Radius() + variableDistance;
//                maxRadius = getRing1Radius();
//                foundCircles.addAll(getCircles(minimap, minRadius, maxRadius, round, true));
                minRadius = getRing3Radius() - 20;
                maxRadius = getRing3Radius() + 20;
                foundCircles.addAll(getCirclesFromFullMap(minimap, minRadius, maxRadius, 3, true, 40,40,120,20,600,8));
            }
        }else{
            if (round == 1){ // ring 1 visible
                minRadius = getRing1Radius() - 20;
                maxRadius = getRing1Radius() + 20;
                foundCircles.addAll(getCirclesFromFullMap(minimap, minRadius, maxRadius, 1, false, 40,40,120,20,600,8));
                //possible ring 2 shown if beacon
//                minRadius = getRing2Radius() - 20;
//                maxRadius = getRing2Radius() + 20;
//                foundCircles.addAll(getCirclesFromFullMap(minimap, minRadius, maxRadius, 1, false, 40,40,120,20,600,8));
            }else if(round == 2){ // ring 1+2 visible
                minRadius = getRing1Radius() - 20;
                maxRadius = getRing1Radius() + 20;
                foundCircles.addAll(getCirclesFromFullMap(minimap, minRadius, maxRadius, 1, false, 40,40,120,20,600,8));
                minRadius = getRing2Radius() - 20;
                maxRadius = getRing2Radius() + 20;
                foundCircles.addAll(getCirclesFromFullMap(minimap, minRadius, maxRadius, 2, false, 40,40,120,20,600,8));
            }else if(round == 3){ // ring 1+2 visible
                minRadius = getRing2Radius() - 20;
                maxRadius = getRing2Radius() + 20;
                foundCircles.addAll(getCirclesFromFullMap(minimap, minRadius, maxRadius, 2, false, 40,40,120,20,600,8));
                minRadius = getRing3Radius() - 20;
                maxRadius = getRing3Radius() + 20;
                foundCircles.addAll(getCirclesFromFullMap(minimap, minRadius, maxRadius, 3, false, 40,40,120,20,600,8));
            }
        }
        return foundCircles;
    }
    private List<DetectedCircle> getCirclesFromFullMap(Mat minimap, int minRadius, int maxRadius, int round, boolean closing, int canny1, int canny2, int houghP1, int houghP2, double threshold, int dilate ){

        minRadius = minRadius/2;
        maxRadius = maxRadius/2;

//        System.out.println("Circles for round: " + round);

        // 2. grey extended image
        Mat gray = new Mat();
        Imgproc.cvtColor(minimap, gray, Imgproc.COLOR_BGR2GRAY);

        // 3. hough Trough on extended grey image
        Mat circles = new Mat();
        Imgproc.HoughCircles(gray, circles, Imgproc.HOUGH_GRADIENT, 1, 1024, houghP1, houghP2, minRadius, maxRadius);

        // 4. find edges, dont expand until after canny
        Mat circleEdges = new Mat();
        Mat x1 = minimap.clone();
        Mat x2 = new Mat();
        Imgproc.cvtColor(x1, x2, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(x2, circleEdges, canny1, canny2);

        if(dilate>0) {
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(dilate, dilate));
            Imgproc.dilate(circleEdges, circleEdges, kernel);
        }


        BufferedImage minimapBuff = matToBufferedImage(minimap);
        BufferedImage minimapExtendGreyBuff = matToBufferedImage(gray);

        BufferedImage edgeMinimapBuff = matToBufferedImage(x1);
        BufferedImage edgeMinimapGrayBuff = matToBufferedImage(x2);
        BufferedImage edgeMinimapGrayCannyBuff = matToBufferedImage(circleEdges);


        List<DetectedCircle> circleList = new ArrayList<>();
        for (int i = 0; i < circles.cols(); i++) {
            double[] circleData = circles.get(0, i);
            double centerX = circleData[0];
            double centerY = circleData[1];
            double radius = circleData[2];

            Point center = new Point(Math.round(centerX), Math.round(centerY));

            double stepSize = 0.5;

            double startAngle = 0;
            double endAngle = 360;

            int maxConsecutiveWhite = 0;
            double currentWhite = 0;
            double currentBlack = 0;


            // Loop over angle from startAngle to endAngle
            for (double angle = startAngle; angle < endAngle; angle += stepSize) {
                int x = (int) (centerX + radius * Math.cos(Math.toRadians(angle)));
                int y = (int) (centerY + radius * Math.sin(Math.toRadians(angle)));

                // Check boundaries
                if (x >= 0 && x < minimap.cols() && y >= 0 && y < minimap.rows()) {
                    double pixelValue = circleEdges.get(y, x)[0];

                    // Check if the pixel is an edge pixel
                    if (pixelValue == 255) {
                        currentWhite++;
                    } else {
                        currentBlack++;
                    }
                }
            }


            if (currentWhite > threshold) {
                DetectedCircle c = new DetectedCircle(new Point(center.x * 2, center.y * 2), Double.valueOf(radius).intValue() * 2, currentWhite, 1, round, closing);
                c.setPerfect(true);
                circleList.add(c);
            }
        }

//        for (int i = 0; i < circleList.size(); i++) {
//            Mat draw = minimap.clone();
//            Imgproc.circle(draw, new Point(circleList.get(i).getCenter().x/2,circleList.get(i).getCenter().y/2) , circleList.get(i).getRadius()/2, new Scalar(0, 255, 0,0), 2);
//            try {
//                CircleUtil cu = new CircleUtil();
//                ScreenProcessToImage.writeImageToFile(cu.matToBufferedImage(draw), String.valueOf(new Date().getTime()));
//            }catch(Exception ex){
//
//            }
//            System.out.println("Center: " + circleList.get(i).getCenter().x + ", " + circleList.get(i).getCenter().y +
//                    " Radius: " + circleList.get(i).getRadius() + " score: " + circleList.get(i).getScore());
//        }

//        circleList = circleList.stream().filter(obj -> obj.getScore() >= scoreThreshold).collect(Collectors.toList());

        try{
            circleEdges.release();
            circles.release();
            x1.release();
            x2.release();
            gray.release();
        } catch (Exception e) {
            //
        }
        return circleList;
    }
    

    private static Mat bufferedImageToMat(BufferedImage fullScreenshot) {
        int width = fullScreenshot.getWidth();
        int height = fullScreenshot.getHeight();
        int type = CvType.CV_8UC3; // Use CvType.CV_8UC3 for a 3-channel (RGB) image

        // Create a ByteBuffer to hold the image data
        ByteBuffer byteBuffer = ByteBuffer.allocate(width * height * 3);

        // Fill the ByteBuffer with the image data from BufferedImage
       /* for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = fullScreenshot.getRGB(x, y);
                byteBuffer.put((byte) ((rgb >> 16) & 0xFF)); // R
                byteBuffer.put((byte) ((rgb >> 8) & 0xFF));  // G
                byteBuffer.put((byte) (rgb & 0xFF));         // B
            }
        }*/
        // Fill the ByteBuffer with the image data from BufferedImage
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = fullScreenshot.getRGB(x, y);
                byteBuffer.put((byte) (rgb & 0xFF));         // B
                byteBuffer.put((byte) ((rgb >> 8) & 0xFF));  // G
                byteBuffer.put((byte) ((rgb >> 16) & 0xFF)); // R
            }
        }

        // Rewind the buffer to the beginning
        byteBuffer.rewind();

        // Create an OpenCV Mat from the ByteBuffer
        Mat matScreenshot = new Mat(height, width, type);
        matScreenshot.put(0, 0, byteBuffer.array());

        return matScreenshot;
    }

    public BufferedImage matToBufferedImage(Mat mat) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", mat, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        BufferedImage bufImage = null;

        try {
            InputStream in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bufImage;
    }

    public static List<DetectedCircle> adjustCircles(final List<DetectedCircle> inputCircles) {
        List<DetectedCircle> outputCircles = new ArrayList<>(inputCircles);
        outputCircles.sort(Comparator.comparing(DetectedCircle::getRadius).reversed());

        boolean hit = true;
        int total = 0;
        while (hit && total < 5) {
            boolean foundPerfect = false;
            hit = false;
            for (int i = 1; i < outputCircles.size(); i++) {
                DetectedCircle larger = outputCircles.get(i - 1);
                DetectedCircle smaller = outputCircles.get(i);

                if (larger.isPerfect()) {
                    foundPerfect = true;
                }
                if (!foundPerfect) {
                    // Move the larger circle toward the smaller one until they overlap
                    while (!covers(larger, smaller)) {
                        moveToward(larger, smaller, 1);
                        hit = true;
                    }
                } else {
                    while (!covers(larger, smaller)) {
                        moveToward(smaller, larger, 1); // Move the smaller circle inside the larger perfect circle
                        hit = true;
                    }
                }
            }
            total++;
        }
        return outputCircles;
    }

    // Utility method to move a circle towards another with specific rounding behavior
    public static void moveToward(DetectedCircle mover, DetectedCircle target, double speed) {
        double dx = target.getCenter().x - mover.getCenter().x;
        double dy = target.getCenter().y - mover.getCenter().y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 1) return;

        // Normalize the direction vector
        if (distance > 0) {
            dx /= distance;
            dy /= distance;
        }

        // Calculate movement with specified rounding behavior
        double moveX = dx * speed;
        double moveY = dy * speed;

        // Use ceil for positive values and floor for negative values
        if (moveX > 0) {
            moveX = (int) Math.ceil(dx * speed);  // Use ceil for positive movement
        } else if (moveX < 0) {
            moveX = (int) Math.floor(dx * speed);  // Use floor for negative movement
        }

        if (moveY > 0) {
            moveY = (int) Math.ceil(dy * speed);  // Use ceil for positive movement
        } else if (moveY < 0) {
            moveY = (int) Math.floor(dy * speed);  // Use floor for negative movement
        }

        // Apply the movement
        mover.getCenter().x += (int)moveX;
        mover.getCenter().y += (int)moveY;
    }



    public static boolean covers(DetectedCircle larger, DetectedCircle smaller) {
        double distance = Math.sqrt((larger.getCenter().x - smaller.getCenter().x) * (larger.getCenter().x - smaller.getCenter().x) +
                (larger.getCenter().y - smaller.getCenter().y) * (larger.getCenter().y - smaller.getCenter().y));
        return distance + smaller.getRadius() <= larger.getRadius();
    }

}
