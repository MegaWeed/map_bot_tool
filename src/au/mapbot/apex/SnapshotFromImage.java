/*
 * SnapshotFromImage
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex;

import au.mapbot.apex.classes.BrowserImage;
import au.mapbot.apex.util.ImageUtil;
import au.mapbot.apex.util.ScreenshotUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;

public class SnapshotFromImage {
    private ScreenshotUtil screenshotUtil;

    public SnapshotFromImage() throws AWTException {
        this.screenshotUtil = new ScreenshotUtil();
    }

    public SnapshotFromImage(final ScreenshotUtil screenshotUtil) {
        this.screenshotUtil = screenshotUtil;
    }

    public BrowserImage getTargetWindow(final String windowName){
        if (windowName.isEmpty()){
            return screenshotUtil.captureWindowToBufferedImage("Apex Legends");
        }
        return screenshotUtil.captureWindowToBufferedImage(windowName);
    }

    public MainProcess.AspectRatios ratio = MainProcess.AspectRatios.SIXTEEN_BY_NINE;
    public MainProcess.AspectRatios streamRatio = MainProcess.AspectRatios.SIXTEEN_BY_NINE;

    /**
     * calculate ratio screen location for PING | SUGGEST | FOLLOW
     * while in drop-ship at start of game
     * l  - 1090/515  = 2.116
     * t  - 613/512   = 1.197
     * w  - 600-515   = 1090/75 = 14.533
     * h -  530-512   = 613/18  = 34.055
     */
    public BufferedImage getDropTextFromWindowStream(final BufferedImage window){
        Dimension originalImageDimensions = new Dimension(1090,613);
        Dimension originalSubImageDimensions = new Dimension(75,18);
        Point subImageLocation = new Point(515,512);

        final int left = Double.valueOf(Math.floor(window.getWidth() / (originalImageDimensions.getWidth() / subImageLocation.getX()))).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / (originalImageDimensions.getHeight() / subImageLocation.getY()))).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / (originalImageDimensions.getWidth() / originalSubImageDimensions.getWidth()))).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / (originalImageDimensions.getHeight() / originalSubImageDimensions.getHeight()))).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }


    /**
     * calculate ratio screen location for PING | SUGGEST | FOLLOW
     * while in drop-ship at start of game
     * l  - 1090/515  = 2.116
     * t  - 613/512   = 1.197  | 1078/900 = 1.19777777778
     * w  - 600-515   = 1090/75 = 14.533
     * h -  530-512   = 613/18  = 34.055
     */
    public BufferedImage getDropTextFromWindowStreamJP(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 2.116)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 1.19777777778)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 14.533)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 30)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * calculate ratio screen location for INVENTORY | SQUAD | LEGEND
     * l  - 1528/510  = 2.996
     * t  - 859/20    = 42.95
     * t2  - 1528/20    = 76.4
     * w  - 1005-510  = 495 = 1528/495 = 3.0868
     * h -  50-20     = 30  = 859/30   = 28.6333
     * h2 -  50-20     = 30  = 1528/30   = 50.9333333333
     */

    public BufferedImage getInventoryWindowStream(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 2.996)).intValue();
        final int top = Double.valueOf(Math.floor(window.getWidth() / 76.4)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 3.0868)).intValue();
        final int height = Double.valueOf(Math.floor(window.getWidth() / 50.9333333333)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    private BufferedImage rescaleByImageRatio(BufferedImage window, int left, int top, int width, int height){

        if (ratio == MainProcess.AspectRatios.SIXTEEN_BY_NINE && streamRatio == MainProcess.AspectRatios.SIXTEEN_BY_NINE) {
            return window.getSubimage(left, top, width, height);
        }else if(ratio == MainProcess.AspectRatios.SIXTEEN_BY_NINE && streamRatio == MainProcess.AspectRatios.SIXTEEN_BY_TEN){
            double heightDiff =  0.105; //11.11 percent difference
            if (top < window.getHeight()/2) {
                return window.getSubimage(left, top - Double.valueOf(top * heightDiff).intValue(), width, height - Double.valueOf(height * heightDiff).intValue());
            }else{
                return window.getSubimage(left, top + Double.valueOf((window.getHeight() - top) * heightDiff).intValue(), width, height - Double.valueOf(height * heightDiff).intValue());
            }
        }else if(ratio == MainProcess.AspectRatios.SIXTEEN_BY_NINE && streamRatio == MainProcess.AspectRatios.FOUR_BY_THREE){
            double heightDiff =  0.25; //25 percent difference
            if (top < window.getHeight()/2) {
                return window.getSubimage(left, top - Double.valueOf(top * heightDiff).intValue(), width, height - Double.valueOf(height * heightDiff).intValue());
            }else{
                return window.getSubimage(left, top + Double.valueOf((window.getHeight() - top) * heightDiff).intValue(), width, height - Double.valueOf(height * heightDiff).intValue());
            }
        }else{
            int newTop = Double.valueOf(top - (0.0909*top)).intValue();
            int newHeight = Double.valueOf(height - (0.0909*height)).intValue();
            int diff = getHeightFromWidth16to10AspectRatio(window.getWidth())-getHeightFromWidth16to9AspectRatio(window.getWidth());
            if (top > window.getHeight() / 2) {
                return window.getSubimage(left, newTop + diff, width, newHeight);
            }else{
                return window.getSubimage(left, newTop, width, newHeight);
            }
        }
    }

    private static boolean isApproximatelyEqual(double a, double b) {
        double epsilon = 0.01; // Adjust as needed for your tolerance
        return Math.abs(a - b) < epsilon;
    }

    /**
     * calculate ratio screen location for Looting death Box or Black market
     * l  - 1528/93   = 16.430
     * t  - 859/68    = 12.6323
     * w  - 215-93    = 1528/122 = 12.524
     * h -  110-68    = 859/42   = 20.4523
     */
    public BufferedImage getLootingWindowStream(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 16.430)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 12.6323)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 12.524)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 20.4523)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * 1920x1080
     * top left x453 y1
     * 1013x1013 map
     * so bot right should be 1466x y1014
     *  l - 1920/453 = 4.23841059603
     *  t - 0
     *  w/h - 1466-453 = 1920/1013 =1.89536031589
     *
     * @param window
     * @return
     */
    public BufferedImage getFullMapShown(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 4.23841059603)).intValue();
        int top = 0;
        int width = Double.valueOf(Math.floor(window.getWidth() / 1.89536031589)).intValue();
        int height = width;
        if (ratio == MainProcess.AspectRatios.SIXTEEN_BY_NINE && streamRatio == MainProcess.AspectRatios.SIXTEEN_BY_TEN){
            top = Double.valueOf((window.getHeight()*0.11)/2).intValue();
            BufferedImage image = rescaleByImageRatio(window, left, top, width, height);
            return ImageUtil.resizeBufferedImage(image, image.getWidth(), image.getWidth());
//            return stretchVertically(image,0.11);
        }else if (ratio == MainProcess.AspectRatios.SIXTEEN_BY_NINE  && streamRatio == MainProcess.AspectRatios.FOUR_BY_THREE){
            top = Double.valueOf((window.getHeight()*0.32)/2).intValue();
            BufferedImage image = rescaleByImageRatio(window, left, top, width, height);
            return ImageUtil.resizeBufferedImage(image, image.getWidth(), image.getWidth());
//            return stretchVertically(image,0.25);
        }else if (ratio == MainProcess.AspectRatios.SIXTEEN_BY_TEN){
            top = Double.valueOf((window.getHeight()*0.11)/2).intValue();
            return window.getSubimage(left, top, width, width);
        }

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * calculate ratio screen location for Ult % at bottom of screen
     * the ult percentage only appears while a character is on teh ground
     * this is the only text that changes between flying and landed
     * l  - 1528/742  = 2.0592
     * t  - 859/822   = 1.045
     * w  - 782-742   = 40 = 1528/40 = 38.2  | 1528/45 = 33.9555
     * h  - 842-822   = 20 = 859/20 = 42.95  | 859/25  = 34.36
     */
    public BufferedImage getUltPercentageSpotStreamHalfscreen(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 2.0592)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 1.045)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 33.9555)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 34.36 )).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }


    /**
     * calculate ratio screen location for SPECTATE | DEATH RECAP | SUMMARY
     * - SUMMARY is only shown when whole team is dead
     * l  - 1528/510  = 2.996                       | 1528/490 = 3.11836734694
     * t  - 859/20    = 42.95
     * w  - 1005-510  = 495 = 1528/495 = 3.0868     | 1528/535 = 2.85607476636
     * h -  50-20     = 30  = 859/30   = 28.6333
     */
    public BufferedImage getTopMainMenuText(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 4)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 42.95)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 2)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 28.6333)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * calculate ratio screen location for SPECTATE | DEATH RECAP | SUMMARY
     * - SUMMARY is only shown when whole team is dead
     * l  - 1528/510  = 2.996                       | 1528/490 = 3.11836734694
     * t  - 859/20    = 42.95
     * w  - 1005-510  = 495 = 1528/495 = 3.0868     | 1528/535 = 2.85607476636
     * h -  50-20     = 30  = 859/30   = 28.6333
     */
    public BufferedImage getDeadStreamHalfScreen(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 3.11836734694)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 42.95)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 2.85607476636)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 28.6333)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * calculate ratio screen location for "CONNECTING TO LOBBY"
     *
     * l  - 1090/385  = 2.8311
     * t  - 613/235   = 2.6085
     * w  - 710-385   = 325 = 1090/325 = 3.353
     * h  - 265-235   = 30  = 613/30   = 20.4333
     */
    public BufferedImage getConnectingToLobbyStreamHalfScreen(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 2.8311)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 2.6085)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 3.353)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 20.4333)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * calculate ratio screen location for map and if is ranked text on champ select
     * l  - 0
     * t  - 0
     * w  - 345 = 1528/345 = 4.428
     * h -  56  = 859/56   = 15.339   | 859/64 =
     */
    public BufferedImage getMapFromChampSelect(final BufferedImage window){
        final int width = Double.valueOf(Math.floor(window.getWidth() / 4.428)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 13.421875)).intValue();

        return rescaleByImageRatio(window, 0, 0, width, height);
    }

    /**
     * calculate ratio screen location for "SQUADS LEFT" in ranked game
     * the location for squads left moved based on ranked or unranked
     * because of the rank logo in corner
     * l  - 1528/1250 = 1.222 = 1528/1245 = 1.2273
     * t  - 859/42    = 20.45 || (859/44) = 19.5227
     * w  - 1372-1250 = 122 = 1528/122 = 12.52
     * h -  60-42     = 22  = 859/22   = 39.0454
     */
    public BufferedImage getSquadsLeftRanked(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.2273)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 19.5227)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 11.7538)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 39.045)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * same as ranked but moved more to right
     * l  - 1528/1305 = 1.1708
     * t  - 859/42    = 20.45
     * w  - 1372-1250 = 122 = 1528/122 = 12.52 = 1528/130 = 11.7538
     * h -  60-42     = 22  = 859/22   = 39.0454
     */
    public BufferedImage getSquadsLeftUnRanked(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.1708)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 20.45)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 11.7538)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 39.045)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }


    /**
     * same as ranked but moved more to right
     * 1920x1078
     * l  - 1920/1605 = 1.19626168224
     * t  - 1078/51   = 21.137254902
     * w  - 1800-1678 = 122 = 1920/122  = 15.737704918
     * h -  77-51     = 26  = 1078/26   = 41.4615384615
     */
    public BufferedImage getSquadsLeftRankedJP(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.19626168224)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 21.137254902)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 15.737704918)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 41.4615384615)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * same as ranked but moved more to right
     * 1920x1078
     * l  - 1920/1678 = 1.1442193087
     * t  - 1078/51   = 21.137254902
     * w  - 1800-1678 = 122 = 1920/122  = 15.737704918
     * h -  77-51     = 26  = 1078/26   = 41.4615384615
     */
    public BufferedImage getSquadsLeftUnRankedJP(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.1442193087)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 21.137254902)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 15.737704918)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 41.4615384615)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * calculate ratio screen location for "SQUADS LEFT" in ranked game
     * the location for squads left moved based on ranked or unranked
     * because of the rank logo in corner
     * l  - 1528/1250 = 1.222
     * t  - 859/42    = 9.1525
     * w  - 1372-1250 = 122 = 1528/122 = 12.52
     * h -  60-42     = 22  = 859/22   = 39.0454
     */
    public BufferedImage getSquadsLeftRankedDead(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.2273)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 9.1525)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 11.7538)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 39.045)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }
    public BufferedImage getSquadsLeftRankedDeadJP(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.19626168224)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 9.1525)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 15.737704918)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 41.4615384615)).intValue();
        return rescaleByImageRatio(window, left, top, width, height);
    }


    /**
     * calculate ratio screen location for "SQUADS LEFT" in ranked game
     * the location for squads left moved based on ranked or unranked
     * because of the rank logo in corner
     * l  - 1528/1250 = 1.222
     * t  - 859/42    = 9.1525
     * w  - 1372-1250 = 122 = 1528/122 = 12.52
     * h -  60-42     = 22  = 859/22   = 39.0454
     */
    public BufferedImage getSquadsLeftUnRankedDead(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.1708)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 9.1525)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 11.7538)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 39.045)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    public BufferedImage getSquadsLeftUnRankedDeadJP(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.1442193087)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 9.1525)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 15.737704918)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 41.4615384615)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }


    /**
     * calculate ratio screen location for UNranked kill text
     * l  - 1528/1306 = 1.1699
     * t  - 859/76    = 11.30
     * w  - 1210-1180 = 30 = 1528/30 = 50.93
     * h -  96-76     = 20 = 859/20  = 42.95
     */
    public BufferedImage getKillUnranked(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.1699)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 11.30)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 50.93)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 42.95)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * -
     * calculate ratio screen location for ranked kill text
     * l  - 1528/1180 = 1.2949
     * t  - 859/76    = 11.30
     * w  - 1210-1180 = 30 = 1528/30 = 50.93
     * h -  96-76     = 20 = 859/20  = 42.95
     *
     *      * l  - 1476/1146 = 1.2879
     *      * t  - 830/74    = 11.2162
     *      * w  - 1172-1159 = 13 = 1476/13 = 113.5384
     *      * h -  91-74     = 16 = 830/16 = 51.875
     *
     *
     *      offset for damage per digit
     *      1476/7 = 210.8571
     *
     *
     *      100 damage - new left 1156 + 17 = 1476/1173 = 1.2583
     *
     *
     */
    public BufferedImage getKillRanked(final BufferedImage window, final int damageOffset){
        int newOffset = window.getWidth() / (1476/damageOffset);
        int left = Double.valueOf(Math.floor(window.getWidth() / 1.2583)).intValue()-newOffset;
        final int top = Double.valueOf(Math.floor(window.getHeight() / 11.2162)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 113.5384)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 51.875)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     *
     * l  - 1476/1313 = 1.1241
     * t  - 830/75    = 11.0666
     * w  - 1350-1313 = 1313/37 = 35.4864
     * h -  92-75     = 830/17 = 48.8235
     */
    public BufferedImage getDamage(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.1241)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 11.0666)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 35.4864)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 48.8235)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     *
     * l  - 1416/1202 = 1.1780 (+ 18 | 1416/1220 = 1.1606
     * t  - 796/71    = 11.2112
     * w  - 1208-1202 = 1416/6 = 236
     * h -  88-71     = 796/17 = 46.8235
     */
    public BufferedImage getTeamAssist(final BufferedImage window, int damageOffset){
        int newOffset = window.getWidth() / (1476/damageOffset);
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.1606)).intValue()-newOffset;;
        final int top = Double.valueOf(Math.floor(window.getHeight() / 11.2112)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 236)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 46.8235)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     *
     * l  - 1416/1160 = 1.1780 (+ 22) | 1416/1182 = 1.1979
     * t  - 796/71    = 11.2112
     * w  - 1208-1202 = 1416/6 = 236
     * h -  88-71     = 796/17 = 46.8235
     */
    public BufferedImage getAssist(final BufferedImage window, int offSet){
        int newOffset = window.getWidth() / (1416/offSet);
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.1979)).intValue()-newOffset;;
        final int top = Double.valueOf(Math.floor(window.getHeight() / 11.2112)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 236)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 46.8235)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     *
     * l  - 1416/1115 = (+ 26) | 1416/1139 = 1.2431 | 1416/1137 = 1.2453
     * t  - 796/71    = 11.2112
     * w  - 1416/11   = 128.7272 | 1416/15 = 94.4
     * h -  88-71     = 796/17 = 46.8235
     */
    public BufferedImage getNewKill(final BufferedImage window, int offSet){
        int newOffset = window.getWidth() / (1416/offSet);
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.2453)).intValue()-newOffset;;
        final int top = Double.valueOf(Math.floor(window.getHeight() / 11.2112)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 94.4)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 46.8235)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     *
     * l  - 1416/1061 = 1.3345
     * t  - 796/71    = 11.2112
     * w  - 1294-1061 = 1416/233 = 6.0772
     * h -  88-71     = 796/17 = 46.8235
     */
    public BufferedImage getAllKillStats(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.3345)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 11.2112)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 6.0772)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 46.8235)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     *
     * l  - 1310/986  = 1.3286
     * t  - 736/65    = 11.3230
     * w  - 1055-986  = 69 = 1310/69 = 18.9855
     * h -  82-65     = 17 = 736/17  = 43.2941 || 736/18  = 40.8888
     */
    public BufferedImage getKillSearchAreaRanked(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.3286)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 11.3230)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 18.9855)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 40.8888)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * same getKillSearchAreaRanked but more to the right
     * l  - 1310/1090  = 1.2018
     * t  - 736/65    = 11.3230
     * w  - 1055-986  = 69 = 1310/69 = 18.9855
     * h -  82-65     = 17 = 736/17  = 43.2941
     */
    public BufferedImage getKillSearchAreaUnranked(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.2018)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 11.3230)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 18.9855)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 40.8888)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * same getKillSearchAreaRanked but more to the right
     * l  - 1310/1090  = 1.2018
     * t  - 736/65    = 11.3230
     * w  - 1055-986  = 69 = 1310/69 = 18.9855
     * h -  82-65     = 17 = 736/17  = 43.2941
     */
    public BufferedImage getKillSearchAreaLarger(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 1.5018)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 11.3230)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 4.1)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 40.8888)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }


    /**
     *
     * l  - 1361/533  = 2.5534
     * t  - 765/252   = 3.0357
     * w  - 741-533   = 208    = 1361/208 = 6.5432
     * h -  286-252   = 34     = 765/34  = 22.5
     */
    public BufferedImage getChampion(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 2.5534)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 3.0357)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 6.5432)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 22.5)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     *
     * window size - 1920x1080
     * top left - 822x953
     *  bot right - 1092x1001
     * l  - 1920/822  = 2.33576642336
     * t  - 1080/953   = 1.1332633788
     * w  - 1092-822   =  270 = 1920/270 = 7.11111111111
     * h -  1001-953   = 48     = 1080/48  = 22.5
     */
    public BufferedImage getChampion2(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 2.33576642336)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 1.1332633788)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 7.11111111111)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 22.5)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * 16:9 = 1:0.5625 = 1.7777777777778:1
     */
    private int getHeightFromWidth16to9AspectRatio(int width){
        return Double.valueOf(width*0.5625).intValue();
    }

    private static int getHeightFromWidth16to10AspectRatio(int width) {
        return Double.valueOf(width * 0.625).intValue();
    }
    public static int getWidthFromHeight16to9AspectRatio(int height) {
        return Double.valueOf(height * 1.7778).intValue();  // 16/9 = 1.7778
    }

    public static boolean isCloserTo16_10(int width, int height) {
        // Calculate the aspect ratio
        double aspectRatio = (double) width / height;

        // Define the aspect ratios for 16:9 and 16:10
        double aspectRatio16_9 = 16.0 / 9.0;
        double aspectRatio16_10 = 16.0 / 10.0;

        // Calculate the differences from both 16:9 and 16:10
        double diff16_9 = Math.abs(aspectRatio - aspectRatio16_9);
        double diff16_10 = Math.abs(aspectRatio - aspectRatio16_10);

        // Check which aspect ratio it is closer to
        return diff16_10 < diff16_9;
    }

    /**
     *
     */
    public BufferedImage getGameWindow(final BufferedImage window, int leftOffset, int rightOffset, int topOffset){
        final int gameWindowWidth = window.getWidth() - leftOffset - rightOffset;

        int gameWindowHeight;
        if (isCloserTo16_10(gameWindowWidth, window.getHeight()- topOffset)){
//            GameState.setIs1610Ratio(true);
            gameWindowHeight = getHeightFromWidth16to10AspectRatio(gameWindowWidth);
        }else {
            gameWindowHeight = getHeightFromWidth16to9AspectRatio(gameWindowWidth);
        }
        if (gameWindowWidth > window.getWidth()
                || gameWindowHeight > window.getHeight()
                || gameWindowWidth < 0
                || gameWindowHeight < 0){
            return null;
        }
        return window.getSubimage(leftOffset, topOffset, gameWindowWidth, gameWindowHeight);
    }

    public static boolean checkImageResolutionIs169or1610(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate the aspect ratio (width:height)
        double aspectRatio = (double) width / height;

        // Check if the aspect ratio is approximately 16:9 or 16:10
        boolean is16_9 = isApproxEqual(aspectRatio, 16.0 / 9);
        boolean is16_10 = isApproxEqual(aspectRatio, 16.0 / 10);

        return is16_9 || is16_10;
    }

    private static boolean isApproxEqual(double a, double b) {
        double epsilon = 0.01; // Tolerance for approximation
        return Math.abs(a - b) < epsilon;
    }
    public BufferedImage getGameWindowWithTop(final BrowserImage window, final String inputScreenText, int top) {
        int leftOffsetStreamSelector;
        int rightOffsetTwitchChat;
        int topOffsetMenuBar;
        if (inputScreenText.isEmpty()) {
            //compare with windows full screen resolutions
            if (checkImageResolutionIs169or1610(window.getScreenImage())
            ){
                //fullscreen client
                leftOffsetStreamSelector = 0;
                rightOffsetTwitchChat = 0;
                topOffsetMenuBar = 0;
            }else {
                //offset for windowed apex game bordered client
                leftOffsetStreamSelector = 10;
                rightOffsetTwitchChat = 10;
                topOffsetMenuBar = 33;
            }
        } else {

            if (window.getBrowser().equals(BrowserImage.BrowserEnum.FIREFOX)) {
                topOffsetMenuBar = top;
                leftOffsetStreamSelector = 51 + 5;
                rightOffsetTwitchChat = 341 + 5;
            }else{
                //chrome slightly different than firefox
                topOffsetMenuBar = top;
                leftOffsetStreamSelector = 51 + 5 + 3;
                rightOffsetTwitchChat = 341 + 5 + 1;
            }
        }
        return getGameWindow(window.getScreenImage(),
                leftOffsetStreamSelector,
                rightOffsetTwitchChat,
                topOffsetMenuBar);
    }
    public BufferedImage getGameWindow(final BrowserImage window, final String inputScreenText) {
        return getGameWindowWithTop(window, inputScreenText, 0);
    }

    double colorDist(Color e1, Color e2){
        long rmean = ( (long)e1.getRed() + (long)e2.getRed() ) / 2;
        long r = (long)e1.getRed() - (long)e2.getRed();
        long g = (long)e1.getGreen() - (long)e2.getGreen();
        long b = (long)e1.getBlue() - (long)e2.getBlue();
        return Math.sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
    }
    /**
     * calculate ratio screen location for minimap
     * l  - 1529/44 = 34.75     - 1529(game window width) | 44(minimap distance from left)
     * t  - 860/44  = 19.54     - 860(game window height) | 44(minimap distance from top)
     * wh - 860/182 = 4.72      - w/h same because is square | 182(width of minimap)
     * using wifht not height = 1529/182 = 8.4010989011
     */
    public BufferedImage getMinimapFromWindowGameClientWindowed(final BufferedImage window){
        final int mapLeft = Double.valueOf(Math.floor(window.getWidth() / 34.75)).intValue();
        final int mapTop = Double.valueOf(Math.floor(window.getHeight() / 19.54)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 8.4010989011)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 4.72)).intValue();


        //minimap is squished in 16:10 so need to unsquish it
        BufferedImage minimapImage = rescaleByImageRatio(window, mapLeft, mapTop, width, height);

        return minimapImage;
    }


    public static BufferedImage stretchVertically(BufferedImage originalImage, double verticalScale) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        int newHeight = (int) (originalHeight * (1 + verticalScale));

        BufferedImage stretchedImage = new BufferedImage(originalWidth, newHeight, originalImage.getType());
        Graphics2D g = stretchedImage.createGraphics();

        // Draw the original image onto the stretched image with vertical scaling
        g.drawImage(originalImage, 0, 0, originalWidth, newHeight, null);

        g.dispose();

        return stretchedImage;
    }

    /**
     * calculate ratio screen location for minimap
     * t  - 1080/123  = 9.39130434783
     */
    public BufferedImage getMinimapFromWindowGameClientWindowedDead(final BufferedImage window){
        final int mapLeft = Double.valueOf(Math.floor(window.getWidth() / 34.75)).intValue();
        final int mapTop = Double.valueOf(Math.floor(window.getHeight() / 8.78048780488)).intValue();
//        final int minimapSize = Double.valueOf(Math.floor(window.getWidth() / 8.4010989011)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 8.4010989011)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 4.72)).intValue();

        //minimap is squished in 16:10 so need to unsquish it
        BufferedImage minimapImage = rescaleByImageRatio(window, mapLeft, mapTop, width, height);

        if (ratio == MainProcess.AspectRatios.SIXTEEN_BY_NINE && streamRatio == MainProcess.AspectRatios.SIXTEEN_BY_TEN) {
            minimapImage = stretchVertically(minimapImage,0.11);
        }
        if (ratio == MainProcess.AspectRatios.SIXTEEN_BY_NINE && streamRatio == MainProcess.AspectRatios.FOUR_BY_THREE) {
            minimapImage = stretchVertically(minimapImage,0.25);
        }
        return minimapImage;
    }

    /**
     * 35x35 - 198x198  (totals 1310x736)
     * l  - 1310/35 = 37.4285
     * t  - 736/35  = 21.0285 (trial 21.1)
     * wh - 197-35  = 736/162 = 4.5432
     * wh - 198-35  = 736/163 = 4.5153
     *
     * 1310/163 = 8.03680981595
     * 736/163 = 4.51533742331
     *
     *  1310/35 = 37.4285714286
     */
    public BufferedImage getMinimapBorder(final BufferedImage window){
//        if (GameState.getIs1610Ratio()){
//            return getMinimapBorder1610squish(window);
//        }
        final int mapLeft = Double.valueOf(Math.floor(window.getWidth() / 37.4285)).intValue();
        final int mapTop = Double.valueOf(Math.floor(window.getHeight() / 21.0285)).intValue();
//        final int minimapSize = Double.valueOf(Math.floor(window.getWidth() / 8.03680981595)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 8.03680981595)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 4.51533742331)).intValue();
        return rescaleByImageRatio(window, mapLeft, mapTop, width, height);
    }

    public BufferedImage getMinimapBorder1610squish(final BufferedImage window){
        final int mapLeft = Double.valueOf(Math.floor(window.getWidth() / 37.4285)).intValue();
        final int mapTop = Double.valueOf(Math.floor(window.getHeight() / 21.0285)).intValue();
//        final int minimapSize = Double.valueOf(Math.floor(window.getWidth() / 8.03680981595)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 8.03680981595)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 4.51533742331)).intValue();

        double heightDiff =  0.1111; //11.11 percent difference
        int squishedTop = mapTop - (int) Math.floor(mapTop * heightDiff);
        int squishedHeight =  height - (int) Math.floor(height * heightDiff);
        return window.getSubimage(mapLeft, squishedTop, width, squishedHeight);
    }

    /**
     * (1920x1080 image)
     *            left   top
     * top left   52     37
     * bot right  286    214
     * l  - 1920/52 = 36.9231
     * t  - 1080/37  = 29.1891
     * width  - 286-52  = 1920/234 = 8.2051
     * height - 214-37  = 1080/177 = 6.1016
     *
     * 1310/163 = 8.03680981595
     * 736/163 = 4.51533742331
     *
     *  1310/35 = 37.4285714286
     */
    public BufferedImage getMinimapBorder43Squish(final BufferedImage window){
//        if (GameState.getIs1610Ratio()){
//            return getMinimapBorder1610squish(window);
//        }
        final int mapLeft = Double.valueOf(Math.floor(window.getWidth() / 36.9231)).intValue();
        final int mapTop = Double.valueOf(Math.floor(window.getHeight() / 27.0)).intValue();
//        final int minimapSize = Double.valueOf(Math.floor(window.getWidth() / 8.03680981595)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 8.01)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 6.00)).intValue();
        return window.getSubimage(mapLeft, mapTop, width, height);
    }


    /**
     * t  - 1080/115  = 9.39130434783
     */
    public BufferedImage getMinimapBorderDead(final BufferedImage window){
        final int mapLeft = Double.valueOf(Math.floor(window.getWidth() / 37.4285)).intValue();
        final int mapTop = Double.valueOf(Math.floor(window.getHeight() / 9.39130434783)).intValue();
//        final int minimapSize = Double.valueOf(Math.floor(window.getWidth() / 8.03680981595)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 8.03680981595)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 4.51533742331)).intValue();

        return rescaleByImageRatio(window, mapLeft, mapTop, width, height);
    }

    /**
     * t  - 1080/115  = 9.39130434783
     */
    public BufferedImage getMinimapBorderDead1610Squish(final BufferedImage window){
        final int mapLeft = Double.valueOf(Math.floor(window.getWidth() / 37.4285)).intValue();
        final int mapTop = Double.valueOf(Math.floor(window.getHeight() / 9.39130434783)).intValue();
//        final int minimapSize = Double.valueOf(Math.floor(window.getWidth() / 8.03680981595)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 8.03680981595)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 4.51533742331)).intValue();

        double heightDiff =  0.1111; //11.11 percent difference
        int squishedTop = mapTop - (int) Math.floor(mapTop * heightDiff);
        int squishedHeight =  height - (int) Math.floor(height * heightDiff);
        return window.getSubimage(mapLeft, squishedTop, width, squishedHeight);
    }

    /**
     * 35x250 - top left
     * 156x268 - bot right
     * 1416x796 - source image size
     *
     * l  - 1416/35 = 40.4571
     * t  - 796/250  = 3.184
     * w  - 156-35  = 1416/121 = 11.7024793388
     * h  - 268-250  = 796/18 = 44.2222222222
     */
    public BufferedImage getRoundClosing(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 40.4571)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 3.184)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 11.7024793388)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 44.2222222222)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     *  1920x1080 image
     *  940x93 top left
     *  983x118 bot right
     *
     *  left = 1920/940 = 2.04255319149 (-2 = 1920/938 = 2.04690831557)
     *  top  = 1080/93 = 11.6129032258
     *  width = 983-940 = 43 = 1920/43 = 44.6511627907 (+3 = 1920/46 = 41.7391304348)
     *  height = 118-93 = 25 = 1080/25 = 43.2 (+1 = 1080/26 = 41.5384615385)
     */
    public BufferedImage getDirection(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 2.04690831557)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 11.6129032258)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 41.7391304348)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 41.5384615385)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     *  1920x1080 image
     *  940x93 top left  (dead top 158)
     *  983x118 bot right
     *
     *  left = 1920/940 = 2.04255319149 (-2 = 1920/938 = 2.04690831557)
     *  top  = 1080/93 = 11.6129032258 | 1080/158 = 6.83544303797
     *  width = 983-940 = 43 = 1920/43 = 44.6511627907 (+3 = 1920/46 = 41.7391304348)
     *  height = 118-93 = 25 = 1080/25 = 43.2 (+1 = 1080/26 = 41.5384615385)
     */
    public BufferedImage getDirectionDead(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 2.04690831557)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 6.83544303797)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 41.7391304348)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 41.5384615385)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * t  - 1080/400  = 2.7
     */
    public BufferedImage getRoundClosingDead(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 40.4571)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 2.69)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 11.7024793388)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 44.2222222222)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    public BufferedImage getRoundClosingDead1610Squished(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 40.4571)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 2.69)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 11.7024793388)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 44.2222222222)).intValue();

        double heightDiff =  0.1111; //11.11 percent difference
        int squishedTop = top - (int) Math.floor(top * heightDiff);
        int squishedHeight =  height - (int) Math.floor(height * heightDiff);
        return window.getSubimage(left, squishedTop, width, squishedHeight);
    }

    /**
     * 69x205- top left
     * 81x216 - bot right
     * 1228x690 - source image size
     **
     * l  - 1228/69 = 17.7971014493
     * t  - 690/205 = 3.36585365854
     * w  - 1228/12 = 102.333333333
     * h  - 690/12 = 57.5
     *
     * 72x214 - top left
     * 84x228 - bot right
     * 1280x720 - source image size
     *
     * l - 1280 / 72 = 17.7777777778
     * t - 720 / 214 = 3.36448598131
     * w - 1280 / 12 = 106.666666667
     * h - 720/14 = 51.4285714286
     *
     */
    public BufferedImage getRound(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 17.7971014493)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 3.36585365854)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 102.333333333)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 51.4)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    /**
     * t  - 1080/383  = 2.81984334204
     */
    public BufferedImage getRoundDead(final BufferedImage window){
        final int left = Double.valueOf(Math.floor(window.getWidth() / 17.7971014493)).intValue();
        final int top = Double.valueOf(Math.floor(window.getHeight() / 2.81984334204)).intValue();
        final int width = Double.valueOf(Math.floor(window.getWidth() / 102.333333333)).intValue();
        final int height = Double.valueOf(Math.floor(window.getHeight() / 51.4)).intValue();

        return rescaleByImageRatio(window, left, top, width, height);
    }

    public BufferedImage getMinimapBorderDetectWindow(final BrowserImage window, int top){
        BufferedImage i = getGameWindow(window, "test");
        final int mapLeft = Double.valueOf(Math.floor(i.getWidth() / 37.4285)).intValue();
        final int mapTop = top;
        final int minimapSize = Double.valueOf(Math.floor(i.getHeight() / 4.5153)).intValue();

        try {
            if (top + minimapSize < i.getHeight()) {
                BufferedImage minimapTry = i.getSubimage(mapLeft, mapTop, minimapSize, minimapSize);
                return minimapTry;
            }
        }
        catch(RasterFormatException ex){
            System.out.println(i.getWidth() + " | " + i.getHeight());
            System.out.println(mapLeft + " | " + mapTop + " | " + minimapSize + " | " + minimapSize);
        }
        return null;
    }

    public static void writeImageToFile(final BufferedImage image, final String imageName) {
        try {
            if (image != null) {
                //XXX uncomment when debugging
//                ImageIO.write(image, "png", new File("src/main/resources/Output/" + imageName + ".png"));
            }
        }catch(Exception ex){
            // dont want excveptions thrown from debugging write
            System.out.println(ex);
        }
    }

    public static String writeToFile(final BufferedImage image, final String imageName) throws IOException {
        File userHome = new File("./Export");
        if (!userHome.exists()) {
            userHome.mkdirs(); // Create directories if they don't exist
        }

        String baseFileName = imageName + ".png";
        File propertiesFile = new File(userHome, baseFileName);

        // Check if the file already exists
        int attempt = 1;
        while (propertiesFile.exists()) {
            // If the file already exists, increment the attempt number
            baseFileName = imageName + "(" + attempt + ").png";
            propertiesFile = new File(userHome, baseFileName);
            attempt++;
        }

        ImageIO.write(image, "png", propertiesFile);
        return propertiesFile.getAbsolutePath();
    }

    public void setAspectStreamAspectRatio(MainProcess.AspectRatios ratio) {
        this.streamRatio = ratio;
    }

    public void setAspectRatio(MainProcess.AspectRatios ratio) {
        this.ratio = ratio;
    }
}
