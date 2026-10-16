/*
 * MapThread
 * v0.94
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex;

import au.mapbot.apex.classes.DetectedCircle;
import au.mapbot.apex.classes.ImageMatching;
import au.mapbot.apex.classes.MapEnum;
import au.mapbot.apex.util.*;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static au.mapbot.apex.MainProcess.checkImageResolutionIs169oor1610;

public class MapThread extends Thread{

    private boolean running;
    private BufferedImage gameImage;
    private SnapshotFromImage screenProcessToImage;
    private WebDriverUtil webDriverUtil;
    private boolean isLocalGame;
    private Robot robot;
    private CircleUtil circleUtil;

    private ScreenshotUtil screenshotUtil;
    private MainProcess.RoundStatus round;
    private BufferedImage currentFullColorMap;
    private List<DetectedCircle> allDetectedCircles;
    private List<DetectedCircle> zonesToUse;
    private ImageCompare imageCompare;
    private BufferedImage beaconBuffImage;
    private BufferedImage beaconMaskBuffImage;
    private BufferedImage rcBuffImage;
    private BufferedImage rcMaskBuffImage;
    private Set<Point> evoPoints;
    private Set<ImageMatching> beaconPoints;
    private Set<ImageMatching> rcPoints;

    private BufferedImage allowedRCBuff;
    private BufferedImage allowedBeaconBuff;
    private IconsOnMapUtil iconsOnMapUtil;

//    final private VideoFrameProcessor fFmpegCapture;

    public MapThread(WebDriverUtil webDriverUtil, SnapshotFromImage screenProcessToImage, boolean isLocalGame, CircleUtil circleUtil, final Robot robot, final ScreenshotUtil screenshotUtil, final ImageCompare imageCompare, BufferedImage fullMap, MapEnum map) throws AWTException, IOException {
        this.running = true;
        this.screenProcessToImage = screenProcessToImage;
        this.webDriverUtil = webDriverUtil;
        this.isLocalGame = isLocalGame;
        this.gameImage = null;
        this.circleUtil = circleUtil;
        this.robot = robot;
        this.screenshotUtil = screenshotUtil;
        this.round = new MainProcess.RoundStatus(0,false);
        this.allDetectedCircles = new ArrayList<>();
        this.zonesToUse = new ArrayList<>();
        this.imageCompare = imageCompare;
        this.beaconBuffImage = ImageUtil.getImageFromResource("/imageMatching/bb1.png");
        this.beaconMaskBuffImage = ImageUtil.getImageFromResource("/imageMatching/bb1Mask.png");
        this.rcBuffImage = ImageUtil.getImageFromResource("/imageMatching/cc1.png");
        this.rcMaskBuffImage = ImageUtil.getImageFromResource("/imageMatching/cc1Mask.png");
//        this.evoBuffImage = ImageUtil.getImageFromResource("/evo.png");
        this.currentFullColorMap = fullMap;
        this.evoPoints = new HashSet<>();
        this.beaconPoints = new HashSet<>();
        this.rcPoints = new HashSet<>();
        this.allowedBeaconBuff = ImageUtil.getImageFromResource(map.getMapAllowedBeaconResourceString());
        this.allowedRCBuff = ImageUtil.getImageFromResource(map.getMapAllowedRCResourceString());
        this.iconsOnMapUtil = new IconsOnMapUtil();
//        this.fFmpegCapture = fFmpegCapture;
    }

    public void setMap(BufferedImage currentMap) throws IOException {
        currentFullColorMap = currentMap;
    }

    public void updateRound(MainProcess.RoundStatus round){
        this.round = round;
    }

    public BufferedImage getGameImage() {
        return gameImage;
    }

    public List<DetectedCircle> getZonesToUse() {
        return zonesToUse;
    }


    public boolean addPoint(Point newPoint, Set<Point> oldPoints) {
        for (Point p : oldPoints) {
            if (Math.abs(p.x - newPoint.x) <= 50 && Math.abs(p.y - newPoint.y) <= 50) {
                return false; // There is a point within the 50-unit range
            }
        }
        return true; // Point added successfully
    }

    public int iii = 0;
    public void run() {
        System.out.println("Map Thread Started");
        this.setPriority(Thread.MIN_PRIORITY);
        while (running) {
            try {
                Date startTime = new Date();
                BufferedImage gameImageRead = getGameImage(isLocalGame);

                BufferedImage fullMapBuff = screenProcessToImage.getFullMapShown(gameImageRead);
                final BufferedImage borderedMinimapNormal = screenProcessToImage.getMinimapBorder(gameImageRead);
                final BufferedImage borderedMinimapSquished = screenProcessToImage.getMinimapBorder1610squish(gameImageRead);
                final BufferedImage borderedMinimapSquished43 = screenProcessToImage.getMinimapBorder43Squish(gameImageRead);
                if (!isMinimapShown(borderedMinimapNormal) && !isMinimapShown(borderedMinimapSquished) && (!isMinimapShown(borderedMinimapSquished43))) {
                    if (isMapShown(fullMapBuff, currentFullColorMap)) {
                        SnapshotFromImage.writeImageToFile(fullMapBuff, "fullMap");
                        System.out.println("Map Shown");
                        //get map icons
                        double scaleAmount = fullMapBuff.getWidth() / 1013d;
//                        double scaleLocationAmount = ((double) currentFullColorMap.getWidth()) / ((double) fullMapBuff.getWidth());

                        int beaconScaleWidth = Double.valueOf(beaconBuffImage.getWidth() * scaleAmount).intValue();
                        int beaconScaleHeight = Double.valueOf(beaconBuffImage.getHeight() * scaleAmount).intValue();
                        BufferedImage beaconBuffScaled = getScaledBufferedImage(beaconBuffImage, beaconScaleWidth, beaconScaleHeight);
                        BufferedImage beaconMaskBuffScaled = getScaledBufferedImage(beaconMaskBuffImage, beaconScaleWidth, beaconScaleHeight);

                        BufferedImage allowedBeaconLocationScaled = getScaledBufferedImage(allowedBeaconBuff, fullMapBuff.getWidth(), fullMapBuff.getHeight());

                        double thresholdMatch = 0.55d; // going to miss some but hopefully pick them up once zone closes
                        List<ImageMatching> beaconIM = iconsOnMapUtil.findInMapNew(ImageUtil.bufferedImage2Mat(fullMapBuff), ImageUtil.bufferedImage2Mat(beaconBuffScaled), iconsOnMapUtil.bufferedImageToGrayMat(beaconMaskBuffScaled), allowedBeaconLocationScaled, thresholdMatch);
                        for (ImageMatching matchLoc : beaconIM) {
                            Optional<ImageMatching> existingMatch = beaconPoints.stream()
                                    .filter(s -> s.getPoint() == matchLoc.getPoint())
                                    .findFirst();

                            if (existingMatch.isPresent()) {
                                ImageMatching im = existingMatch.get();
                                if (matchLoc.getScore() > im.getScore()) {
                                    beaconPoints.remove(im);
                                    beaconPoints.add(matchLoc);
                                }
                            } else {
                                beaconPoints.add(matchLoc);
                            }
                        }

                        int rcScaleWidth = Double.valueOf(rcBuffImage.getWidth() * scaleAmount).intValue();
                        int rcScaleHeight = Double.valueOf(rcBuffImage.getHeight() * scaleAmount).intValue();
                        BufferedImage rcBuffScaled = getScaledBufferedImage(rcBuffImage, rcScaleWidth, rcScaleHeight);
                        BufferedImage rcMaskBuffScaled = getScaledBufferedImage(rcMaskBuffImage, rcScaleWidth, rcScaleHeight);
                        BufferedImage allowedRCLocationScaled = getScaledBufferedImage(allowedRCBuff, fullMapBuff.getWidth(), fullMapBuff.getHeight());
                        List<ImageMatching> rcIM = iconsOnMapUtil.findInMapNew(ImageUtil.bufferedImage2Mat(fullMapBuff), ImageUtil.bufferedImage2Mat(rcBuffScaled), iconsOnMapUtil.bufferedImageToGrayMat(rcMaskBuffScaled), allowedRCLocationScaled, thresholdMatch);
                        for (ImageMatching matchLoc : rcIM) {
                            Optional<ImageMatching> existingMatch = rcPoints.stream()
                                    .filter(s -> s.getPoint() == matchLoc.getPoint())
                                    .findFirst();

                            if (existingMatch.isPresent()) {
                                ImageMatching im = existingMatch.get();
                                if (matchLoc.getScore() > im.getScore()) {
                                    rcPoints.remove(im);
                                    rcPoints.add(matchLoc);
                                }
                            } else {
                                rcPoints.add(matchLoc);
                            }
                        }

//                        int rcScaleWidth2 = Double.valueOf(rcBuffImage2.getWidth() * scaleAmount).intValue();
//                        int rcScaleHeight2 = Double.valueOf(rcBuffImage2.getHeight() * scaleAmount).intValue();
//                        BufferedImage rcBuffScaled2 = getScaledBufferedImage(rcBuffImage2,rcScaleWidth2,rcScaleHeight2);
//                        BufferedImage rcMaskBuffScaled2 = getScaledBufferedImage(rcMaskBuffImage2,rcScaleWidth2,rcScaleHeight2);
//                        List<ImageMatching> rcIM2 = iconsOnMapUtil.findInMapNew(ImageUtil.bufferedImage2Mat(fullMapBuff),ImageUtil.bufferedImage2Mat(rcBuffScaled2),iconsOnMapUtil.bufferedImageToGrayMat(rcMaskBuffScaled2),allowedRCLocationScaled,0.55d);
//                        for (ImageMatching matchLoc : rcIM2) {
//                            Optional<ImageMatching> existingMatch = rcPoints.stream()
//                                    .filter(s -> s.getPoint() == matchLoc.getPoint())
//                                    .findFirst();
//
//                            if (existingMatch.isPresent()) {
//                                ImageMatching im = existingMatch.get();
//                                if (matchLoc.getScore() > im.getScore()) {
//                                    rcPoints.remove(im);
//                                    rcPoints.add(matchLoc);
//                                }
//                            }else{
//                                rcPoints.add(matchLoc);
//                            }
//                        }

                        SnapshotFromImage.writeImageToFile(fullMapBuff, ("mapImage" + iii++));

                        //get zones
                        if (round != null && round.getRound() > 0) {
                            //get zones from detected on screen
                            java.util.List<DetectedCircle> zonesFromMap = getZones(fullMapBuff, round.getRound(), round.getRoundClosing());
                            allDetectedCircles.addAll(zonesFromMap);
                        }
                    } else {
                        //only set game image when map isn't shown
                        gameImage = gameImageRead;
                    }
                } else {
                    //only set game image when map isn't shown
                    gameImage = gameImageRead;
                }
                try {
//                    System.out.println("Sleep map thread 500");
                    if (new Date().getTime() - startTime.getTime() < 1000){
                        sleep(1000 - (new Date().getTime() - startTime.getTime()));
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }

        System.out.println("MapThread shut down");
    }

    public List<DetectedCircle> getAllDetectedFullMapZones(){
        return allDetectedCircles;
    }

    public boolean isAllowedLocation(final Point p, BufferedImage mapBW){
        int xValue = Double.valueOf(p.x).intValue();
        int yValue = Double.valueOf(p.y).intValue();

        if (xValue > mapBW.getWidth() || yValue > mapBW.getHeight() || xValue < 0 || yValue < 0){
            return false;
        }

        Color wePixelColor = new Color(mapBW.getRGB(xValue, yValue));
        if (wePixelColor.equals(Color.BLACK)) {
            return true;
        }
        return false;
    }

    private static int rgbToGray(int rgb, MainProcess.MODE mode) {
        // split rgb integer into R, G and B components
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        int gray;
        // Select mode
        switch (mode) {
            case LIGHTNESS:
                gray = Math.round((Math.max(r, Math.max(g, b)) + Math.min(r, Math.min(g, b))) / 2);
                break;
            case LUMINOSITY:
                gray = Math.round(0.21f * r + 0.72f * g + 0.07f * b);
                break;
            case AVERAGE:
            default:
                gray = Math.round((r + g + b) / 3);
                break;
        }
        return gray;
    }
    public Boolean isMinimapShown(final BufferedImage potentialMinimapImage){
        int threshold = 140;
        //top left
        int found = 4;
        if ((rgbToGray(potentialMinimapImage.getRGB(0, 0), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(1, 0), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(0, 1), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(1, 1), MainProcess.MODE.LIGHTNESS) < threshold)){
            found -= 1;
        }
        //top right
        if ((rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 1, 0), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 2, 0), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 1, 1), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 2, 1), MainProcess.MODE.LIGHTNESS) < threshold)){
            found -= 1;
        }
        //bottom left
        if ((rgbToGray(potentialMinimapImage.getRGB(0, potentialMinimapImage.getHeight() - 1), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(1, potentialMinimapImage.getHeight() - 1), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(0, potentialMinimapImage.getHeight() - 2), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(1, potentialMinimapImage.getHeight() - 2), MainProcess.MODE.LIGHTNESS) < threshold)){
            found -= 1;
        }
        //bottom right
        if ((rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 1, potentialMinimapImage.getHeight() - 1), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 2, potentialMinimapImage.getHeight() - 1), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 1, potentialMinimapImage.getHeight() - 2), MainProcess.MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 2, potentialMinimapImage.getHeight() - 2), MainProcess.MODE.LIGHTNESS) < threshold)){
            found -= 1;
        }
        if (found >= 3){
            return true;
        }
        return false;
    }

    BufferedImage getScaledBufferedImage(BufferedImage originalImage, int width, int height) {
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
    public Set<Point> getEvoPoints(){
        return this.evoPoints;
    }

    public Set<Point> getBeaconPoints(){
        Set<Point> p = new HashSet<>();
        for(ImageMatching bp : beaconPoints) {
            boolean thisIsBetter = true;
            for (ImageMatching rcp : rcPoints) {
                if (bp.getPoint().equals(rcp.getPoint()) && rcp.getScore() > bp.getScore()) {
                    thisIsBetter = false;
                }
            }
            if (thisIsBetter) {
                p.add(bp.getPoint());
            }
        }
        return p;
    }

    public Set<Point> getRcPoints(){
        Set<Point> p = new HashSet<>();
        for(ImageMatching rcp : rcPoints) {
            boolean thisIsBetter = true;
            for (ImageMatching bp : beaconPoints) {
                if (bp.getPoint().equals(rcp.getPoint()) && bp.getScore() > rcp.getScore()) {
                    thisIsBetter = false;
                }
            }
            if (thisIsBetter) {
                p.add(rcp.getPoint());
            }
        }
        return p;
    }


    public java.util.List<DetectedCircle> getZones(final BufferedImage mapImage, final int round, final boolean isClosing) throws IOException {
        Mat fullMapMat = ImageUtil.bufferedImage2Mat(mapImage);
        Mat fullMapMatResized = ImageUtil.resizeMat(fullMapMat, 1024, 1024);
        List<DetectedCircle> circles = circleUtil.getCirclesInFullMap(fullMapMatResized, round, isClosing);
        return circles;
    }

    public BufferedImage getGameImage(final boolean isLocalGame) throws IOException, InterruptedException, TimeoutException {
        BufferedImage gameImage;
//        if (fFmpegCapture != null){
//            //gameImage = StreamlinkFFMPEGScreenshotUtil.getScreenImage(hlsUrl);
//            gameImage = fFmpegCapture.getFrame();
//        }else {
        if(webDriverUtil != null && !webDriverUtil.isChromeStillRunning()){
            running = false;
            System.out.println("Chrome no longer running, likely closed by user, terminating session...");
        }
            if (!isLocalGame) {
                gameImage = webDriverUtil.screenShotVideo();
            } else {
                ScreenshotUtil.WindowInfo window = screenshotUtil.getSpecificWindow("Apex Legends");
                final int width = window.rect.right - window.rect.left;
                final int height = window.rect.bottom - window.rect.top;

                //if it is prefectly 16:9 or 16:10 it won't be awindowed game client
                if (checkImageResolutionIs169oor1610(width, height)) {
                    return robot.createScreenCapture(new Rectangle(window.rect.left, window.rect.top, width, height));
                } else {
                    //offset for windowed apex game bordered client local games
                    return robot.createScreenCapture(new Rectangle(window.rect.left + 10, window.rect.top + 33, width - 20, height - 43));
                }
            }
//        }
        return gameImage;
    }

    public Boolean isMapShown(final BufferedImage gameImage, final BufferedImage map) throws IOException {

        if (map != null) {
            Mat fullMapMat = ImageUtil.bufferedImage2Mat(gameImage);
            Mat fullMapMatResized = ImageUtil.resizeMat(fullMapMat, 2048, 2048);
            BufferedImage fullMapMatResizedBuff = circleUtil.matToBufferedImage(fullMapMatResized).getSubimage(20, 20, fullMapMatResized.width() - 40, fullMapMatResized.height() - 40);

            Mat actualMap = ImageUtil.bufferedImage2Mat(map);
            ImageMatching matchLoc = imageCompare.findInMapBasic(ImageUtil.bufferedImage2Mat(fullMapMatResizedBuff), actualMap);
            if (matchLoc.getScore() > 0.4) {
                return true;
            }
        }
        return false;
    }

    public List<DetectedCircle> getActiveZones(int round){
        java.util.List<DetectedCircle> returnCircles = new ArrayList<>();

        if (round >= 1) {
            DetectedCircle zone1Exact = circleUtil.getZoneExact(allDetectedCircles, 1, circleUtil.getRing1Radius(), round);
            if (zone1Exact != null) {
                returnCircles.add(zone1Exact);
            }
        }

        if (round >= 2) {
            DetectedCircle zone2Exact = circleUtil.getZoneExact(allDetectedCircles, 2, circleUtil.getRing2Radius(), round);
            if (zone2Exact != null) {
                returnCircles.add(zone2Exact);
            }
        }

        if (round >= 3) {
            DetectedCircle zone3Exact = circleUtil.getZoneExact(allDetectedCircles, 3, circleUtil.getRing3Radius(), round);
            if (zone3Exact != null) {
                returnCircles.add(zone3Exact);
            }
        }
        if (round >= 4) {
            DetectedCircle zone4Exact = circleUtil.getZoneExact(allDetectedCircles, 4, circleUtil.getRing4Radius(), round);
            if (zone4Exact != null) {
                returnCircles.add(zone4Exact);
            }
        }
        if (round >= 5) {
            DetectedCircle zone5Exact = circleUtil.getZoneExact(allDetectedCircles, 5, circleUtil.getRing5Radius(), round);
            if (zone5Exact != null) {
                returnCircles.add(zone5Exact);
            }
        }

        return returnCircles;
    }

    public void end() {
        this.running = false;
        System.out.println("MapThread shut down request");

    }

    public boolean isRunning() {
        return this.running;
    }
}
