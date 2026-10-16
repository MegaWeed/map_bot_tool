/*
 * MainProcess
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex;

import au.mapbot.apex.classes.*;
import au.mapbot.apex.client.OrchestratorClient;
import au.mapbot.apex.util.*;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang3.StringUtils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class MainProcess {

    String consoleValue = "";
    WebDriverUtil webDriverUtil = null;
    public SnapshotFromImage screenProcessToImage;
    public Ocr ocr;
    MapEnum detectedMap;
    MapEnum detectedMapPreviousMatchMap;
    public Boolean isRanked;
    ScreenshotUtil screenshotUtil;
    Robot robot;
    Integer squadsLeft;
    public Boolean isStreamedSquished;
    Boolean playerHasDied;
    public Boolean playerIsDead;
    Boolean wholeTeamDead;
    Boolean hasPlayerLanded;
    Integer playerLandedPoint;
    Integer currentRound;
    Boolean minimapRotation;
    CircleUtil circleUtil;
    ImageCompare imageCompare;
    BufferedImage currentMapAllowedLocations;
    BufferedImage currentFullColorMap;


    List<DetectedCircle> allDetectedCircles;
    List<DetectedCircle> allDetectedFullMapCircles;

    List<DetectedCircle> zonesToUse;
    List<ImageMatching> allPoints;

    List<PointList> pointList;

    List<ImageMatching> deadPoints;

    AspectRatios ratio;
    AspectRatios streamRatio;

    Long looptimeLong = 0L;
    List<Integer> killPoints;
    List<Integer> totalKillsListBuffer;
    List<Integer> squadsLeftList;

    List<ImageMatching> inDropshipPoints;
    Boolean gameIsRunning;

    List<String> displayEventLog;

    BufferedImage minimapToShow;
    boolean hasCheckedMinimapRotation;

    boolean isRoundClosing;
    boolean capture;

    boolean solos;
    boolean recheckMinimapZoomValues;
    private Languages languageGUI;
    private String gameTypeGUI;
    private AspectRatios streamAspectRatioGUI;
    private AspectRatios aspectRatioGUI;
    private Boolean minimapRotationGUI;
    private MapEnum currentMapGUI;
    private Integer gameTypeComboGuiIndex;
    private String streamNameOrVodId;

    private List<String> twitchKillClipList;

    boolean serverHosted;

    public String gameStateString;
    public boolean saved;

    public void stop() {
        MBLogger.log("Stop command recieved");
        capture = false;
        gameIsRunning = false;
        if (webDriverUtil != null) {
            webDriverUtil.shutdown();
        }
        if (mapThread != null && mapThread.isAlive()) {
            mapThread.end();  // Custom method to signal the thread to end
            try {
                mapThread.join(5000); // Wait for the thread to finish
                if (mapThread.isAlive()) {
                    mapThread.interrupt(); // Force stop if it's still running
                    mapThread.join(); // Ensure it stops before proceeding
                }
            } catch (InterruptedException e) {
                //
            }
        }
    }

    public PassData getData(){
        List<PointList> points = new ArrayList<>();
        for (ImageMatching im : allPoints){
            PointList pl = new PointList();
            pl.setPoint(im.getPoint());
            points.add(pl);
        }
        AspectRatios sRatio = null;
        if (streamAspectRatioGUI != null){
            sRatio = streamAspectRatioGUI;
        }else{
            if (isStreamedSquished != null){
                if (isStreamedSquished){
                    if (streamRatio != null && streamRatio == AspectRatios.FOUR_BY_THREE){
                        sRatio = AspectRatios.FOUR_BY_THREE;
                    }else{
                        sRatio = AspectRatios.SIXTEEN_BY_TEN;
                    }
                }else{
                    sRatio = AspectRatios.SIXTEEN_BY_NINE;
                }
            }
        }

        Integer gameTypeGuiIndex = null;
        if (gameTypeGUI == null) {
            if (solos) {
                gameTypeGuiIndex = 3;
            } else if (algs != null && algs) {
                gameTypeGuiIndex = 4;
            } else if (isRanked != null && isRanked) {
                gameTypeGuiIndex = 1;
            } else if (isRanked != null && !isRanked) {
                gameTypeGuiIndex = 2;
            }
        }else{
            if (gameTypeGUI.equals("Unranked Solo's")) {
                gameTypeGuiIndex = 3;
            }else if (gameTypeGUI.equals("Ranked")){
                gameTypeGuiIndex = 1;
            }else if (gameTypeGUI.equals("Unranked")){
                gameTypeGuiIndex = 2;
            }else if (gameTypeGUI.equals("ALGS/Customs")){
                gameTypeGuiIndex = 4;
            }
        }
        MapEnum map;
        if (currentMapGUI != null){
            map = currentMapGUI;
        }else{
            map = detectedMap;
        }
        List<DetectedCircle> adjustCircles = CircleUtil.adjustCircles(List.copyOf(zonesToUse));

        long time = 0l;
        if (startTime != null){
            time = startTime.getTime();
        }
        RoundStatus rs = new RoundStatus(currentRound, isRoundClosing);

        return new PassData(map, squadsLeft, points, List.copyOf(killPoints), playerLandedPoint, adjustCircles, dropshipLine, List.copyOf(displayEventLog), minimapToShow, minimapRotation, ratio, sRatio, language, gameTypeGuiIndex, beaconPoints, evoPoints, rcPoints, streamNameOrVodId, twitchKillClipList, time, gameStateString, rs);
    }

    public void setManualLanguage(Languages language) {
        this.languageGUI = language;
        this.language = language;
        logAndDisplay("Language manually set to: " + language);
        if (ocr != null){
            if (language == Languages.JAPANESE) {
                ocr.setLanguageJpn();
            }else if (language == Languages.CHINESE) {
                ocr.setLanguageChs();
            }else{
                ocr.setLanguageEng();
            }
        }
    }

    public void setGameType(String gameTypeOption) {
        if (gameTypeOption != null && gameTypeOption.isBlank()){
            this.gameTypeGUI = null;
        }else{
            logAndDisplay("Game Type manually set to: " + gameTypeOption);
            this.gameTypeGUI = gameTypeOption;
        }
    }

    public void setStreamAspectRatio(AspectRatios ratio) {
        logAndDisplay("Stream Ratio manually set to: " + ratio);
        this.streamAspectRatioGUI = ratio;
        if (streamAspectRatioGUI != null){
            streamRatio = streamAspectRatioGUI;
            screenProcessToImage.setAspectStreamAspectRatio(streamAspectRatioGUI);
            if (this.ratio != null && this.ratio == AspectRatios.SIXTEEN_BY_NINE && streamAspectRatioGUI == AspectRatios.SIXTEEN_BY_TEN) {
                isStreamedSquished = true;
            }
            if (this.ratio != null && this.ratio == AspectRatios.SIXTEEN_BY_NINE && streamAspectRatioGUI == AspectRatios.SIXTEEN_BY_NINE) {
                isStreamedSquished = false;
            }
            if (this.ratio != null && this.ratio == AspectRatios.SIXTEEN_BY_NINE && streamAspectRatioGUI == AspectRatios.FOUR_BY_THREE) {
                isStreamedSquished = true;
            }
        }
    }
    public void setAspectRatio(AspectRatios ratio) {
        logAndDisplay("Aspect Ratio manually set to: " + ratio);
        this.aspectRatioGUI = ratio;
        this.ratio = ratio;
        screenProcessToImage.setAspectRatio(ratio);
    }

    public void setMinimapRotation(Boolean rotation) {
        logAndDisplay("Minimap rotation manually set to: " + rotation);
        this.minimapRotationGUI = rotation;
        this.minimapRotation = rotation;
    }

    public void setMap(String mapOption) {
        if (StringUtils.isNotBlank(mapOption)) {
            logAndDisplay("Map manually set to: " + mapOption);
            this.currentMapGUI = MapEnum.parseFromDisplayNameString(mapOption);
            this.detectedMap = currentMapGUI;

            try {
                this.currentMapAllowedLocations = ImageUtil.getImageFromResource(currentMapGUI.getMapAllowedLocationResourceString());
                this.currentFullColorMap = au.mapbot.apex.util.CustomMaps.load(currentMapGUI, true);
                if (mapThread != null) {
                    mapThread.setMap(currentFullColorMap);
                }
            }catch (Exception ex){
                //try and set map if thread is ruinning
            }
        }else{
            this.currentMapGUI = null;
        }
    }


    public enum AspectRatios {
        SIXTEEN_BY_NINE, SIXTEEN_BY_TEN, FOUR_BY_THREE;
    }

    GameStates gameState;
    public enum GameStates {
        UNKNOWN, HOME, LOBBY, INGAME;
    }

    public Languages language = null;
    public enum Languages {
        ENGLISH, JAPANESE, CHINESE;
    }

    /** Japanese and Chinese share the CJK screen regions and the 2-character text comparison. */
    public static boolean isCjk(final Languages language) {
        return language == Languages.JAPANESE || language == Languages.CHINESE;
    }

    List<TimeSnapshot> timeSnapshots;

    Date startTime;

    Boolean algs;

    Line dropshipLine;

    Date loopTime;
    Integer loopcount;

    BufferedImage skull = ImageUtil.getImageFromResource("/skull.png");
    BufferedImage skullLeader = ImageUtil.getImageFromResource("/skullLeader.png");

    MapThread mapThread;
    Set<Point> beaconPoints;
    Set<Point> evoPoints;
    Set<Point> rcPoints;

    Integer skipCount;
    Double zoom;

//    String hlsUrl;
//    VideoFrameProcessor fFmpegCapture;

    public MainProcess() throws AWTException, IOException {
        screenProcessToImage = new SnapshotFromImage();
        gameState = GameStates.UNKNOWN;
        ocr = new Ocr();
        screenshotUtil = new ScreenshotUtil();
        robot = new Robot();
        isStreamedSquished = null;
        playerHasDied = false;
        playerIsDead = false;
        wholeTeamDead = false;
        hasPlayerLanded = false;
        minimapRotation = null;
        currentRound = 0;
        circleUtil = new CircleUtil();
        imageCompare = new ImageCompare();
        allDetectedCircles = new ArrayList<>();
        allDetectedFullMapCircles = new ArrayList<>();
        zonesToUse = new ArrayList<>();
        allPoints = new ArrayList<>();
        deadPoints = new ArrayList<>();
        squadsLeft = 20;
        killPoints = new ArrayList<>();
        totalKillsListBuffer = new ArrayList<>();
        squadsLeftList = new ArrayList<>();
        timeSnapshots = new ArrayList<>();
        capture = true;
        playerLandedPoint = null;
        isRoundClosing = false;
        gameIsRunning = true;
        startTime = null;
        detectedMap = null;
        detectedMapPreviousMatchMap = null;
        isRanked = null;
        ratio = null;
        currentMapAllowedLocations = null;
        currentFullColorMap = null;
        inDropshipPoints = new ArrayList<>();
        pointList = new ArrayList<>();
        algs = false;
        dropshipLine = null;
        displayEventLog = new ArrayList<>();
        loopTime = null;
        solos = false;
        loopcount = 0;
        recheckMinimapZoomValues =false;
        mapThread = null;
        minimapToShow = null;
        beaconPoints = new HashSet<>();
        rcPoints = new HashSet<>();
        evoPoints = new HashSet<>();
        skipCount = 0;
        zoom = null;
        hasCheckedMinimapRotation = false;
        streamNameOrVodId = "";
        twitchKillClipList = new ArrayList<>();
        serverHosted =false;
        //hlsUrl = null;
        gameStateString = "";
//        fFmpegCapture = null;
        saved = false;
    }
    public static void main(String[] args) throws AWTException, TesseractException, IOException, InterruptedException, TimeoutException {
        MainProcess p2 = new MainProcess();
        //Verhulst ranked game
//        p2.process(false, "https://www.twitch.tv/videos/2146639408?t=04h18m10s");
        //sorawiz bad data twtiter message
        p2.process(false, "https://www.twitch.tv/videos/2148135446?t=04h17m38s", false);
        //game 2: https://www.twitch.tv/videos/2148135446?t=04h42m29s
        //lou Col scrims (has minimap rotation)
        //https://www.twitch.tv/videos/2152623747?t=00h03m24s
    }

    public class TimeSnapshot{

        long currentTIme;

        String name;
        public TimeSnapshot(String name){
            this.name = name;
            this.currentTIme = new Date().getTime();
        }
    }

    public static class PassData{
        MapEnum map;
        Integer squadsLeft;
        List<PointList> points;
        List<Integer> kills;
        Integer landpoint;
        List<DetectedCircle> zones;
        Line dropshipLine;
        List<String> eventLog;
        BufferedImage minimap;
        Boolean minimapRotation;
        AspectRatios ratio;
        AspectRatios streamRatio;
        Languages language;
        Integer gameTypeGuiIndex;
        Set<Point> beaconPoints;
        Set<Point> evoPoints;
        Set<Point> rcPoints;
        String stream;

        List<String> killClips;

        long datetime;

        String gamestate;
        RoundStatus roundStatus;

        public PassData(final MapEnum map,
                        final Integer squadsLeft,
                        final List<PointList> points,
                        final List<Integer> kills,
                        final Integer landpoint,
                        final List<DetectedCircle> zones,
                        final Line dropshipLine,
                        final List<String> eventLog,
                        final BufferedImage minimap,
                        final Boolean minimapRotation,
                        final AspectRatios aRatio,
                        final AspectRatios sARatio,
                        final Languages language,
                        final Integer gameType,
                        final Set<Point> beaconPoints,
                        final Set<Point> evoPoints,
                        final Set<Point> rcPoints,
                        final String stream,
                        final List<String> killClips,
                        final long datetime,
                        final String gameStateIn,
                        final RoundStatus round) {
            this.map = map;
            this.squadsLeft = squadsLeft;
            this.points = points;
            this.kills = kills;
            this.landpoint = landpoint;
            this.zones = zones;
            this.dropshipLine = dropshipLine;
            this.eventLog = eventLog;
            this.minimap = minimap;
            this.ratio = aRatio;
            this.streamRatio = sARatio;
            this.minimapRotation = minimapRotation;
            this.language = language;
            this.gameTypeGuiIndex = gameType;
            this.beaconPoints = beaconPoints;
            this.evoPoints = evoPoints;
            this.rcPoints = rcPoints;
            this.stream = stream;
            this.killClips = killClips;
            this.datetime = datetime;
            this.gamestate = gameStateIn;
            this.roundStatus = round;
        }


        public List<DetectedCircle> getZones() {
            return zones;
        }
        public BufferedImage getMinimap(){
            return minimap;
        }
        public MapEnum getMap() {
            return map;
        }
        public Integer getSquadsLeft() {
            return squadsLeft;
        }
        public List<PointList> getPoints() {
            return points;
        }
        public List<Integer> getKills() {
            return kills;
        }
        public Integer getLandpoint() {
            return landpoint;
        }
        public Line getDropshipLine() {
            return dropshipLine;
        }
        public List<String> getEventLog() {
            return eventLog;
        }
        public Boolean getMinimapRotation() {
            return minimapRotation;
        }
        public AspectRatios getRatio() {
            return ratio;
        }
        public AspectRatios getStreamRatio() {
            return streamRatio;
        }
        public Languages getLanguage() {
            return language;
        }
        public Integer getGameTypeGuiIndex() {
            return gameTypeGuiIndex;
        }
        public Set<Point> getBeaconPoints() {
            return beaconPoints;
        }
        public Set<Point> getEvoPoints() {
            return evoPoints;
        }
        public Set<Point> getRcPoints() {
            return rcPoints;
        }
        public List<String> getKillClips() {return killClips;}

        public long getDatetime() {
            return datetime;
        }
        public String getGamestate(){return gamestate;}
        public RoundStatus getRoundStatus(){ return roundStatus;}
        @Override
        public String toString() {

            StringBuilder pointString = new StringBuilder();
            for (PointList point : points){
                if (!pointString.toString().isEmpty()){
                    pointString.append(",");
                }
                pointString.append("{\"x\":").append(point.getPoint().x).append(",").append("\"y\":").append(point.getPoint().y).append("}");
            }
            StringBuilder killString = new StringBuilder();
            for (Integer kill : kills){
                if (!killString.toString().isEmpty()){
                    killString.append(",");
                }
                killString.append(kill);
            }
            StringBuilder killClipString = new StringBuilder();
            for (String killClip : killClips){
                if (!killClipString.toString().isEmpty()){
                    killClipString.append(",");
                }
                killClipString.append("\"").append(killClip).append("\"");
            }
            StringBuilder zonesString = new StringBuilder();

            for (DetectedCircle zone : zones){
                if (!zonesString.toString().isEmpty()){
                    zonesString.append(",");
                }
                zonesString.append("{\"x\":").append(zone.getCenter().x).append(",").append("\"y\":").append(zone.getCenter().y).append(",").append("\"radius\":").append(zone.getRadius()).append("}");
            }
            StringBuilder beaconString = new StringBuilder();
            for (Point point : beaconPoints){
                if (!beaconString.toString().isEmpty()){
                    beaconString.append(",");
                }
                beaconString.append("{\"x\":").append(point.x).append(",").append("\"y\":").append(point.y).append("}");
            }
            StringBuilder rcString = new StringBuilder();
            for (Point point : rcPoints){
                if (!rcString.toString().isEmpty()){
                    rcString.append(",");
                }
                rcString.append("{\"x\":").append(point.x).append(",").append("\"y\":").append(point.y).append("}");
            }

            String gameType = "";
            if (gameTypeGuiIndex != null) {
                if (gameTypeGuiIndex == 3) {
                    gameType = "Unranked Solo's";
                } else if (gameTypeGuiIndex == 1) {
                    gameType = "Ranked";
                } else if (gameTypeGuiIndex == 2) {
                    gameType = "Unranked";
                } else if (gameTypeGuiIndex == 4) {
                    gameType = "ALGS/Customs";
                }
            }

            String gameStateOut = gamestate;
            if (roundStatus != null){
                if (roundStatus.round > 0){
                    gameStateOut = gamestate + " | Round: " + roundStatus.round + (roundStatus.isRoundClosing ? " closing" : "");
                }
            }

            return "{" +
                        "\"map\":\"" + map + "\"" +
                        ",\"datetime\":" + datetime +
                        ", \"stream\":\"" + stream + "\"" +
                        ", \"squadsLeft\":\"" + squadsLeft + "\"" +
                        ", \"points\":{ \"point\":[" + pointString + "]}" +
                        ", \"kills\":[" + killString + "]" +
                        ", \"killsClips\":[" + killClipString + "]" +
                        ", \"landpoint\":\"" + landpoint + "\"" +
                        ", \"zones\":{ \"zone\":[" + zonesString + "]}" +
                        //", dropshipLine:" + dropshipLine +
                        ", \"beaconPoints\":{ \"point\":[" + beaconString + "]}" +
                        ", \"rcPoints\":{ \"point\":[" + rcString + "]}" +
                        ", \"gameType\":\"" + gameType + "\"" +
                        ", \"gameState\":\"" + gameStateOut + "\"" +
                    "}";
        }
    }

    public void resetPerGameValues(){
        if (allPoints.size() <= 3){
            if (streamAspectRatioGUI == null){
                streamRatio = null;
                isStreamedSquished = null;
            }
        }
        gameIsRunning = true;
        gameState = GameStates.UNKNOWN;
        playerHasDied = false;
        playerIsDead = false;
        wholeTeamDead = false;
        hasPlayerLanded = false;
        currentRound = 0;
        allDetectedCircles = new ArrayList<>();
        allDetectedFullMapCircles = new ArrayList<>();
        zonesToUse = new ArrayList<>();
        allPoints = new ArrayList<>();
        deadPoints = new ArrayList<>();
        squadsLeft = 20;
        killPoints = new ArrayList<>();
        totalKillsListBuffer = new ArrayList<>();
        squadsLeftList = new ArrayList<>();
        timeSnapshots = new ArrayList<>();
        playerLandedPoint = null;
        isRoundClosing = false;
        startTime = null;
        detectedMap = null;
        detectedMapPreviousMatchMap = null;
        isRanked = null;
        currentMapAllowedLocations = null;
        currentFullColorMap = null;
        inDropshipPoints = new ArrayList<>();
        pointList = new ArrayList<>();
        algs = false;
        dropshipLine = null;
        loopTime = null;
        solos = false;
        loopcount = 0;
        recheckMinimapZoomValues = false;

        try {
            if (mapThread != null) {
                mapThread.end();
                mapThread.join(4000); // Wait for thread to finish
                // Ensure any running thread is properly stopped before replacing it
                if (mapThread != null) {
                    mapThread.interrupt(); // Request stop
                    mapThread.join(); // Wait for thread to finish
                }
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
        mapThread = null;

        minimapToShow = null;
        beaconPoints = new HashSet<>();
        rcPoints = new HashSet<>();
        evoPoints = new HashSet<>();
        skipCount = 0;
        zoom = null;
        hasCheckedMinimapRotation = false;
        if (ocr != null) {
            ocr.cleanup();
        }
        ocr = new Ocr(); // reinitilaise OCR to make sure GC can clean up resources
        twitchKillClipList = new ArrayList<>();
        gameStateString = "";
        saved = false;
        //manually trigger GC
        Runtime.getRuntime().gc();
    }

    public void logAndDisplay(String log){
        String prefixGameTime = "";
        if (startTime != null){
            prefixGameTime = formatMillisecondsToMMSS(new Date().getTime() - startTime.getTime()) + " - ";
        }
        MBLogger.log(prefixGameTime + log);
        displayEventLog.add(prefixGameTime + log);
    }

    public static String formatMillisecondsToMMSS(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }




    public void process(final boolean isLocalGame, final String streamOrVodText, boolean isHeadless) throws IOException, TesseractException, AWTException, InterruptedException, TimeoutException {
        //start tesseract
        Tesseract.initTesseract();

        if (isHeadless){
            //headless mode is only set for server version
            serverHosted = true;
        }

        //initialise webdriver for first pass if stream/vod capture
        if (!isLocalGame && webDriverUtil == null){
            if (isHeadless){
                WebDriverUtil.TwitchVodInfo tvi = WebDriverUtil.extractTwitchVodInfo(streamOrVodText);
                if (tvi != null) {
                    if (tvi.getChannel() != null) {
                        //hlsUrl = StreamlinkFFMPEGScreenshotUtil.getHlsUrl(tvi.getChannel());
//                        if (fFmpegCapture == null) {
//                            fFmpegCapture = new VideoFrameProcessor(tvi.getChannel());
//                        }
                        webDriverUtil = new WebDriverUtil(true, tvi.getChannel());


                    }
                } else {
                    //hlsUrl = StreamlinkFFMPEGScreenshotUtil.getHlsUrl(streamOrVodText);
//                    if (fFmpegCapture == null) {
//                        fFmpegCapture = new VideoFrameProcessor(streamOrVodText);
//                    }
                    webDriverUtil = new WebDriverUtil(true, streamOrVodText);
                }
            }else{
                webDriverUtil = new WebDriverUtil(isHeadless, streamOrVodText);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(webDriverUtil::shutdown));
        }

        if (StringUtils.isNotEmpty(streamOrVodText)){
            logAndDisplay("Using VOD/Stream: " + streamOrVodText);
            streamNameOrVodId = streamOrVodText;
        }
        if (isLocalGame){
            logAndDisplay("Recording locally played game of apex");
        }

        while (capture) {
            //resets values that will change every game but not things like language, aspectRatio, minimap rotation
            // things that took time to calculate, if found in a previous game in same session
            if (mapThread != null){
                mapThread.end();
            }

            //if all ponts is empty here it is the first time through and we don't want to reset initial values
            if(allPoints.isEmpty()){
                gameTypeGUI = null;
                currentMapGUI = null;
            }

            logAndDisplay("Resetting per game values");
            resetPerGameValues();


            while (gameIsRunning && capture) {

                timeSnapshots.clear();

                if (loopTime != null) {
                    Date tmpDate = new Date();
                    looptimeLong = tmpDate.getTime() - loopTime.getTime();
                    if (looptimeLong < 3000){
                        //loop only every 5 seconds
                        System.out.println("Process Loop Time Taken:" + looptimeLong + "ms");
                        Thread.sleep(3000-looptimeLong);
                    }
                    loopTime = tmpDate;
                } else {
                    loopTime = new Date();
                }



                //set values from GUI
                if (languageGUI != null){
                    language = languageGUI;
                }
                if (gameTypeGUI != null){
                    if (gameTypeGUI.equals("Unranked Solo's")) {
                        solos = true;
                        isRanked = true;
                    }else if (gameTypeGUI.equals("Ranked")){
                        isRanked = true;
                    }else if (gameTypeGUI.equals("Unranked")){
                        isRanked = false;
                    }else if (gameTypeGUI.equals("ALGS/Customs")){
                        playerHasDied = true;
                        isRanked = false;
                        algs = true;
                        zoom = 1.27;
                    }
                }
                if (aspectRatioGUI != null){
                    screenProcessToImage.setAspectRatio(aspectRatioGUI);
                    ratio = aspectRatioGUI;
                }
                if (streamAspectRatioGUI != null){
                    screenProcessToImage.setAspectStreamAspectRatio(streamAspectRatioGUI);
                    if (ratio != null && ratio == AspectRatios.SIXTEEN_BY_NINE && streamAspectRatioGUI == AspectRatios.SIXTEEN_BY_TEN) {
                        isStreamedSquished = true;
                    }
                    if (ratio != null && ratio == AspectRatios.SIXTEEN_BY_NINE && streamAspectRatioGUI == AspectRatios.FOUR_BY_THREE) {
                        isStreamedSquished = true;
                    }
                    if (ratio != null && ratio == AspectRatios.SIXTEEN_BY_NINE && streamAspectRatioGUI == AspectRatios.SIXTEEN_BY_NINE) {
                        isStreamedSquished = false;
                    }
                }
                if (minimapRotationGUI != null){
                    if (minimapRotationGUI){
                        minimapRotation = true;
                    }else{
                        minimapRotation = false;
                    }
                }
                if (currentMapGUI != null && detectedMap != currentMapGUI){
                    detectedMap = currentMapGUI;
                    currentMapAllowedLocations = ImageUtil.getImageFromResource(currentMapGUI.getMapAllowedLocationResourceString());
                    currentFullColorMap = au.mapbot.apex.util.CustomMaps.load(currentMapGUI, true);
                    if (mapThread != null){
                        mapThread.setMap(currentFullColorMap);
                    }
                }

                //rest internal values if after a few passes we still don't have good data
                if (skipCount > 3 && allPoints.size() < skipCount && allPoints.size() < 7){
                    logAndDisplay("resetting internal values if not manually set in UI");
                    allPoints.clear();
                    skipCount = 0;

                    if (minimapRotationGUI == null){
                        logAndDisplay("minimap rotation not manually set, resetting detected value");
                        minimapRotation = null;
                        hasCheckedMinimapRotation = false;
                    }
                    if (aspectRatioGUI == null){
                        logAndDisplay("aspect ratio not manually set, resetting detected value");
                        ratio = null;
                    }
                    if (streamAspectRatioGUI == null){
                        logAndDisplay("stream ratio not manually set, resetting detected value");
                        streamRatio = null;
                        isStreamedSquished = null;
                    }
                    if (gameTypeGUI == null || gameTypeGUI.isEmpty()){
                        logAndDisplay("gameType not manually set, resetting detected value");
                        algs = null;
                        zoom = null;
                    }else if (gameTypeGUI.equals("ALGS/Customs")){
                        //don't want to reset zoom here, but otherwise should
                    }else{
                        logAndDisplay("minimap zoom reset");
                        algs = null;
                        zoom = null;
                    }
                }



                //get the game image
                BufferedImage gameImage = null;
//                if (mapThread!= null && mapThread.getGameImage() != null){
//                    gameImage = mapThread.getGameImage();
//                }else{
                    if (isHeadless){
                        gameImage = getGameImage(isLocalGame, "true");
                    }else {
                        gameImage = getGameImage(isLocalGame);
                    }
//                }
                timeSnapshots.add(new TimeSnapshot("GameImage"));

                //game image needs to be captured (might be null if window isn't active so can't capture)
                if (gameImage != null) {
                    //only need to detect ratio if not already set
                    if (aspectRatioGUI == null) {
                        //get resolution of game image
                        if (ratio == null) {
                            if (checkImageResolutionIs169(gameImage)) {
                                ratio = AspectRatios.SIXTEEN_BY_NINE;
                                logAndDisplay("Image aspectRatio detected as 16:9");
                                screenProcessToImage.setAspectRatio(ratio);
                            } else if (checkImageResolutionIs1610(gameImage)) {
                                ratio = AspectRatios.SIXTEEN_BY_TEN;
                                logAndDisplay("Image aspectRatio detected as 16:10");
                                screenProcessToImage.setAspectRatio(ratio);
                            } else {
                                logAndDisplay("Could not detect a valid aspectRatio from: " + gameImage.getWidth() + "x" + gameImage.getHeight() + ", defaulting to 16:9");
                            }
                        }
                    }
                    timeSnapshots.add(new TimeSnapshot("Check Image Ratio"));


                    //pregame states
                    if (gameState == GameStates.UNKNOWN || gameState == GameStates.LOBBY || gameState == GameStates.HOME) {
                        //is in main menu
                        if (gameState != GameStates.HOME) {
                            //deadtext is the same sceen zone as main menu
                            final BufferedImage topMenuText = screenProcessToImage.getTopMainMenuText(gameImage);
                            if (isInMainMenu(topMenuText)) {
                                gameState = GameStates.HOME;
                                logAndDisplay("Detected currently on Main Menu");
                                gameStateString = "In Menu";
                            }
                            if (isLobby(topMenuText)) {
                                gameState = GameStates.LOBBY;
                                logAndDisplay("Detected currently on Lobby");
                                gameStateString = "In Lobby";
                            }
                            timeSnapshots.add(new TimeSnapshot("Home Screen Check"));
                        }


                        //look for ranked text on lobby screen if ranked value hasn't been set
//                        if (isRanked == null) {
//                            if (checkRankedTextLobby(gameImage)) {
//                                isRanked = true;
//                                gameState = GameStates.LOBBY;
//                                logAndDisplay("Ranked game detected from lobby or champ select");
//                                gameStateString = "In Lobby";
//
//                            }
//                            timeSnapshots.add(new TimeSnapshot("Pregame RankedCheck"));
//                        }

                        //look for map text on lobby screen if ranked value hasn't been set, update if different value is found
                        if (currentMapGUI == null) {
                            MapEnum getMapFromScreenText = checkMap(gameImage);
                            if (getMapFromScreenText != null) {
                                if (detectedMap == null || getMapFromScreenText != detectedMap) {
                                    detectedMap = getMapFromScreenText;
                                    gameState = GameStates.LOBBY;
                                    currentMapAllowedLocations = ImageUtil.getImageFromResource(detectedMap.getMapAllowedLocationResourceString());
                                    currentFullColorMap = au.mapbot.apex.util.CustomMaps.load(detectedMap, true);
                                    if (mapThread != null){
                                        mapThread.setMap(currentFullColorMap);
                                    }
                                    logAndDisplay("Detected map being played in Lobby or champ select: " + detectedMap.getText());
//                                    gameStateString = "In Lobby/Champ Select Screen";
                                }
                            }
                        }else{
                            if (currentMapGUI != detectedMap){
                                detectedMap = currentMapGUI;
                                currentMapAllowedLocations = ImageUtil.getImageFromResource(detectedMap.getMapAllowedLocationResourceString());
                                currentFullColorMap = au.mapbot.apex.util.CustomMaps.load(detectedMap, true);
                                if (mapThread != null){
                                    mapThread.setMap(currentFullColorMap);
                                }
                                logAndDisplay("Map set by GUI: " + detectedMap.getText());
//                                gameStateString = "In Lobby/Champ Select Screen";
                            }
                        }
                        timeSnapshots.add(new TimeSnapshot("Pregame detected map"));


                        //the ping suggest launch text that appears when players are now in dropship, pregame has ended
                        if (languageGUI == null && language == null) {
                            BufferedImage dropImgEng = screenProcessToImage.getDropTextFromWindowStream(gameImage);
                            BufferedImage dropImgJP = screenProcessToImage.getDropTextFromWindowStreamJP(gameImage);
                            if (inShipCheckEnglish(dropImgEng)) {
                                language = Languages.ENGLISH;
                                gameState = GameStates.INGAME;
                                logAndDisplay("English Language Detected");
                                logAndDisplay("Player in dropship, game has started");
                                gameStateString = "In dropship, Game started";
                            } else if (inShipCheckJapanese(dropImgJP)) {
                                language = Languages.JAPANESE;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Japanese Language Detected");
                                logAndDisplay("Player in dropship, game has started");
                                gameStateString = "In dropship, Game started";
                            } else if (inShipCheckChinese(dropImgJP)) {
                                language = Languages.CHINESE;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Chinese Language Detected");
                                logAndDisplay("Player in dropship, game has started");
                                gameStateString = "In dropship, Game started";
                            }
                        }else if (languageGUI == Languages.ENGLISH || language == Languages.ENGLISH){
                            BufferedImage dropImgEng = screenProcessToImage.getDropTextFromWindowStream(gameImage);
                            if (inShipCheckEnglish(dropImgEng)) {
                                gameState = GameStates.INGAME;
                                logAndDisplay("Player in dropship, game has started");
                                gameStateString = "In dropship, Game started";
                            }
                        }else if (languageGUI == Languages.JAPANESE || language == Languages.JAPANESE){
                            BufferedImage dropImgJP = screenProcessToImage.getDropTextFromWindowStreamJP(gameImage);
                            if (inShipCheckJapanese(dropImgJP)) {
                                gameState = GameStates.INGAME;
                                logAndDisplay("Player in dropship, game has started");
                                gameStateString = "In dropship, Game started";
                            }
                        }else if (languageGUI == Languages.CHINESE || language == Languages.CHINESE){
                            BufferedImage dropImgJP = screenProcessToImage.getDropTextFromWindowStreamJP(gameImage);
                            if (inShipCheckChinese(dropImgJP)) {
                                gameState = GameStates.INGAME;
                                logAndDisplay("Player in dropship, game has started");
                                gameStateString = "In dropship, Game started";
                            }
                        }

                        timeSnapshots.add(new TimeSnapshot("In Dropship"));

                        //the squadsleft text in top right (two different locations based on if ranked or not, pregame has ended)
                        if (languageGUI == null && language == null) {
                            BufferedImage squadsLeftRankedEng = screenProcessToImage.getSquadsLeftRanked(gameImage);
                            BufferedImage squadsLeftUnRankedEng = screenProcessToImage.getSquadsLeftUnRanked(gameImage);
                            BufferedImage squadsLeftRankedJpn = screenProcessToImage.getSquadsLeftRankedJP(gameImage);
                            BufferedImage squadsLeftUnRankedJpn = screenProcessToImage.getSquadsLeftUnRankedJP(gameImage);
                            if (getSquadLeftEng(squadsLeftRankedEng)) {
                                isRanked = true;
                                language = Languages.ENGLISH;
                                gameState = GameStates.INGAME;
                                logAndDisplay("English Language Detected");
                                logAndDisplay("Ranked Game has started");
                                gameStateString = "In Game";

                            } else if (getSquadLeftEng(squadsLeftUnRankedEng)) {
                                isRanked = false;
                                language = Languages.ENGLISH;
                                gameState = GameStates.INGAME;
                                logAndDisplay("English Language Detected");
                                logAndDisplay("UnRanked Game has started");
                                gameStateString = "In Game";
                            } else if (getSquadLeftJpn(squadsLeftRankedJpn)) {
                                isRanked = true;
                                language = Languages.JAPANESE;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Japanese Language Detected");
                                logAndDisplay("Ranked Game has started");
                                gameStateString = "In Game";
                            } else if (getSquadLeftJpn(squadsLeftUnRankedJpn)) {
                                isRanked = false;
                                language = Languages.JAPANESE;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Japanese Language Detected");
                                logAndDisplay("Unranked Game has started");
                                gameStateString = "In Game";
                            } else if (getSquadLeftChs(squadsLeftRankedJpn)) {
                                isRanked = true;
                                language = Languages.CHINESE;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Chinese Language Detected");
                                logAndDisplay("Ranked Game has started");
                                gameStateString = "In Game";
                            } else if (getSquadLeftChs(squadsLeftUnRankedJpn)) {
                                isRanked = false;
                                language = Languages.CHINESE;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Chinese Language Detected");
                                logAndDisplay("Unranked Game has started");
                                gameStateString = "In Game";
                            }
                        } else if (languageGUI == Languages.ENGLISH || language == Languages.ENGLISH){
                            BufferedImage squadsLeftRankedEng = screenProcessToImage.getSquadsLeftRanked(gameImage);
                            BufferedImage squadsLeftUnRankedEng = screenProcessToImage.getSquadsLeftUnRanked(gameImage);
                            if (getSquadLeftEng(squadsLeftRankedEng)) {
                                isRanked = true;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Ranked Game has started");
                                gameStateString = "In Game";
                            } else if (getSquadLeftEng(squadsLeftUnRankedEng)) {
                                isRanked = false;
                                gameState = GameStates.INGAME;
                                logAndDisplay("UnRanked Game has started");
                                gameStateString = "In Game";
                            }
                        } else if (languageGUI == Languages.JAPANESE || language == Languages.JAPANESE){
                            BufferedImage squadsLeftRankedJpn = screenProcessToImage.getSquadsLeftRankedJP(gameImage);
                            BufferedImage squadsLeftUnRankedJpn = screenProcessToImage.getSquadsLeftUnRankedJP(gameImage);
                            if (getSquadLeftJpn(squadsLeftRankedJpn)) {
                                isRanked = true;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Ranked Game has started");
                                gameStateString = "In Game";
                            } else if (getSquadLeftJpn(squadsLeftUnRankedJpn)) {
                                isRanked = false;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Unranked Game has started");
                                gameStateString = "In Game";
                            }
                        } else if (languageGUI == Languages.CHINESE || language == Languages.CHINESE){
                            BufferedImage squadsLeftRankedChs = screenProcessToImage.getSquadsLeftRankedJP(gameImage);
                            BufferedImage squadsLeftUnRankedChs = screenProcessToImage.getSquadsLeftUnRankedJP(gameImage);
                            if (getSquadLeftChs(squadsLeftRankedChs)) {
                                isRanked = true;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Ranked Game has started");
                                gameStateString = "In Game";
                            } else if (getSquadLeftChs(squadsLeftUnRankedChs)) {
                                isRanked = false;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Unranked Game has started");
                                gameStateString = "In Game";
                            }
                        }
                        timeSnapshots.add(new TimeSnapshot("SquadsLeft pregame"));
                    }
                    //potentially if stream is in 16:10 on twitch it will be squished to 16:9 and mess up where things are on screen
                    if (!isLocalGame) {
                        if (isStreamedSquished == null && streamRatio == null) {
                            final BufferedImage borderedMinimapNormal = screenProcessToImage.getMinimapBorder(gameImage);
                            final BufferedImage borderedMinimap1610Squished = screenProcessToImage.getMinimapBorder1610squish(gameImage);
                            final BufferedImage borderedMinimap43Squished = screenProcessToImage.getMinimapBorder43Squish(gameImage);

                            //logAndDisplay("Attempting to check stream content aspect ratio");
                            if (isMinimapShown(borderedMinimapNormal) && !isMinimapShown(borderedMinimap1610Squished) && !isMinimapShown(borderedMinimap43Squished)) {
                                isStreamedSquished = false;
                                screenProcessToImage.setAspectStreamAspectRatio(AspectRatios.SIXTEEN_BY_NINE);
                                streamRatio = AspectRatios.SIXTEEN_BY_NINE;
                                logAndDisplay("Stream content is in 16:9");
                                gameState = GameStates.INGAME;
                            } else if (isMinimapShown(borderedMinimap1610Squished) && !isMinimapShown(borderedMinimap43Squished)){
                                //when in 16:10 the round timer bar is in teh minimap spot for 16:9 so can't check that
                                isStreamedSquished = true;
                                screenProcessToImage.setAspectStreamAspectRatio(AspectRatios.SIXTEEN_BY_TEN);
                                streamRatio = AspectRatios.SIXTEEN_BY_TEN;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Stream content is in 16:10 (and squished to be shown in 16:9 by stream)");
                            } else if (!isMinimapShown(borderedMinimapNormal) && !isMinimapShown(borderedMinimap1610Squished)  && isMinimapShown(borderedMinimap43Squished)) {
                                isStreamedSquished = true;
                                screenProcessToImage.setAspectStreamAspectRatio(AspectRatios.FOUR_BY_THREE);
                                streamRatio = AspectRatios.FOUR_BY_THREE;
                                gameState = GameStates.INGAME;
                                logAndDisplay("Stream content is in 4:3 (and squished to be shown in 16:9 by stream)");
                            }
                        }
                        timeSnapshots.add(new TimeSnapshot("Ratio check"));
                    }

                    // game enter game loop only once a language is detected and text from in game is detected
                    if (gameState == GameStates.INGAME) {
                        if (isHeadless) {
                            String aspect = "";
                            if (screenProcessToImage != null && screenProcessToImage.streamRatio != null) {
                                if (screenProcessToImage.streamRatio == AspectRatios.SIXTEEN_BY_NINE) {
                                    aspect = " 16:9";
                                } else if (screenProcessToImage.streamRatio == AspectRatios.SIXTEEN_BY_TEN) {
                                    aspect = " 16:10";
                                } else if (screenProcessToImage.streamRatio == AspectRatios.FOUR_BY_THREE) {
                                    aspect = " 4:3";
                                }
                            }
                            String rottationString = "";
                            if (minimapRotation != null) {
                                if (minimapRotation) {
                                    rottationString = " rotate";
                                } else if (!minimapRotation) {
                                    rottationString = " noRotate";
                                }
                            }
                            String consoleOut = streamOrVodText + aspect + rottationString;
                            if (!consoleOut.equals(consoleValue)){
                                ConsoleUtil.updateConsoleStatus(consoleOut);
                                consoleValue = consoleOut;
                            }
                        }
                        if ((mapThread == null && detectedMap != null) && gameIsRunning) {
                            mapThread = new MapThread(webDriverUtil,screenProcessToImage,isLocalGame,circleUtil,robot,screenshotUtil,imageCompare, currentFullColorMap, detectedMap);
                            mapThread.start();
                        }
//                        if (recheckMinimapZoomValues){
//                            recheckMinimapZoomValues = false;
//                        }
//                        if ( loopcount > 3 && allPoints.size() < loopcount/3 && allPoints.size()<8){
//                            if (isStreamedSquished != null) {
//                                isStreamedSquished = null;
//                                screenProcessToImage.streamRatio= AspectRatios.SIXTEEN_BY_NINE;
//                                minimapRotation = false;
//                                playerIsDead = false;
//                                solos = false;
//                                allPoints.clear();
//                            }
//                            recheckMinimapZoomValues = true;
//                            loopcount = 0;
//                            logAndDisplay("No gamepoints found after multiple detection loops, resetting detected stream values");
//                        }

                        if (startTime == null) {
                            startTime = new Date();
                        }



                        //we need the stream aspect ratio if using stream
                        if (isLocalGame || isStreamedSquished != null) {

                            loopcount++;
                            //detect if the minimap on screen
                            Boolean isMinimapShown = false;
                            if (!playerIsDead) {
                                if (isStreamedSquished != null && isStreamedSquished) {

                                    if (streamRatio != null && streamRatio == AspectRatios.FOUR_BY_THREE){
                                        //only use squished if explicitly set for streams where 4:3 is squished to 16:9
                                        final BufferedImage borderedMinimapSquished = screenProcessToImage.getMinimapBorder43Squish(gameImage);
                                        if (isMinimapShown(borderedMinimapSquished)) {
                                            isMinimapShown = true;
                                            gameStateString = "In Game";
                                        }
                                    }else{
                                        //only use squished if explicitly set for streams where 16:10 is squished to 16:9
                                        final BufferedImage borderedMinimapSquished = screenProcessToImage.getMinimapBorder1610squish(gameImage);
                                        if (isMinimapShown(borderedMinimapSquished)) {
                                            isMinimapShown = true;
                                            gameStateString = "In Game";
                                        }
                                    }

                                    timeSnapshots.add(new TimeSnapshot("Get Minimap"));
                                } else {
                                    //minimap is not squished
                                    final BufferedImage borderedMinimap = screenProcessToImage.getMinimapBorder(gameImage);
                                    if (isMinimapShown(borderedMinimap)) {
                                        isMinimapShown = true;
                                        gameStateString = "In Game";
                                    }
                                    timeSnapshots.add(new TimeSnapshot("Get Minimap"));
                                }
                            } else {
                                gameStateString = "Dead";
                                if (isStreamedSquished != null && isStreamedSquished) {
                                    if (streamRatio != null && streamRatio == AspectRatios.FOUR_BY_THREE){
                                        //TODO dead 4:3
                                    }else{
                                        BufferedImage borderedMinimap = screenProcessToImage.getMinimapBorderDead1610Squish(gameImage);
                                        if (isMinimapShown(borderedMinimap)) {
                                            isMinimapShown = true;
                                        }
                                        if (!isMinimapShown) {
                                            borderedMinimap = screenProcessToImage.getMinimapBorder1610squish(gameImage);
                                            if (isMinimapShown(borderedMinimap)) {
                                                logAndDisplay("Player has been respawned");
                                                playerIsDead = false;
                                                gameStateString = "In Game";
                                                isMinimapShown = true;
                                            }
                                        }
                                    }

                                } else {
                                   BufferedImage borderedMinimap = screenProcessToImage.getMinimapBorderDead(gameImage);
                                    if (isMinimapShown(borderedMinimap)) {
                                        isMinimapShown = true;
                                    }
                                    if (!isMinimapShown) {
                                        borderedMinimap = screenProcessToImage.getMinimapBorder(gameImage);
                                        if (isMinimapShown(borderedMinimap)) {
                                            logAndDisplay("Player has been respawned");
                                            playerIsDead = false;
                                            gameStateString = "In Game";
                                            isMinimapShown = true;
                                        }
                                    }
                                    timeSnapshots.add(new TimeSnapshot("Get Minimap"));
                                }
                            }


                            //minimap isn't shown on screen
                            if (!isMinimapShown || (skipCount>= 5 && skipCount%5==0)) {

                                if (!isMinimapShown) {
                                    logAndDisplay("No Minimap shown on screen");
                                    skipCount++;
                                }else if (skipCount>= 5 && skipCount%5==0){
                                    logAndDisplay("Haven't found minimap in last 5 attempts also checking if have missed game ending");
                                    skipCount++;
                                }

                                //is player currently dead
                                final BufferedImage playerDeadText = screenProcessToImage.getDeadStreamHalfScreen(gameImage);
                                if (isPlayerDead(playerDeadText)) {
                                    playerHasDied = true;
                                    if (!playerIsDead) {
                                        playerIsDead = true;
                                        logAndDisplay("Player Died, Will still get zone data while spectating");

                                        //add a 0,0 point which means dead (used for drawing on UI)
                                        ImageMatching im = new ImageMatching();
                                        im.setPoint(new Point(0, 0));
                                        allPoints.add(im);
                                        gameStateString = "Dead";
                                    }

                                    //is the whole team dead
                                    //TODO doesn't work during current season as heading don't change when single dead or whole team
                                    if (isWholeTeamDead(playerDeadText)) {
                                        if (wholeTeamDead == null || !wholeTeamDead) {
                                            wholeTeamDead = true;
                                            logAndDisplay("Whole Team Dead, Will still get zone data while spectating");
                                            gameStateString = "Whole team dead";
                                        }
                                    }
                                }
                                timeSnapshots.add(new TimeSnapshot("Is Player Dead"));

                                //game is in main menu, may have missed game ending screen
                                final String playerDeadTextValue = ocr.basicProcessOcrScale(playerDeadText).replace(" ", "").trim();
                                if (isInMainMenu(playerDeadTextValue)){
                                    logAndDisplay("In Main menu, stop recording, current game, waiting for next");
                                    gameStateString = "In Main Menu";
                                    gameIsRunning = false;
                                }

                                if (isLobby(playerDeadTextValue)){
                                    logAndDisplay("In Lobby, stop recording, current game, waiting for next");
                                    gameStateString = "In Lobby";
                                    gameIsRunning = false;
                                }

                                //Game won, end
                                final BufferedImage playerChampText = screenProcessToImage.getChampion(gameImage);
                                final String playerTextChampValue = ocr.basicProcessOcrScale(playerChampText);
                                if (containsWithVariation(playerTextChampValue, "YOUARETHE")
                                        || containsWithVariation(playerTextChampValue, "YOUARE")
                                        || containsWithVariation(playerTextChampValue, "ARETHE")
                                        || (language == Languages.CHINESE && compareStringsOnlyNeed2CharsMatch(playerTextChampValue, CHS_CHAMPION))) {
                                    if (!wholeTeamDead) {
                                        logAndDisplay("YOU ARE THE CHAMPION");
                                        squadsLeft = 1;
                                    }
                                    logAndDisplay("Game has ended");
                                    gameIsRunning = false;
                                    gameStateString = "In Menu";
                                }

                                //Game won, end
                                final BufferedImage playerChampText2 = screenProcessToImage.getChampion2(gameImage);
                                final String playerTextChampValue2 = ocr.basicProcessOcrScale(playerChampText2);
                                if (containsWithVariation(playerTextChampValue2, "CHAMPION")
                                        || (language == Languages.CHINESE && compareStringsOnlyNeed2CharsMatch(playerTextChampValue2, CHS_CHAMPION))) {
                                    if (!wholeTeamDead) {
                                        logAndDisplay("YOU ARE THE CHAMPION");
                                        squadsLeft = 1;
                                    }
                                    logAndDisplay("Game has ended");
                                    gameIsRunning = false;
                                    gameStateString = "In Menu";
                                }

                                //get is Ranked from champ select screen, if found game must have ended
                                final BufferedImage champSelectMap = screenProcessToImage.getMapFromChampSelect(gameImage);
                                final String mapString = ocr.basicProcessOcrScale(champSelectMap);
                                if (mapString.contains(ocr.screenText(Ocr.TextEnum.RANKED))
                                        || containsWithVariation(mapString, "STORMPOINT")
                                        || containsWithVariation(mapString, "WORLD'SEDGE")
                                        || containsWithVariation(mapString, "BROKENMOON")
                                        || containsWithVariation(mapString, "OLYMPUS")
                                        || containsWithVariation(mapString, "KINGSANYON")
                                        || (language == Languages.CHINESE && checkMapChinese(mapString) != null)) {
                                    logAndDisplay("Detected champ select screen, previous game must have ended");

                                    gameIsRunning = false;
                                    gameStateString = "In Menu";
                                }
                            } else {
                                logAndDisplay("Minimap shown on screen");
                                //read minimap including when dead (minimap is moved down when dead, just used for zones)
                                final BufferedImage minimapReadTMP;
                                if (playerIsDead) {
                                    minimapReadTMP = screenProcessToImage.getMinimapFromWindowGameClientWindowedDead(gameImage);
                                } else {
                                    minimapReadTMP = screenProcessToImage.getMinimapFromWindowGameClientWindowed(gameImage);
                                }
                                //scal image to be square
                                BufferedImage minimapRead = ImageUtil.resizeBufferedImage(minimapReadTMP, minimapReadTMP.getWidth(), minimapReadTMP.getWidth());

//                                minimapToShow = minimapRead;
                                timeSnapshots.add(new TimeSnapshot("Get Minimap"));

                                //need to know map if we are to process data
                                if (detectedMap == null) {
                                    getMapBeingPlayedFromMinimapData(minimapRead, gameImage);
//                                    MapEnum mapBeingPlayed = getMapBeingPlayedFromMinimapData(minimapRead);
//                                    if (mapBeingPlayed != null) {
//                                        logAndDisplay("Detected map being played as: " + mapBeingPlayed.getText());
//                                        detectedMap = mapBeingPlayed;
//                                        currentMapAllowedLocations = ImageUtil.getImageFromResource(detectedMap.getMapAllowedLocationResourceString());
//                                        currentFullColorMap = au.mapbot.apex.util.CustomMaps.load(detectedMap, true);
//                                        if (mapThread != null){
//                                            mapThread.setMap(currentFullColorMap);
//                                        }
//                                    }
                                    timeSnapshots.add(new TimeSnapshot("Get map being played from live game"));
                                }

                                //if we know what map we are on we can process minimap data
                                if (detectedMap != null) {
                                    //Has playerLanded, indicated by ult percentage
                                    if (!hasPlayerLanded) {
                                        final BufferedImage ultChargeImg = screenProcessToImage.getUltPercentageSpotStreamHalfscreen(gameImage);
                                        if (ultChargeImg != null && isUltPercentShown(ultChargeImg)) {
                                            logAndDisplay("Player has landed");
                                            hasPlayerLanded = true;
                                            playerLandedPoint = allPoints.size() - 1;
                                        }
                                        timeSnapshots.add(new TimeSnapshot("Check player landed"));
                                    }

                                    //is possible we don't know if ranked or not yet
                                    if (isRanked == null) {
                                        if (isSquadsLeftTextShownRanked(gameImage)) {
                                            logAndDisplay("Ranked game detected");
                                            isRanked = true;
                                        }
                                        if (isSquadsLeftTextShownUnRanked(gameImage)) {
                                            logAndDisplay("Unranked game detected");
                                            isRanked = false;
                                        }
                                        timeSnapshots.add(new TimeSnapshot("Is Ranked"));
                                    }

                                    //get squads left
                                    Integer squadsLeftValue = null;

                                    //different location depending on language or if player is dead
                                    if (isRanked != null && isRanked) {
                                        squadsLeftValue = getSquadsLeftRanked(gameImage, (playerIsDead && !wholeTeamDead));
                                    } else if (isRanked != null && !isRanked) {
                                        squadsLeftValue = getSquadsLeftUnRanked(gameImage, (playerIsDead && !wholeTeamDead));
                                    }

                                    //XXX added playerIsDead while wholeteamdead doesn't work but should be removed once wholeteamdead can be calculated again
                                    if (squadsLeftValue != null && !wholeTeamDead && !playerIsDead) {
                                        squadsLeftList.add(squadsLeftValue);
                                        //if the last three reads are the same use that value
                                        if (squadsLeftList.size() > 1 && squadsLeftValue != 0 && squadsLeftValue != 1) {
                                            if (Objects.equals(squadsLeftList.get(squadsLeftList.size() - 1), squadsLeftList.get(squadsLeftList.size() - 2))) {
                                                //if the squads left value is different
                                                if (!Objects.equals(squadsLeftList.get(squadsLeftList.size() - 1), squadsLeft)) {
                                                    logAndDisplay("Squads Left changed from: " + squadsLeft + " to " + squadsLeftValue);
                                                    squadsLeft = squadsLeftList.get(squadsLeftList.size() - 1);
                                                }
                                            }
                                        }
                                    }
                                    timeSnapshots.add(new TimeSnapshot("Get SquadsLeft"));


                                    //update round based on time

                                    int firstZoneTime = 90;
                                    long timeSinceStartSeconds = (new Date().getTime() - startTime.getTime()) / 1000;
                                    if (zoom != null && zoom == 1.27 && allPoints.size() < 3 && isRanked != null && !isRanked) {
                                        algs = true;

                                    }
                                    if (algs != null && algs) {
                                        firstZoneTime = 60;
                                    }
                                    int ring1Wait = 90;
                                    int ring1Close = 270;
                                    int ring2Wait = 120;
                                    int ring2Close = 120;
                                    int ring3Wait = 95;
                                    int ring3Close = 60;
                                    int ring4Wait = 95;
                                    int ring4Close = 50;
                                    int ring5Wait = 80;
                                    int ring5Close = 55;
                                    int ring6Wait = 60;
                                    int ring6Close = 120;

                                    //timeminimum to make sure it doesn't move backwards from bad reads
                                    int timeMinimum = 0;
                                    if (timeSinceStartSeconds >= firstZoneTime) {
                                        timeMinimum = 1;
                                        if (currentRound < 1) {
                                            logAndDisplay("Round changed from: " + currentRound + " to 1 (detected by match time)");
                                            currentRound = 1;
                                            isRoundClosing = false;
                                            if(mapThread!=null) {
                                                mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                            }
                                        }
                                        if (timeSinceStartSeconds >= firstZoneTime + ring1Wait && currentRound == 1 && !isRoundClosing) {
                                            isRoundClosing = true;
                                            if(mapThread!=null) {
                                                mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                            }
                                            logAndDisplay("Ring 1 is closing (detected by match time)");
                                        }
                                    }
                                    if (timeSinceStartSeconds >= firstZoneTime + ring1Wait + ring1Close) {
                                        timeMinimum = 2;
                                        if (currentRound < 2) {
                                            logAndDisplay("Round changed from: " + currentRound + " to 2 (detected by match time)");
                                            currentRound = 2;
                                            isRoundClosing = false;
                                            if(mapThread!=null) {
                                                mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                            }
                                        }
                                        if (timeSinceStartSeconds >= firstZoneTime + ring1Wait + ring1Close + ring2Wait && currentRound == 2 && !isRoundClosing) {
                                            isRoundClosing = true;
                                            if(mapThread!=null) {
                                                mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                            }
                                            logAndDisplay("Ring 2 is closing (detected by match time)");
                                        }
                                    }
                                    if (timeSinceStartSeconds >= firstZoneTime + ring1Wait + ring1Close + ring2Wait + ring2Close) {
                                        timeMinimum = 3;
                                        if (currentRound < 3) {
                                            logAndDisplay("Round changed from: " + currentRound + " to 3 (detected by match time)");
                                            currentRound = 3;
                                            isRoundClosing = false;
                                            if(mapThread!=null) {
                                                mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                            }
                                        }
                                        if (timeSinceStartSeconds >= firstZoneTime + ring1Wait + ring1Close + ring2Wait + ring2Close + ring3Wait && currentRound == 3 && !isRoundClosing) {
                                            isRoundClosing = true;
                                            if(mapThread!=null) {
                                                mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                            }
                                            logAndDisplay("Ring 3 is closing (detected by match time)");
                                        }
                                    }
                                    if (timeSinceStartSeconds >= firstZoneTime + ring1Wait + ring1Close + ring2Wait + ring2Close + ring3Wait + ring3Close) {
                                        timeMinimum = 4;
                                        if (currentRound < 4) {
                                            logAndDisplay("Round changed from: " + currentRound + " to 4 (detected by match time)");
                                            currentRound = 4;
                                            isRoundClosing = false;
                                            if(mapThread!=null) {
                                                mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                            }
                                        }
                                        if (timeSinceStartSeconds >= firstZoneTime + ring1Wait + ring1Close + ring2Wait + ring2Close + ring3Wait + ring3Close + ring4Wait && currentRound == 4 && !isRoundClosing) {
                                            isRoundClosing = true;
                                            if(mapThread!=null) {
                                                mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                            }
                                            logAndDisplay("Ring 4 is closing (detected by match time)");
                                        }
                                    }
                                    if (timeSinceStartSeconds >= firstZoneTime + ring1Wait + ring1Close + ring2Wait + ring2Close + ring3Wait + ring3Close + ring4Wait + ring4Close) {
                                        timeMinimum = 5;
                                        if (currentRound < 5) {
                                            logAndDisplay("Round changed from: " + currentRound + " to 5 (detected by match time)");
                                            currentRound = 5;
                                            isRoundClosing = false;
                                            if(mapThread!=null) {
                                                mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                            }
                                        }
                                        if (timeSinceStartSeconds >= firstZoneTime + ring1Wait + ring1Close + ring2Wait + ring2Close + ring3Wait + ring3Close + ring4Wait + ring4Close + ring5Wait && currentRound == 5 && !isRoundClosing) {
                                            isRoundClosing = true;
                                            if(mapThread!=null) {
                                                mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                            }
                                            logAndDisplay("Ring 5 is closing (detected by match time)");
                                        }
                                    }
                                    if (timeSinceStartSeconds > firstZoneTime + ring1Wait + ring1Close + ring2Wait + ring2Close + ring3Wait + ring3Close + ring4Wait + ring4Close + ring5Wait + ring5Close) {
                                        timeMinimum = 6;
                                        if (currentRound < 6) {
                                            logAndDisplay("Round changed from: " + currentRound + " to final round (detected by match time)");
                                            currentRound = 6;
                                            isRoundClosing = false;
                                            if(mapThread!=null) {
                                                mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                            }
                                        }
                                    }
                                    if (timeSinceStartSeconds >= firstZoneTime + ring1Wait + ring1Close + ring2Wait + ring2Close + ring3Wait + ring3Close + ring4Wait + ring4Close + ring5Wait + ring5Close + ring6Wait + ring6Close) {
                                        gameIsRunning = false;
                                        if(mapThread!=null) {
                                            mapThread.updateRound(new RoundStatus(currentRound, isRoundClosing));
                                        }
                                        logAndDisplay("Game must have ended as maximum game length duration exceeded");
                                    }

                                    //get round value
                                    RoundStatus roundValue = getRound(gameImage);
                                    if (roundValue != null) {
                                        //if a round value is found we must have landed if not detected by now
                                        if (hasPlayerLanded == null || !hasPlayerLanded) {
                                            hasPlayerLanded = true;
                                            playerLandedPoint = allPoints.size() - 1;
                                            logAndDisplay("Player has Landed");
                                        }
                                        if (!roundValue.getRound().equals(currentRound)) {
                                            if (roundValue.getRound() > timeMinimum) {
                                                logAndDisplay("Round changed from: " + currentRound + " to " + roundValue.getRound());
                                                currentRound = roundValue.getRound();
                                                if (mapThread != null) {
                                                    mapThread.updateRound(new RoundStatus(currentRound, false));
                                                }
                                            }

                                        }
                                        if (roundValue.getRoundClosing() != isRoundClosing) {
                                            if (roundValue.getRoundClosing()) {
                                                logAndDisplay("Ring is Closing");
                                                if (mapThread != null) {
                                                    mapThread.updateRound(new RoundStatus(currentRound, true));
                                                }
                                            }
                                            isRoundClosing = roundValue.getRoundClosing();

                                        }

                                    }
                                    timeSnapshots.add(new TimeSnapshot("Get Round value"));

                                    //get kills
                                    BufferedImage killStats = screenProcessToImage.getKillSearchAreaLarger(gameImage);
                                    Integer killValueRead = getKills(gameImage, killStats);
                                    //if the kill value is larger
                                    if (killValueRead != null && killValueRead > killPoints.size() && !wholeTeamDead) {
                                        totalKillsListBuffer.add(killValueRead);
                                        //if the last three reads are the same use that value
                                        if (totalKillsListBuffer.size() > 1) {
                                            if (Objects.equals(totalKillsListBuffer.get(totalKillsListBuffer.size() - 1), totalKillsListBuffer.get(totalKillsListBuffer.size() - 2))) {
                                                logAndDisplay("Got " + (killValueRead - killPoints.size()) + " kills, new total kills: " + killValueRead);
                                                if (serverHosted) {
                                                    if (StringUtils.isNotEmpty(streamOrVodText)) {
                                                        WebDriverUtil.TwitchVodInfo tvi = WebDriverUtil.extractTwitchVodInfo(streamOrVodText);
                                                        if (tvi != null) {
                                                            if (tvi.getChannel() != null) {
                                                                String twitchClip = OrchestratorClient.createTwitchClip(tvi.getChannel());
                                                                logAndDisplay("Kill clip:" + twitchClip);
                                                                twitchKillClipList.add(twitchClip);
                                                            }
                                                        } else {
                                                            String twitchClip = OrchestratorClient.createTwitchClip(streamOrVodText);
                                                            logAndDisplay("Kill clip:" + twitchClip);
                                                            twitchKillClipList.add(twitchClip);
                                                        }
                                                    }
                                                }
                                                int currentKills = killPoints.size();
                                                for (int i = 0; i < (killValueRead - currentKills); i++) {
                                                    killPoints.add(allPoints.size() - 1);
                                                }
                                            }
                                        }
                                    }
                                    timeSnapshots.add(new TimeSnapshot("Get Kills"));

                                    //get Location from minimap
                                    ImageMatching location = new ImageMatching();
                                    location.setScore(0d);
                                    location.setPoint(new Point(0,0));

                                    //detect minimap zoom being used
                                    if (zoom == null){
                                        //check zoom level normal
                                        if (minimapRotation == null || !minimapRotation) {
                                            ZoomLevelCheck zlc = getMinimapZoom(minimapRead, detectedMap, currentFullColorMap);
                                            if (zlc.imageMatching.getScore() > 0.4) {
                                                minimapToShow = minimapRead;
                                                zoom = zlc.zoom;
                                                location = zlc.imageMatching;
                                                minimapRotation = false;
                                                logAndDisplay("minimap zoom level detected at: " + zoom);
                                                logAndDisplay("minimap Rotation is off");
                                            }
                                        }
                                        //detect minimap zoom being used with minimap rotation
                                        if (minimapRotation == null || minimapRotation) {
                                            //check with minimap rotation
                                            BufferedImage compassDirection = screenProcessToImage.getDirection(gameImage);
                                            Mat minimapMat = ImageUtil.bufferedImage2Mat(minimapRead);
                                            Mat rotatedMinimap = rotateMinimap(minimapMat, compassDirection, minimapRotation);
                                            if (rotatedMinimap != null) {
                                                minimapToShow = circleUtil.matToBufferedImage(minimapMat);
                                                //TODO this wont work, need to zoom before crop(rotate above)
                                                ZoomLevelCheck zlcRotate = getMinimapZoom(rotatedMinimap, detectedMap, currentFullColorMap, true);
                                                System.out.println("Has checked minimap rotation, score: " + zlcRotate.imageMatching.getScore() + "score to beat: " + location.getScore());
                                                if (zlcRotate.imageMatching.getScore() > 0.4 && zlcRotate.imageMatching.getScore() > location.getScore()) {
                                                    zoom = zlcRotate.zoom;
                                                    location = zlcRotate.imageMatching;
                                                    minimapRotation = true;
                                                    logAndDisplay("minimap zoom level detected at: " + zoom);
                                                    logAndDisplay("minimap Rotation is on");
                                                }
                                            }
                                        }

                                    }else{
                                        // we have a zoom value set
                                        if (location.getScore() == 0){
                                            Mat minimapMat = ImageUtil.bufferedImage2Mat(minimapRead);
                                            boolean useRotate = false;
                                            if (minimapRotation != null && minimapRotation){
                                                BufferedImage compassDirection = screenProcessToImage.getDirection(gameImage);
                                                minimapMat = rotateMinimap(minimapMat, compassDirection, minimapRotation);
                                                useRotate = true;
                                            }
                                            //make sure we have a minimap read (might be null from rotate)
                                            if (minimapMat != null) {
                                                minimapToShow = circleUtil.matToBufferedImage(minimapMat);
                                                Mat currentMapMat = ImageUtil.bufferedImage2Mat(currentFullColorMap);
                                                //use lastPoint in calc?
                                                Point lastPoint = null;
                                                boolean punish = false;
                                                if (!allPoints.isEmpty() && !playerIsDead) {
                                                    lastPoint = allPoints.get(allPoints.size() - 1).getPoint();
                                                    if (skipCount < 10) {
                                                        punish = true;
                                                    }
                                                }

                                                //just respawned as player isn't dead and last known point was 0,0 (deadpoint), don't punish on respawn
                                                if (punish && !playerIsDead){
                                                    if (allPoints.size() > 3){
                                                        if((allPoints.get(allPoints.size()-1).getPoint().x == 0) && (allPoints.get(allPoints.size()-1).getPoint().y == 0)){
                                                            punish = false;
                                                            lastPoint = null;
                                                        }
                                                    }
                                                }

                                                if (currentRound < 5) {
                                                    Mat minimapScaled = getScaledMinimap(detectedMap, minimapMat, zoom);
                                                    location = imageCompare.findInMap(minimapScaled, currentMapMat, lastPoint, useRotate,punish, zonesToUse);
                                                } else if (currentRound == 5) {
                                                    Mat minimapScaled = getScaledMinimap(detectedMap, minimapMat, 0.76);
                                                    location = imageCompare.findInMap(minimapScaled, currentMapMat, lastPoint, useRotate, punish, zonesToUse);
                                                } else if (currentRound == 6) {
                                                    Mat minimapScaled = getScaledMinimap(detectedMap, minimapMat, (0.76 * 0.5));
                                                    location = imageCompare.findInMap(minimapScaled, currentMapMat, lastPoint, useRotate, punish, zonesToUse);
                                                }
                                            }
                                        }

                                        //this it to check that sometimes minimap rotation is missed because if player is facing north it also gets good reads 9ignore if set by GUI)
                                        if (allPoints.size() < 7 && location.getScore() < 0.4 && !minimapRotation && minimapRotationGUI==null){
                                            Mat minimapMat = ImageUtil.bufferedImage2Mat(minimapRead);

                                            BufferedImage compassDirection = screenProcessToImage.getDirection(gameImage);
                                            minimapMat = rotateMinimap(minimapMat, compassDirection, minimapRotation);

                                            //make sure we have a minimap read (might be null from rotate)
                                            if (minimapMat != null) {
                                                Mat currentMapMat = ImageUtil.bufferedImage2Mat(currentFullColorMap);
                                                //use lastPoint in calc?
                                                Point lastPoint = null;
                                                boolean punish = false;

                                                if (!allPoints.isEmpty() && !playerIsDead) {
                                                    lastPoint = allPoints.get(allPoints.size() - 1).getPoint();
                                                    if (skipCount < 10) {
                                                        punish = true;
                                                    }
                                                }
                                                //just respawned as player isn't dead and last known point was 0,0 (deadpoint), don't punish on respawn
                                                if (punish && !playerIsDead){
                                                    if (allPoints.size() > 3) {
                                                        if ((allPoints.get(allPoints.size() - 1).getPoint().x == 0) && (allPoints.get(allPoints.size() - 1).getPoint().y == 0)) {
                                                            punish = false;
                                                            lastPoint = null;
                                                        }
                                                    }
                                                }

                                                Mat minimapScaled = getScaledMinimap(detectedMap, minimapMat, zoom);
                                                ImageMatching rotateLocation = imageCompare.findInMap(minimapScaled, currentMapMat, lastPoint, true, punish, zonesToUse);
                                                if (rotateLocation.getScore() > 0.6){
                                                    if (isPointInsideAllowedMapLocations(rotateLocation, currentMapAllowedLocations)) {
                                                        location =  rotateLocation;
                                                        minimapRotation = true;
                                                        logAndDisplay("minimap zoom level detected at: " + zoom);
                                                        logAndDisplay("minimap Rotation is on");
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    //check map thread for ful map values and beacon/rc values
                                    if (mapThread != null){

                                        evoPoints = mapThread.getEvoPoints();
                                        beaconPoints = mapThread.getBeaconPoints();
                                        rcPoints = mapThread.getRcPoints();

                                        allDetectedFullMapCircles = mapThread.getAllDetectedFullMapZones();
                                        List<DetectedCircle> allCircles = new ArrayList<>();
                                        allCircles.addAll(allDetectedCircles);
                                        allCircles.addAll(allDetectedFullMapCircles);
                                        List<DetectedCircle> activeZoneNew = getActiveZones(currentRound, allCircles);
                                        //see if any new zone data is present
                                        if (!activeZoneNew.isEmpty()) {
                                            if (!zonesToUse.isEmpty()) {
                                                int i = 0;
                                                for (DetectedCircle dz : activeZoneNew) {
                                                    for (DetectedCircle az : zonesToUse) {
                                                        if (Math.floor(dz.getCenter().x) == Math.floor(az.getCenter().x)
                                                                && Math.floor(dz.getCenter().y) == Math.floor(az.getCenter().y)) {
                                                            i++;
                                                        }
                                                    }
                                                }
                                                if (i < activeZoneNew.size()) {
                                                    String out = "";
                                                    for (DetectedCircle dz : activeZoneNew) {
                                                        out = out + "New Zone Center(x" + dz.getCenter().x + ", y" + dz.getCenter().y + ") ";
                                                    }
                                                    for (DetectedCircle az : zonesToUse) {
                                                        out = out + "Previous Zone Center(x" + az.getCenter().x + ", y" + az.getCenter().y + ") ";
                                                    }
                                                    logAndDisplay("Zone Data Updated from Map Data: " + out);
                                                    zonesToUse = activeZoneNew;
                                                }
                                            } else {
                                                logAndDisplay("Map Zone Data Found");
                                                zonesToUse = activeZoneNew;
                                            }
                                        }
                                    }

                                    //just respawned as player isn't dead and last known point was 0,0 (deadpoint)
                                    boolean justRespawned = false;
                                    if (!playerIsDead){
                                        if (allPoints.size() > 3) {
                                            if ((allPoints.get(allPoints.size() - 1).getPoint().x == 0) && (allPoints.get(allPoints.size() - 1).getPoint().y == 0)) {
                                                justRespawned = true;
                                            }
                                        }
                                    }

                                    //updated points for player location from minimap matching
                                    if ((location.getScore() > 0.38 && !justRespawned) || //normal point cutoff
                                            (location.getScore() > 0.32 && currentRound >= 4 ) || //lower normal cutoff once round 4 as more clutter
                                            (location.getScore() > 0.45 && justRespawned) ) { //if player just respawned need higher first result
                                        if (isPointInsideAllowedMapLocations(location, currentMapAllowedLocations)) {
                                            if (withinAllowedDistance(location)) {
                                                skipCount = 0;
                                                if (!playerIsDead) {
                                                    logAndDisplay("Player location detected: " + location.getPoint().x + "," + location.getPoint().y + " with confidence score: " + location.getScore());
                                                    allPoints.add(location);
                                                } else {
                                                    logAndDisplay("Spectating Player location detected: " + location.getPoint().x + "," + location.getPoint().y + " with confidence score: " + location.getScore());
                                                    deadPoints.add(location);
                                                }

                                                //check dropship lines
                                                //the ping suggest launch text that appears when players are now in dropship, pregame has ended
                                                if (!hasPlayerLanded && inDropshipPoints.size() < 2) {
                                                    if (language == Languages.JAPANESE) {
                                                        BufferedImage dropImg = screenProcessToImage.getDropTextFromWindowStreamJP(gameImage);
                                                        if (inShipCheckJapanese(dropImg)) {
                                                            inDropshipPoints.add(location);
                                                        }
                                                    } else if (language == Languages.CHINESE) {
                                                        BufferedImage dropImg = screenProcessToImage.getDropTextFromWindowStreamJP(gameImage);
                                                        if (inShipCheckChinese(dropImg)) {
                                                            inDropshipPoints.add(location);
                                                        }
                                                    } else {
                                                        BufferedImage dropImg = screenProcessToImage.getDropTextFromWindowStream(gameImage);
                                                        if (inShipCheckEnglish(dropImg)) {
                                                            inDropshipPoints.add(location);
                                                        }
                                                    }
                                                } else {
                                                    if (dropshipLine == null && inDropshipPoints.size() >= 2) {
                                                        dropshipLine = LineUtil.extendLineToEdges(inDropshipPoints.get(0).getPoint(), inDropshipPoints.get(1).getPoint());
                                                    }
                                                }

                                                //also check if zones are in minimap data only if point is updated as we need to know location to get minimap zones
                                                if (currentRound != null && currentRound > 0) {
                                                    //don't need to get from minimap if we already have a perfect zone pull from fullmap to save compute time
                                                    boolean getMinimapZonesIfNoFullmapZOnes = true;

                                                    // when "round 1" only 1 circle is shown
                                                    // when "round 1 closing" only 1 circle is shown
                                                    // when "round 2" 1 and 2 circle is shown (true for all zones after)
                                                    // when "round 2 closing" only 2 circle is shown (true for all zones after)
                                                    if ((allDetectedFullMapCircles.stream().filter(s -> s.getZone() == currentRound).anyMatch(DetectedCircle::isPerfect) && currentRound == 1)
                                                        ||(!isRoundClosing && allDetectedFullMapCircles.stream().filter(s -> s.getZone() == currentRound - 1).anyMatch(DetectedCircle::isPerfect) && allDetectedFullMapCircles.stream().filter(s -> s.getZone() == currentRound).anyMatch(DetectedCircle::isPerfect)) // round 1
                                                        || ((isRoundClosing && allDetectedFullMapCircles.stream().filter(s -> s.getZone() == currentRound).anyMatch(DetectedCircle::isPerfect)))) {
                                                        getMinimapZonesIfNoFullmapZOnes = false;
                                                        logAndDisplay("100% sure of zone location, no need to calculate");
                                                    }
                                                    if (getMinimapZonesIfNoFullmapZOnes){
                                                        //get zones from detected on screen + all previous
                                                        List<DetectedCircle> circlesFromMinimap = getCirclesFromMinimap(location.getPoint(), location.getMat(), currentRound, isRoundClosing);
                                                        if (!circlesFromMinimap.isEmpty()) {
                                                            allDetectedCircles.addAll(circlesFromMinimap);
                                                            List<DetectedCircle> allCircles = new ArrayList<>();
                                                            allCircles.addAll(allDetectedCircles);
                                                            allCircles.addAll(allDetectedFullMapCircles);
                                                            List<DetectedCircle> activeZoneNew = getActiveZones(currentRound, allCircles);
                                                            //see if any new zone data is present
                                                            if (!activeZoneNew.isEmpty()) {
                                                                if (!zonesToUse.isEmpty()) {
                                                                    int i = 0;
                                                                    for (DetectedCircle dz : activeZoneNew) {
                                                                        for (DetectedCircle az : zonesToUse) {
                                                                            if (Math.floor(dz.getCenter().x) == Math.floor(az.getCenter().x)
                                                                                    && Math.floor(dz.getCenter().y) == Math.floor(az.getCenter().y)) {
                                                                                i++;
                                                                            }
                                                                        }
                                                                    }
                                                                    if (i < activeZoneNew.size()) {
                                                                        String out = "";
                                                                        for (DetectedCircle dz : activeZoneNew) {
                                                                            out = out + "New Zone Center(x" + dz.getCenter().x + ", y" + dz.getCenter().y + ") ";
                                                                        }
                                                                        for (DetectedCircle az : zonesToUse) {
                                                                            out = out + "Previous Zone Center(x" + az.getCenter().x + ", y" + az.getCenter().y + ") ";
                                                                        }
                                                                        logAndDisplay("Zone Data Updated from MiniMap Data: " + out);
                                                                        zonesToUse = activeZoneNew;
                                                                    }
                                                                } else {
                                                                    logAndDisplay("Minimap Zone Data Found");
                                                                    zonesToUse = activeZoneNew;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                boolean stillUse = false;
                                                if (!playerIsDead) {
                                                    if (allPoints.size() > 3) {
                                                        if ((allPoints.get(allPoints.size() - 2).getPoint().x == 0)
                                                                && (allPoints.get(allPoints.size() - 2).getPoint().y == 0)
                                                                && location.getScore() - allPoints.get(allPoints.size() - 1).getScore() > 0.15) {
                                                            allPoints.remove(allPoints.size() - 1);
                                                            allPoints.add(location);
                                                            stillUse = true;
                                                            logAndDisplay("Changing player spawn location due to better data(" + location.getPoint().x + "," + location.getPoint().y + "), Score:" + location.getScore());
                                                        }
                                                    }
                                                }
                                                if (!stillUse) {
                                                    skipCount++;
                                                    logAndDisplay("Low confidence, not using data point (" + location.getPoint().x + "," + location.getPoint().y + ") (location to far from previous point), Score:" + location.getScore());
                                                }
                                            }

                                            //remove point outliers, in same line as adding points
                                            if (allPoints.size() >= 3) {
                                                removePointOutliers();
                                            }
                                        } else {
                                            skipCount++;
                                            logAndDisplay("Low confidence, Not using data point (" + location.getPoint().x + "," + location.getPoint().y + ") (location outside map bounds), Score:" + location.getScore());
                                        }
                                        timeSnapshots.add(new TimeSnapshot("Set Location Points"));
                                    }else{
                                        skipCount++;
                                        logAndDisplay("Low confidence, Not using data point (" + location.getPoint().x + "," + location.getPoint().y + ") (bads score), Score:" + location.getScore());
                                    }
                                }else{
                                    logAndDisplay("Map being played unknown, cannot get location until map recognised");
                                }
                            }
                        }
                    }
                }else{
                    logAndDisplay("Error, minimap Image is null, cannot get minimap data from game/stream");
                }

                //Write log of time taken per section
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < timeSnapshots.size(); i++) {
                    sb.append(timeSnapshots.get(i).name + ": " + (timeSnapshots.get(i).currentTIme - timeSnapshots.get(i - 1).currentTIme) + " | ");
                }
//                logAndDisplay(sb.toString());
            }


            //saving in automatic rollover of games
            String gameType = "";
            if (gameTypeGUI == null) {
                if (solos) {
                    gameType = "Unranked Solo's";
                } else if (algs != null && algs) {
                    gameType = "ALGS/Customs";
                } else if (isRanked != null && isRanked) {
                    gameType = "Ranked";
                } else if (isRanked != null && !isRanked) {
                    gameType = "Unranked";
                }
            }else{
                gameType = gameTypeGUI;
            }

            //individual game has ended, (gameIsRunning is false) so exited normal loop, reset it to start capturing next game
            logAndDisplay("saving completed game");
            save(allPoints, startTime, streamOrVodText, detectedMap, List.copyOf(zonesToUse), playerLandedPoint, killPoints, squadsLeft, beaconPoints, rcPoints, evoPoints, twitchKillClipList, gameType, dropshipLine);
            saved = true;
            logAndDisplay("Game complete, waiting for next game");
        }

        //second save call exactly wame as above but used when manually stopped
        if (!saved){
            String gameType = "";
            if (gameTypeGUI == null) {
                if (solos) {
                    gameType = "Unranked Solo's";
                } else if (algs != null && algs) {
                    gameType = "ALGS/Customs";
                } else if (isRanked != null && isRanked) {
                    gameType = "Ranked";
                } else if (isRanked != null && !isRanked) {
                    gameType = "Unranked";
                }
            }else{
                gameType = gameTypeGUI;
            }

            //individual game has ended, (gameIsRunning is false) so exited normal loop, reset it to start capturing next game
            logAndDisplay("Saving Game");
            save(allPoints, startTime, streamOrVodText, detectedMap, List.copyOf(zonesToUse), playerLandedPoint, killPoints, squadsLeft, beaconPoints, rcPoints, evoPoints, twitchKillClipList, gameType, dropshipLine);
            saved = true;
            logAndDisplay("Saving Complete");
        }

        //capture has ended close the webdriver if capturing a vod/stream
        if (webDriverUtil != null){
            logAndDisplay("Closing Chrome webdriver");
            webDriverUtil.shutdown();
        }
//        if (fFmpegCapture != null) {
//            fFmpegCapture.close();
//        }
        if (mapThread != null && mapThread.isAlive()) {
            logAndDisplay("Closing Chrome mapThread");
            mapThread.end();  // Custom method to signal the thread to end
            try {
                mapThread.join(5000); // Wait for the thread to finish
                if (mapThread.isAlive()) {
                    mapThread.interrupt(); // Force stop if it's still running
                    mapThread.join(); // Ensure it stops before proceeding
                }
            } catch (InterruptedException e) {
                //
            }
        }
        logAndDisplay("Ended main process shutting down");
    }

    public Integer getKills(BufferedImage gameImage, BufferedImage killStatsLarger) throws IOException {
        if (!playerIsDead) {

            Mat skullImageMat = ImageUtil.bufferedImage2Mat(skull);
            Mat skullLeaderImageMat = ImageUtil.bufferedImage2Mat(skullLeader);
//            ImageCompare ic = new ImageCompare();

            double scaleAmount = gameImage.getWidth() / 1210d;
            double scaleWidth = skullImageMat.width() * scaleAmount;
            double scaleHeight = killStatsLarger.getHeight();

            double scaleAmountLeader = gameImage.getWidth() / 1600d;
            double scaleWidthLeader = skullLeaderImageMat.width() * scaleAmountLeader;
            double scaleHeightLeader = killStatsLarger.getHeight();

            skullImageMat = ImageUtil.resizeMat(skullImageMat, scaleWidth, scaleHeight);
            skullLeaderImageMat = ImageUtil.resizeMat(skullLeaderImageMat, scaleWidthLeader, scaleHeightLeader);

            ImageMatching im = imageCompare.findInMapBasic(skullImageMat, ImageUtil.bufferedImage2Mat(killStatsLarger));
            ImageMatching im2 = imageCompare.findInMapBasic(skullLeaderImageMat, ImageUtil.bufferedImage2Mat(killStatsLarger));
            if (im2.getScore() > im.getScore()) {
                im = im2;
            }
            int subimageLeft = Double.valueOf(im.getPoint().x + im.getMat().width()).intValue();
            int subImageWidth = killStatsLarger.getWidth() / 18;

            if (im.getScore() > 0.55 && (subimageLeft + subImageWidth < killStatsLarger.getWidth())) {
                BufferedImage killGrab = killStatsLarger.getSubimage(subimageLeft, 0, subImageWidth, killStatsLarger.getHeight());
                OCRMatch match = ocr.ocrSingle(killGrab, killPoints.size(), false, 150, 180);
                if (match.getConfidence() > 80) {
                    try {
                        Integer killValue = Integer.parseInt(match.getResult());
                        return killValue;
                    } catch (NumberFormatException ex) {

                    }
                }
            }
        }
        return null;
    }

    public SaveFile save(final List<ImageMatching> points,
                         final Date matchStart,
                         final String inputScreenText,
                         final MapEnum currentMapEnum,
                         final List<DetectedCircle> useZones,
                         final Integer landpoint,
                         final List<Integer> kills,
                         final int squadsLeft,
                         final Set<Point> beaconPoints,
                         final Set<Point> rcPoints,
                         final Set<Point> evoPoints,
                         final List<String> killClips,
                         final String gameType,
                         final Line dropshipLine) throws IOException {
        if (points.size() > 2 && currentMapEnum != null) {

            logAndDisplay("Saving game results to file");
            //game ended and player didn't come first so must have died, if not already picked up
            if (squadsLeft > 1){
                if (allPoints.get(allPoints.size()-1).getPoint().x != 0 && allPoints.get(allPoints.size()-1).getPoint().x != 0) {
                    //add a 0,0 point which means dead (used for drawing on UI)
                    ImageMatching im = new ImageMatching();
                    im.setPoint(new Point(0, 0));
                    allPoints.add(im);
                }
            }

            Date matchEnd = new Date();
            final long matchLength = matchEnd.getTime() - matchStart.getTime();
            final SaveFile saveFile = new SaveFile();
            saveFile.setDate(matchStart);
            if (!inputScreenText.isEmpty()) {
                WebDriverUtil.TwitchVodInfo tvi = WebDriverUtil.extractTwitchVodInfo(inputScreenText);
                saveFile.setStreamName(tvi.getString());
            }
            saveFile.setDurationMilliseconds(matchLength);

            List<PointList> pointList = new ArrayList<>();
            for (ImageMatching im : points){
                PointList pl = new PointList();
                pl.setPoint(im.getPoint());
                pointList.add(pl);
            }
            saveFile.setPointsList(pointList);
            saveFile.setMap(currentMapEnum);
            saveFile.setKillList(kills);
            saveFile.setPlacement(squadsLeft);
            saveFile.setIndex(0);
            saveFile.setCircleList(CircleUtil.adjustCircles(useZones));
            saveFile.setLanded(landpoint);
            saveFile.setBeaconsPoints(beaconPoints);
            saveFile.setEvoPoints(evoPoints);
            saveFile.setRcPoints(rcPoints);
            saveFile.setKillClipList(killClips);
            saveFile.setGameType(gameType);
            saveFile.setDropship(dropshipLine);

            if (serverHosted) {
                OrchestratorClient.saveToOrchestrator(saveFile);
            }
            SaveFileUtil.saveToFile(saveFile);
            return saveFile;
        }
        return null;
    }

    public boolean withinAllowedDistance(ImageMatching matchLoc){

        List<ImageMatching> pointsListToUse;
        if (!playerIsDead){
            pointsListToUse = allPoints;
        }else{
            pointsListToUse = deadPoints;
        }
        if (pointsListToUse.isEmpty()){
            return true;
        }
        //dead point so maximum allowed distance
        if (pointsListToUse.get(pointsListToUse.size() - 1).getPoint().x == 0 && pointsListToUse.get(pointsListToUse.size() - 1).getPoint().y == 0){
            return true;
        }

        double xDistance = 0;
        double yDistance = 0;
        if (!pointsListToUse.isEmpty()) {
            xDistance = Math.abs(matchLoc.getPoint().x - pointsListToUse.get(pointsListToUse.size() - 1).getPoint().x);
            yDistance = Math.abs(matchLoc.getPoint().y - pointsListToUse.get(pointsListToUse.size() - 1).getPoint().y);
        }

        int distance = 150;

        //lower max distance in final rounds
        if (currentRound < 5 ){
            distance = 350;
        }
        if (playerIsDead || skipCount > 10){
            distance = 500;
        }
        if (!hasPlayerLanded || skipCount > 20){
            distance = 1000;
        }

        if ((xDistance > distance || yDistance > distance)) {
            return false;
        }
        return true;
    }

    public void removePointOutliers(){
        final double xDistance12 = Math.abs(allPoints.get(allPoints.size() - 1).getPoint().x - allPoints.get(allPoints.size() - 2).getPoint().x);
        final double yDistance12 = Math.abs(allPoints.get(allPoints.size() - 1).getPoint().y - allPoints.get(allPoints.size() - 2).getPoint().y);

        final double xDistance23 = Math.abs(allPoints.get(allPoints.size() - 2).getPoint().x - allPoints.get(allPoints.size() - 3).getPoint().x);
        final double yDistance23 = Math.abs(allPoints.get(allPoints.size() - 2).getPoint().y - allPoints.get(allPoints.size() - 3).getPoint().y);

        final double xDistance13 = Math.abs(allPoints.get(allPoints.size() - 1).getPoint().x - allPoints.get(allPoints.size() - 3).getPoint().x);
        final double yDistance13 = Math.abs(allPoints.get(allPoints.size() - 1).getPoint().y - allPoints.get(allPoints.size() - 3).getPoint().y);

        if (xDistance13 > 5){
            if (((xDistance12 >= xDistance13*2) && (xDistance23 >= xDistance13*2))) {
                if ((allPoints.get(allPoints.size() - 2).getPoint().x != 0) && (allPoints.get(allPoints.size() - 2).getPoint().y != 0)) {
                    logAndDisplay("Outlier point removed");
                    allPoints.remove(allPoints.size() - 2);
                }
            } else if (((yDistance12 >= yDistance13*2) && (yDistance23 >= yDistance13*2))) {
                if ((allPoints.get(allPoints.size() - 2).getPoint().x != 0) && (allPoints.get(allPoints.size() - 2).getPoint().y != 0)) {
                    allPoints.remove(allPoints.size() - 2);
                    logAndDisplay("Outlier point removed");
                }
            }
        }
    }

    public List<DetectedCircle> getCirclesFromMinimap(final Point location, final Mat minimapMat, int currentRound, boolean isRoundClosing) {
        List<DetectedCircle> newCircles = circleUtil.getCirclesInMinimap(minimapMat, currentRound, isRoundClosing);

        List<DetectedCircle> returnCirclesNormalised = new ArrayList<>();
        if (!newCircles.isEmpty()) {
//            Point currentPoint = allPoints.get(allPoints.size()-1).getPoint();
            returnCirclesNormalised = circleUtil.updateList(location.x, location.y, minimapMat, newCircles, returnCirclesNormalised, currentMapAllowedLocations);
        }
        return returnCirclesNormalised;
    }

    public Integer getKill(final BufferedImage killStats) throws IOException {

        BufferedImage killStatBlackWhite =  killStats;
        boolean getNext = true;
        int count = 0;
        int hitId = 0;
        int hitIdWidth = 0;
        int missCount = 0;
        boolean hasHit = false;

        //awkward but don't have better working solution
        //gets an area for where the kill feed it, then look for the skull
        // and then get the number after that
        //width of space
        int averageBrightness = ocr.computeHalfwayBrightness(killStatBlackWhite);
//                    System.out.println("brightness:" + averageBrightness);
        for (int i = 0; i != killStatBlackWhite.getWidth() && getNext; i++) {
            int foundCount = 0;
            for (int j = 0; j < killStatBlackWhite.getHeight(); j++) {
                int pixelTest = killStatBlackWhite.getRGB(i, j);
                if (rgbToGray(pixelTest, MODE.LIGHTNESS) >= averageBrightness) {
                    foundCount++;
                }
            }
            if (foundCount>=killStatBlackWhite.getHeight()/3) {
                //white (could be numbers or skull)
                count++;
                hasHit = true;
                missCount = 0;

            } else {
                missCount++;
                if (count> 6){
                    if (hitId == 0){
                        //this it the end of the skull icon
                        hitId = i + 1;
                        hasHit = false;
                    }
                }
                if (hasHit){
                    if (hitId > 0) {
                        if (missCount > 3) {
                            //gap after number
                            hitIdWidth = i - hitId + 1;
                            getNext = false;
                        }
                    }
                }
                count = 0;
            }
        }

        if (hitIdWidth > 0) {
            BufferedImage killGrab = killStatBlackWhite.getSubimage(hitId, 0, hitIdWidth, killStatBlackWhite.getHeight());

            OCRMatch match = ocr.ocrSingle(killGrab, killPoints.size(), false, 150, 180);

            //this was previously 60 because of single reads, now that we get multiple reads can increase values
            if (match.getConfidence() > 80) {
                try {
                    int killValue = Integer.parseInt(match.getResult());
                    return killValue;
                } catch (NumberFormatException ex) {

                }
            }
        }
        return null;
    }

    public boolean isPointInsideAllowedMapLocations(final ImageMatching matchLoc, BufferedImage mapBW){
        int xValue = Double.valueOf(matchLoc.getPoint().x).intValue();
        int yValue = Double.valueOf(matchLoc.getPoint().y).intValue();

        if (xValue >= mapBW.getWidth() || yValue >= mapBW.getHeight() || xValue <= 0 || yValue <= 0){
            return false;
        }

        Color wePixelColor = new Color(mapBW.getRGB(xValue, yValue));
        if (wePixelColor.equals(Color.BLACK)) {
            return true;
        }
        return false;
    }


    public Mat rotateMinimap(final Mat input, final BufferedImage direction, Boolean allowZero) throws IOException {
        int brightness = ocr.computeHalfwayBrightness(direction);
        OCRMatch directionOcr = ocr.ocrSingle(direction, 0, false, brightness - 5, brightness + 5);

        try {
            System.out.println("rotation value read:" + directionOcr.getResult() + " | confidence:"+ directionOcr.getConfidence() + " | threshold: " + directionOcr.getThresholdFound());
            SnapshotFromImage.writeImageToFile(circleUtil.matToBufferedImage(input), "rotation value");
            int degree = Integer.parseInt(directionOcr.getResult());
            if ((degree > 5 && degree < 355) || (allowZero != null && allowZero)) {
                CircleUtil cu = new CircleUtil();
                BufferedImage inputBuff = cu.matToBufferedImage(input);
                BufferedImage rotateBuff = ImageUtil.rotateAndCrop(inputBuff, degree);
                return ImageUtil.bufferedImage2Mat(rotateBuff);
            }
        }catch (Exception exception){

        }
        return null;
    }

    public Mat rotateMinimap(final Mat input, final BufferedImage direction) throws IOException {
       return rotateMinimap(input,direction, null);
    }


    public class ZoomLevelCheck{
        public ImageMatching imageMatching;
        public Double zoom;
        public ZoomLevelCheck(final ImageMatching im, final double zoom){
            this.imageMatching = im;
            this.zoom = zoom;
        }
    }

    public ZoomLevelCheck getMinimapZoom(final Mat minimapBaseMat, final MapEnum currentMapEnum, BufferedImage fullMap, boolean isRotated) throws IOException {
        Mat currentMapMat = ImageUtil.bufferedImage2Mat(fullMap);

        ImageMatching matchLoc = new ImageMatching();
        matchLoc.setScore((double) 0);

        Mat minimapNoScale = getScaledMinimap(currentMapEnum,minimapBaseMat,1d);
        Mat minimapAlgsScale = getScaledMinimap(currentMapEnum,minimapBaseMat,1.27d);
        Mat minimapFurtherZoomScale = getScaledMinimap(currentMapEnum,minimapBaseMat,1.4d);

        if (currentMapAllowedLocations == null){
            currentMapAllowedLocations = ImageUtil.getImageFromResource(currentMapEnum.getMapAllowedLocationResourceString());
        }

        ImageMatching im = imageCompare.findInMap(minimapNoScale, currentMapMat, null, isRotated, false, zonesToUse);
        if (!isPointInsideAllowedMapLocations(im, currentMapAllowedLocations)){
            im.setScore(0d);
        }
        ImageMatching im2 = imageCompare.findInMap(minimapAlgsScale, currentMapMat, null, isRotated, false, zonesToUse);
        if (!isPointInsideAllowedMapLocations(im2, currentMapAllowedLocations)){
            im2.setScore(0d);
        }
        ImageMatching im3 = imageCompare.findInMap(minimapFurtherZoomScale, currentMapMat, null, isRotated, false, zonesToUse);
        if (!isPointInsideAllowedMapLocations(im3, currentMapAllowedLocations)){
            im3.setScore(0d);
        }

        if (im.getScore() > im2.getScore() && im.getScore() > im3.getScore()){
            return new ZoomLevelCheck(im,1d);
        }
        if (im2.getScore() > im.getScore() && im2.getScore() > im3.getScore()){
            return new ZoomLevelCheck(im2,1.27d);
        }
        return new ZoomLevelCheck(im3, 1.4);
    }
    public ZoomLevelCheck getMinimapZoom(final BufferedImage minimapRead, final MapEnum currentMapEnum, BufferedImage fullMap) throws IOException {
        Mat minimapBaseMat = ImageUtil.bufferedImage2Mat(minimapRead);
        return getMinimapZoom(minimapBaseMat, currentMapEnum, fullMap, false);

    }
    public Mat getScaledMinimap(MapEnum currentMapEnum, Mat minimapBaseMat, Double zoomScale){
        final int olympusScaleResolution = 313;
        final int stormPointScaleResolution = 274;
        final int worldsEdgeScaleResolution = 315;
        final int brokenMoonScaleResolution = 332;
        final int kingsCanyonScaleResolution = 340;
        final int districtScaleResolution = 330;

        if (MapEnum.STORMPOINT.equals(currentMapEnum)) {
            //313*1.27=397.51
            final int scaleValue = Double.valueOf(stormPointScaleResolution * zoomScale).intValue();
            return ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
        } else if (MapEnum.STORMPOINT_19.equals(currentMapEnum)) {
            //274*1.27=347.98
            final int scaleValue = Double.valueOf(stormPointScaleResolution * zoomScale).intValue();
            return ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
        } else if (MapEnum.OLYMPUS.equals(currentMapEnum)) {
            //274*1.27=347.98
            final int scaleValue = Double.valueOf(olympusScaleResolution * zoomScale).intValue();
            return ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
        } else if (MapEnum.WORLDSEDGE.equals(currentMapEnum)) {
            //315*1.27=400.05
            final int scaleValue = Double.valueOf(worldsEdgeScaleResolution * zoomScale).intValue();
            return ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
        } else if (MapEnum.BROKENMOON.equals(currentMapEnum)) {
            //330*1.27=419.1
            final int scaleValue = Double.valueOf(brokenMoonScaleResolution * zoomScale).intValue();
            return ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
        } else if (MapEnum.KINGSCANYON.equals(currentMapEnum)) {
            //340*1.27=431.8
            final int scaleValue = Double.valueOf(kingsCanyonScaleResolution * zoomScale).intValue();
            return ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
        } else if (MapEnum.DISTRICT.equals(currentMapEnum)) {
            //340*1.27=431.8
            final int scaleValue = Double.valueOf(districtScaleResolution * zoomScale).intValue();
            return ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
        }
        return null;
    }

 /*   public ImageMatching getMinimapLocation(final BufferedImage minimapRead, final int round, final MapEnum currentMapEnum, final BufferedImage directionImageFOrRotate, BufferedImage fullMap) throws IOException {
        Mat minimapBaseMat= ImageUtil.bufferedImage2Mat(minimapRead);
        Mat minimapMat = ImageUtil.bufferedImage2Mat(minimapRead);
        Mat currentMapMat = ImageUtil.bufferedImage2Mat(fullMap);

        //round 1 values
        final int olympusScaleResolution = 313;
        final int stormPointScaleResolution = 274;
        final int worldsEdgeScaleResolution = 315;
        final int brokenMoonScaleResolution = 332;
        final int kingsCanyonScaleResolution = 340;

        ImageMatching matchLoc = new ImageMatching();
        matchLoc.setScore((double) 0);


        //Default image matching for zones 1,2,3,4
        if (round == 0 || round == 1 || round == 2 || round == 3 || round == 4) {
            if (playerHasDied && !solos) {
                if (MapEnum.STORMPOINT.equals(currentMapEnum)) {
                    //313*1.27=397.51
                    final int scaleValue = Double.valueOf(stormPointScaleResolution * 1.27).intValue();
                    minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
                } else if (MapEnum.STORMPOINT_19.equals(currentMapEnum)) {
                    //274*1.27=347.98
                    final int scaleValue = Double.valueOf(stormPointScaleResolution * 1.27).intValue();
                    minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
                } else if (MapEnum.OLYMPUS.equals(currentMapEnum)) {
                    //274*1.27=347.98
                    final int scaleValue = Double.valueOf(olympusScaleResolution * 1.27).intValue();
                    minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
                } else if (MapEnum.WORLDSEDGE.equals(currentMapEnum)) {
                    //315*1.27=400.05
                    final int scaleValue = Double.valueOf(worldsEdgeScaleResolution * 1.27).intValue();
                    minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
                } else if (MapEnum.BROKENMOON.equals(currentMapEnum)) {
                    //330*1.27=419.1
                    final int scaleValue = Double.valueOf(brokenMoonScaleResolution * 1.28).intValue();
                    minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
                } else if (MapEnum.KINGSCANYON.equals(currentMapEnum)) {
                    //340*1.27=431.8
                    final int scaleValue = Double.valueOf(kingsCanyonScaleResolution * 1.27).intValue();
                    minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
                }

                if (directionImageFOrRotate != null){
                    minimapMat = rotateMinimap(minimapMat, directionImageFOrRotate);
                    if (minimapMat == null){
                        ImageMatching im = new ImageMatching();
                        im.setScore(0d);
                        return im;
                    }
                }
                matchLoc = imageCompare.findInMap(minimapMat, currentMapMat, allPoints.isEmpty()||playerIsDead||skipCount>1?null:allPoints.get(allPoints.size()-1).getPoint());
            }
            if ((!playerHasDied || solos) || (playerHasDied && playerIsDead && matchLoc.getScore() < 0.38)) {
                if (MapEnum.OLYMPUS.equals(currentMapEnum)) {
                    minimapMat = ImageUtil.resizeMat(minimapBaseMat, olympusScaleResolution, olympusScaleResolution);
                } else if (MapEnum.STORMPOINT.equals(currentMapEnum)) {
                    minimapMat = ImageUtil.resizeMat(minimapBaseMat, stormPointScaleResolution, stormPointScaleResolution);
                } else if (MapEnum.STORMPOINT_19.equals(currentMapEnum)) {
                    minimapMat = ImageUtil.resizeMat(minimapBaseMat, stormPointScaleResolution, stormPointScaleResolution);
                } else if (MapEnum.WORLDSEDGE.equals(currentMapEnum)) {
                    if (solos){
                        final int scaleValue = Double.valueOf(worldsEdgeScaleResolution * 1.4).intValue();
                        minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
                    }else{
                        minimapMat = ImageUtil.resizeMat(minimapBaseMat, worldsEdgeScaleResolution, worldsEdgeScaleResolution);
                    }
                } else if (MapEnum.BROKENMOON.equals(currentMapEnum)) {
                    minimapMat = ImageUtil.resizeMat(minimapBaseMat, brokenMoonScaleResolution, brokenMoonScaleResolution);
                } else if (MapEnum.KINGSCANYON.equals(currentMapEnum)) {
                    minimapMat = ImageUtil.resizeMat(minimapBaseMat, kingsCanyonScaleResolution, kingsCanyonScaleResolution);
                }
                if (directionImageFOrRotate != null){
                    minimapMat = rotateMinimap(minimapMat, directionImageFOrRotate);
                    if (minimapMat == null){
                        ImageMatching im = new ImageMatching();
                        im.setScore(0d);
                        return im;
                    }
                }

                matchLoc = imageCompare.findInMap(minimapMat, currentMapMat, allPoints.isEmpty()||playerIsDead||skipCount>1?null:allPoints.get(allPoints.size()-1).getPoint());
            }
        } else if (round == 5) {
            if (MapEnum.OLYMPUS.equals(currentMapEnum)) {
                //313*0.76=237.88
                final int scaleValue = Double.valueOf(olympusScaleResolution * 0.76).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            } else if (MapEnum.STORMPOINT.equals(currentMapEnum)) {
                //274*0.76=191.8
                final int scaleValue = Double.valueOf(stormPointScaleResolution * 0.76).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            } else if (MapEnum.STORMPOINT_19.equals(currentMapEnum)) {
                //274*0.76=191.8
                final int scaleValue = Double.valueOf(stormPointScaleResolution * 0.76).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            } else if (MapEnum.WORLDSEDGE.equals(currentMapEnum)) {
                //315*0.76=239.4
                final int scaleValue = Double.valueOf(worldsEdgeScaleResolution * 0.76).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            } else if (MapEnum.BROKENMOON.equals(currentMapEnum)) {
                //330*0.76=250.8
                final int scaleValue = Double.valueOf(brokenMoonScaleResolution * 0.76).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            } else if (MapEnum.KINGSCANYON.equals(currentMapEnum)) {
                //330*0.76=250.8
                final int scaleValue = Double.valueOf(kingsCanyonScaleResolution * 0.76).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            }
            if (directionImageFOrRotate != null){
                minimapMat = rotateMinimap(minimapMat, directionImageFOrRotate);
                if (minimapMat == null){
                    ImageMatching im = new ImageMatching();
                    im.setScore(0d);
                    return im;
                }
            }
            matchLoc = imageCompare.findInMap(minimapMat, currentMapMat, allPoints.isEmpty()||playerIsDead||skipCount>1?null:allPoints.get(allPoints.size()-1).getPoint());
            //                            matchLoc = imageCompare.findInMap(minimap, currentMap, GameState.getUsedPoints().get(GameState.getUsedPoints().size()-1).getPoint());
        } else if (round == 6) {
            if (MapEnum.OLYMPUS.equals(currentMapEnum)) {
                //313*0.76=237.88*0.5=118.94
                final int scaleValue = Double.valueOf(olympusScaleResolution * 0.76 * 0.5).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            } else if (MapEnum.STORMPOINT.equals(currentMapEnum)) {
                //274*0.76=191.8*0.5=95.9
                final int scaleValue = Double.valueOf(stormPointScaleResolution * 0.76 * 0.5).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            } else if (MapEnum.STORMPOINT_19.equals(currentMapEnum)) {
                //274*0.76=191.8*0.5=95.9
                final int scaleValue = Double.valueOf(stormPointScaleResolution * 0.76 * 0.5).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            } else if (MapEnum.WORLDSEDGE.equals(currentMapEnum)) {
                //315*0.76=239.4*0.5=119.7
                final int scaleValue = Double.valueOf(worldsEdgeScaleResolution * 0.76 * 0.5).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            } else if (MapEnum.BROKENMOON.equals(currentMapEnum)) {
                //330*0.76=250.8*0.5=125.4
                final int scaleValue = Double.valueOf(brokenMoonScaleResolution * 0.76 * 0.5).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            } else if (MapEnum.KINGSCANYON.equals(currentMapEnum)) {
                //330*0.76=250.8*0.5=125.4
                final int scaleValue = Double.valueOf(kingsCanyonScaleResolution * 0.76 * 0.5).intValue();
                minimapMat = ImageUtil.resizeMat(minimapBaseMat, scaleValue, scaleValue);
            }
            if (directionImageFOrRotate != null){
                minimapMat = rotateMinimap(minimapMat, directionImageFOrRotate);
                if (minimapMat == null){
                    ImageMatching im = new ImageMatching();
                    im.setScore(0d);
                    return im;
                }
            }
            matchLoc = imageCompare.findInMap(minimapMat, currentMapMat, allPoints.isEmpty()||playerIsDead||skipCount>1?null:allPoints.get(allPoints.size()-1).getPoint());
        }
        return matchLoc;
    }*/

    public void getMapBeingPlayedFromMinimapData(final BufferedImage minimapImage, final BufferedImage gameImage) throws IOException {
        //scale minimap to size to fit the 2048x2048 template
        logAndDisplay("Current Map Unknown, attempting to resolve map being played, this can take some time");
        Mat minimapBase = ImageUtil.bufferedImage2Mat(minimapImage);

        //round 1 values
        ImageMatching bestLocation = new ImageMatching();
        bestLocation.setScore(0d);
        bestLocation.setPoint(new Point(0, 0));
        MapEnum bestLocationMap = null;
        Boolean isMinimapRotation = false;
        Double internalZoom = 1d;
        BufferedImage bestLocationFullMap = null;
        BufferedImage bestLocationAllowedLocations = null;
        for (MapEnum map : MapEnum.values()) {
            BufferedImage fullMapImage = au.mapbot.apex.util.CustomMaps.load(map, true);
            BufferedImage fullMapAllowedLocationImage = ImageUtil.getImageFromResource(map.getMapAllowedLocationResourceString());

            if (minimapRotation == null || !minimapRotation) {
                ZoomLevelCheck zlc = getMinimapZoom(minimapImage, map, fullMapImage);
                if (zlc.imageMatching.getScore() > bestLocation.getScore()) {
                    if (isPointInsideAllowedMapLocations(zlc.imageMatching, fullMapAllowedLocationImage)) {
                        internalZoom = zlc.zoom;
                        bestLocation = zlc.imageMatching;
                        bestLocationMap = map;
                        isMinimapRotation = false;
                        bestLocationFullMap = fullMapImage;
                        bestLocationAllowedLocations = fullMapAllowedLocationImage;
                    }
                }
            }
            if (minimapRotation == null || minimapRotation) {
                //check with minimap rotation
                BufferedImage compassDirection = screenProcessToImage.getDirection(gameImage);
                Mat minimapMat = ImageUtil.bufferedImage2Mat(minimapImage);
                Mat rotatedMinimap = rotateMinimap(minimapMat, compassDirection);
                if (rotatedMinimap != null) {
                    ZoomLevelCheck zlcRotate = getMinimapZoom(rotatedMinimap, map, fullMapImage, true);
                    if (zlcRotate.imageMatching.getScore() > bestLocation.getScore()) {
                        if (isPointInsideAllowedMapLocations(zlcRotate.imageMatching, fullMapAllowedLocationImage)) {
                            internalZoom = zlcRotate.zoom;
                            bestLocation = zlcRotate.imageMatching;
                            bestLocationMap = map;
                            isMinimapRotation = true;
                            bestLocationFullMap = fullMapImage;
                            bestLocationAllowedLocations = fullMapAllowedLocationImage;
                        }
                    }
                }
            }
        }
        if (bestLocation.getScore() >= 0.6 || detectedMapPreviousMatchMap!= null && detectedMapPreviousMatchMap == bestLocationMap) {
            if (isMinimapRotation) {
                minimapRotation = true;
                logAndDisplay("minimap Rotation is on");
            } else {
                minimapRotation = false;
                logAndDisplay("minimap Rotation is off");
            }
            zoom = internalZoom;
            logAndDisplay("minimap zoom level detected at: " + internalZoom);

            detectedMap = bestLocationMap;
            currentMapAllowedLocations = bestLocationAllowedLocations;
            currentFullColorMap = bestLocationFullMap;
            if (mapThread != null){
                mapThread.setMap(currentFullColorMap);
            }
            logAndDisplay("map detected as: " + bestLocationMap.getText() + " with score: " + bestLocation.getScore());


//            return bestLocationMap;
        }else{
            if (bestLocationMap != null){
                detectedMapPreviousMatchMap = bestLocationMap;
                logAndDisplay("Attempted to detect map but not high enough confidence yet, map:" + bestLocationMap.getText() + " with score: " + bestLocation.getScore());
            }else{
                logAndDisplay("Need to collect more map data to detect map being played");
            }
        }
//        return null;



//        logAndDisplay("Game is started and current map is unknown, attempting to resolve");
        //get highest percentage image match map
        //need to do to this way as because of backgrounds can still get decent scores on wrong map
//        Mat findMap = ImageUtil.resizeMat(minimapBase, olympusScaleResolution, olympusScaleResolution);
//        ImageMatching matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.OLYMPUS));
//        MapEnum highestMap = MapEnum.OLYMPUS;
////                            Double highestScore = matchLoc.getScore();
//        ImageMatching best = matchLoc;
//
//        findMap = ImageUtil.resizeMat(minimapBase, stormPointScaleResolution, stormPointScaleResolution);
//        matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.STORMPOINT_19));
//        if (matchLoc.getScore() > best.getScore()) {
//            highestMap = MapEnum.STORMPOINT_19;
//            best = matchLoc;
//        }
//
//        findMap = ImageUtil.resizeMat(minimapBase, worldsEdgeScaleResolution, worldsEdgeScaleResolution);
//        matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.WORLDSEDGE));
//        if (matchLoc.getScore() > best.getScore()) {
//            highestMap = MapEnum.WORLDSEDGE;
//            best = matchLoc;
//        }
//
//        findMap = ImageUtil.resizeMat(minimapBase, brokenMoonScaleResolution, brokenMoonScaleResolution);
//        matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.BROKENMOON));
//        if (matchLoc.getScore() > best.getScore()) {
//            highestMap = MapEnum.BROKENMOON;
//            best = matchLoc;
//        }
//
//        findMap = ImageUtil.resizeMat(minimapBase, kingsCanyonScaleResolution, kingsCanyonScaleResolution);
//        matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.KINGSCANYON));
//        if (matchLoc.getScore() > best.getScore()) {
//            highestMap = MapEnum.KINGSCANYON;
//            best = matchLoc;
//        }
//
//        //with new season algs spawn in dropship and treated as dead from the start so need the zoomed in values
//        if (best.getScore() < 0.6 || (highestMap == MapEnum.BROKENMOON && best.getScore() < 0.45)){
//            int scaleValue = Double.valueOf(stormPointScaleResolution * 1.27).intValue();
//            findMap = ImageUtil.resizeMat(minimapBase, scaleValue, scaleValue);
//            matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.STORMPOINT_19));
//            if (matchLoc.getScore() > best.getScore()) {
//                highestMap = MapEnum.STORMPOINT_19;
//                best = matchLoc;
//            }
//
//            scaleValue = Double.valueOf(worldsEdgeScaleResolution * 1.27).intValue();
//            findMap = ImageUtil.resizeMat(minimapBase, scaleValue, scaleValue);
//            matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.WORLDSEDGE));
//            if (matchLoc.getScore() > best.getScore()) {
//                highestMap = MapEnum.WORLDSEDGE;
//                best = matchLoc;
//            }
//
//            scaleValue = Double.valueOf(brokenMoonScaleResolution * 1.27).intValue();
//            findMap = ImageUtil.resizeMat(minimapBase, scaleValue, scaleValue);
//            matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.BROKENMOON));
//            if (matchLoc.getScore() > best.getScore()) {
//                highestMap = MapEnum.BROKENMOON;
//                best = matchLoc;
//            }
//
//            scaleValue = Double.valueOf(kingsCanyonScaleResolution * 1.27).intValue();
//            findMap = ImageUtil.resizeMat(minimapBase, scaleValue, scaleValue);
//            matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.KINGSCANYON));
//            if (matchLoc.getScore() > best.getScore()) {
//                highestMap = MapEnum.KINGSCANYON;
//                best = matchLoc;
//            }
//        }
//
//        //with new season algs spawn in dropship and treated as dead from the start so need the zoomed in values
//        if (best.getScore() < 0.6 || (highestMap == MapEnum.BROKENMOON && best.getScore() < 0.45)){
//            int scaleValue = Double.valueOf(stormPointScaleResolution * 1.4).intValue();
//            findMap = ImageUtil.resizeMat(minimapBase, scaleValue, scaleValue);
//            matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.STORMPOINT_19));
//            if (matchLoc.getScore() > best.getScore()) {
//                highestMap = MapEnum.STORMPOINT_19;
//                best = matchLoc;
//            }
//
//            scaleValue = Double.valueOf(worldsEdgeScaleResolution * 1.4).intValue();
//            findMap = ImageUtil.resizeMat(minimapBase, scaleValue, scaleValue);
//            matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.WORLDSEDGE));
//            if (matchLoc.getScore() > best.getScore()) {
//                highestMap = MapEnum.WORLDSEDGE;
//                best = matchLoc;
//            }
//
//            scaleValue = Double.valueOf(brokenMoonScaleResolution * 1.4).intValue();
//            findMap = ImageUtil.resizeMat(minimapBase, scaleValue, scaleValue);
//            matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.BROKENMOON));
//            if (matchLoc.getScore() > best.getScore()) {
//                highestMap = MapEnum.BROKENMOON;
//                best = matchLoc;
//            }
//
//            scaleValue = Double.valueOf(kingsCanyonScaleResolution * 1.4).intValue();
//            findMap = ImageUtil.resizeMat(minimapBase, scaleValue, scaleValue);
//            matchLoc = imageCompare.findInMap(findMap, ImageCompare.getMap(MapEnum.KINGSCANYON));
//            if (matchLoc.getScore() > best.getScore()) {
//                highestMap = MapEnum.KINGSCANYON;
//                best = matchLoc;
//            }
//        }
//
//        if (best.getScore() > 0.5 || (highestMap == MapEnum.BROKENMOON && best.getScore() > 0.45)) {
//            switch (highestMap) {
//                case KINGSCANYON:
//                    BufferedImage kcBufferedImage = ImageUtil.getImageFromResource("/maps/KingsCanyonBW.png");
//                    Color kcPixelColor = new Color(kcBufferedImage.getRGB(Double.valueOf(best.getPoint().x).intValue(), Double.valueOf(best.getPoint().y).intValue()));
//                    if (kcPixelColor.equals(Color.BLACK)) {
//                        return MapEnum.KINGSCANYON;
//                    }
//                    break;
//                case OLYMPUS:
//                    BufferedImage oBufferedImage = ImageUtil.getImageFromResource("/maps/OlympusBW.png");
//                    Color oPixelColor = new Color(oBufferedImage.getRGB(Double.valueOf(best.getPoint().x).intValue(), Double.valueOf(best.getPoint().y).intValue()));
//                    if (oPixelColor.equals(Color.BLACK)) {
//                        return MapEnum.OLYMPUS;
//                    }
//                    break;
//                case BROKENMOON:
//                    BufferedImage bmBufferedImage = ImageUtil.getImageFromResource("/maps/BrokenMoonBW.png");
//                    Color bmPixelColor = new Color(bmBufferedImage.getRGB(Double.valueOf(best.getPoint().x).intValue(), Double.valueOf(best.getPoint().y).intValue()));
//                    if (bmPixelColor.equals(Color.BLACK)) {
//                        return MapEnum.BROKENMOON;
//                    }
//                    break;
//                case STORMPOINT_19:
//                    BufferedImage spBufferedImage = ImageUtil.getImageFromResource("/maps/Stormpoint19BW.png");
//                    Color spPixelColor = new Color(spBufferedImage.getRGB(Double.valueOf(best.getPoint().x).intValue(), Double.valueOf(best.getPoint().y).intValue()));
//                    if (spPixelColor.equals(Color.BLACK)) {
//                        return MapEnum.STORMPOINT_19;
//                    }
//                    break;
//                case WORLDSEDGE:
//                    BufferedImage weBufferedImage = ImageUtil.getImageFromResource("/maps/WorldsEdgeBW.png");
//                    Color wePixelColor = new Color(weBufferedImage.getRGB(Double.valueOf(best.getPoint().x).intValue(), Double.valueOf(best.getPoint().y).intValue()));
//                    if (wePixelColor.equals(Color.BLACK)) {
//                        return MapEnum.WORLDSEDGE;
//                    }
//                    break;
//                default:
//                    break;
//            }
//
//        }
//        return null;
    }
    public java.util.List<DetectedCircle> getZones(final BufferedImage mapImage, final int round, final boolean isClosing) throws IOException {
        Mat fullMapMat = ImageUtil.bufferedImage2Mat(mapImage);
        Mat fullMapMatResized = ImageUtil.resizeMat(fullMapMat, 1024, 1024);
        List<DetectedCircle> circles = circleUtil.getCirclesInFullMap(fullMapMatResized, round, isClosing);
        return circles;
    }

    public List<DetectedCircle> getActiveZones(int round, List<DetectedCircle> detectedCircles){
        java.util.List<DetectedCircle> returnCircles = new ArrayList<>();

        if (round >= 1) {
            DetectedCircle zone1Exact = circleUtil.getZoneExact(detectedCircles, 1, circleUtil.getRing1Radius(), round);
            if (zone1Exact != null) {
                returnCircles.add(zone1Exact);
            }
        }

        if (round >= 2) {
            DetectedCircle zone2Exact = circleUtil.getZoneExact(detectedCircles, 2, circleUtil.getRing2Radius(), round);
            if (zone2Exact != null) {
                returnCircles.add(zone2Exact);
            }
        }

        if (round >= 3) {
            DetectedCircle zone3Exact = circleUtil.getZoneExact(detectedCircles, 3, circleUtil.getRing3Radius(), round);
            if (zone3Exact != null) {
                returnCircles.add(zone3Exact);
            }
        }
        if (round >= 4) {
            DetectedCircle zone4Exact = circleUtil.getZoneExact(detectedCircles, 4, circleUtil.getRing4Radius(), round);
            if (zone4Exact != null) {
                returnCircles.add(zone4Exact);
            }
        }
        if (round >= 5) {
            DetectedCircle zone5Exact = circleUtil.getZoneExact(detectedCircles, 5, circleUtil.getRing5Radius(), round);
            if (zone5Exact != null) {
                returnCircles.add(zone5Exact);
            }
        }


        return returnCircles;
    }

    public Boolean isMapShown(final BufferedImage gameImage, final BufferedImage map) throws IOException {

        if (map != null) {
            Mat fullMapMat = ImageUtil.bufferedImage2Mat(gameImage);
            Mat fullMapMatResized = ImageUtil.resizeMat(fullMapMat, 2048, 2048);
            BufferedImage fullMapMatResizedBuff = circleUtil.matToBufferedImage(fullMapMatResized).getSubimage(20, 20, fullMapMatResized.width() - 40, fullMapMatResized.height() - 40);

            Mat actualMap = ImageUtil.bufferedImage2Mat(map);
            ImageMatching matchLoc = imageCompare.findInMap(ImageUtil.bufferedImage2Mat(fullMapMatResizedBuff), actualMap, null, false, false, null);
            if (matchLoc.getScore() > 0.4) {
                return true;
            }
        }
        return false;
    }

    public static class RoundStatus{
        Integer round;
        Boolean isRoundClosing;
        public RoundStatus(final Integer round, final Boolean isClosing){
            this.round = round;
            this.isRoundClosing = isClosing;
        }

        public Integer getRound() {
            return round;
        }

        public Boolean getRoundClosing() {
            return isRoundClosing;
        }
    }

    public RoundStatus getRound(final BufferedImage gameImage) throws TesseractException, IOException {
        //get round

        //ring closing text

        //ring is not closing text (round + countdown)
        BufferedImage roundNumberText;
        if (!playerIsDead) {
            roundNumberText = screenProcessToImage.getRound(gameImage);
        }else{
            roundNumberText = screenProcessToImage.getRoundDead(gameImage);
        }
        SnapshotFromImage.writeImageToFile(roundNumberText, "roundNumberText");

        OCRMatch roundNumberValue = ocr.ocrSingle(roundNumberText, currentRound, false, 190, 220);
        if (roundNumberValue.getConfidence() > 72) {
            if (roundNumberValue.getResult().equals("1")) {
                return new RoundStatus(1, false);
            } else if (roundNumberValue.getResult().equals("2")) {
                return new RoundStatus(2, false);
            } else if (roundNumberValue.getResult().equals("3")) {
                return new RoundStatus(3, false);
            } else if (roundNumberValue.getResult().equals("4")) {
                return new RoundStatus(4, false);
            } else if (roundNumberValue.getResult().equals("5")) {
                return new RoundStatus(5, false);
            } else if (roundNumberValue.getResult().equals("6")) {
                return new RoundStatus(6, false);
            }
        }

        BufferedImage roundText;
        if (!playerIsDead) {
            roundText = screenProcessToImage.getRoundClosing(gameImage);
        }else{
            if (isStreamedSquished != null && isStreamedSquished){
                //TODO dead 4:3
                roundText = screenProcessToImage.getRoundClosingDead1610Squished(gameImage);
            }else {
                roundText = screenProcessToImage.getRoundClosingDead(gameImage);
            }
        }
        SnapshotFromImage.writeImageToFile(roundText, "roundText");

        String roundValue = ocr.basicProcessOcrScale(roundText).replaceAll("\\|","1");

        boolean containsRoundClosingText = false;
        if (language == Languages.ENGLISH){
            if (containsWithVariation(roundValue,ocr.screenText(Ocr.TextEnum.ROUND_CLOSING_pt1)) || containsWithVariation(roundValue,ocr.screenText(Ocr.TextEnum.ROUND_CLOSING_pt2))) {
                containsRoundClosingText = true;
            }
        }else if (isCjk(language)){
            if (compareStringsOnlyNeed2CharsMatch(roundValue,ocr.screenText(Ocr.TextEnum.ROUND_CLOSING_pt1)) || compareStringsOnlyNeed2CharsMatch(roundValue,ocr.screenText(Ocr.TextEnum.ROUND_CLOSING_pt2))){
                containsRoundClosingText = true;
            }
        }
        if (containsRoundClosingText) {
            if (roundValue.contains("1")) {
                return new RoundStatus(1, true);
            } else if (roundValue.contains("2")) {
                return new RoundStatus(2, true);
            } else if (roundValue.contains("3")) {
                return new RoundStatus(3, true);
            } else if (roundValue.contains("4")) {
                return new RoundStatus(4, true);
            } else if (roundValue.contains("5")) {
                return new RoundStatus(5, true);
            } else if (roundValue.contains("6")) {
                return new RoundStatus(6, true);
            } else if (containsWithVariation(roundValue, ocr.screenText(Ocr.TextEnum.FINAL_ROUND))) {
                return new RoundStatus(6, true);
            }
        }
        return null;
    }

//    public static void main(String[] args) throws AWTException, TesseractException, IOException {
////        String str1 = "10SQUADSLEFL";
////        String str2 = "SQUADSLEFT";
//        String str1 = "RANKEDLEAGUES\nKTNGSCANYON";
//        String str2 = "RANKED";
//        boolean containsWithVariation = containsWithVariation(str1, str2);
//        System.out.println(containsWithVariation);
//    }

    public static boolean contains(final String str1, final String str2) {
        if (str1.contains(str2)){
            return true;
        }
        return false;
    }
    public static boolean containsWithVariation(final String str1, final String str2) {
        if (str1.contains(str2)){
            return true;
        }

        int str2Val = 0;
        boolean alreadySkipped = false;
        boolean startedWork = false;
        for (int i = 0; i < str1.length(); i++) {
            char ch = str1.charAt(i);

            //if hit first or second value
            if (!startedWork) {
                if (String.valueOf(ch).equalsIgnoreCase(String.valueOf(str2.charAt(str2Val)))) {
                    startedWork = true;
                } else if (str2Val + 1 < str2.length() - 1) {
                    if (String.valueOf(ch).equalsIgnoreCase(String.valueOf(str2.charAt(str2Val + 1)))) {
                        startedWork = true;
                        alreadySkipped = true;
                        str2Val = str2Val + 1;
                    }
                }
            }
            if (startedWork){
                if(String.valueOf(ch).equalsIgnoreCase(String.valueOf(str2.charAt(str2Val)))){
                    str2Val++;
                }else{
                    if (!alreadySkipped){
                        str2Val++;
                        alreadySkipped = true;
                    }else{
                        startedWork = false;
                        str2Val = 0;
                    }
                }
            }
            if (str2Val == str2.length()){
                return true;
            }

        }
        return false;
    }

    public boolean isSquadsLeftTextShownRanked(final BufferedImage gameImage) throws TesseractException {

        BufferedImage squadsLeft;
        if (isCjk(language)){
            squadsLeft = screenProcessToImage.getSquadsLeftRankedJP(gameImage);
        }else {
            squadsLeft = screenProcessToImage.getSquadsLeftRanked(gameImage);
        }

        String value = ocr.basicProcessOcrScale(squadsLeft).replace(" ", "").trim();
        String compareValue = ocr.screenText(Ocr.TextEnum.SQUADLEFT);
        if (isCjk(language)){
            return compareStringsOnlyNeed2CharsMatch(value, compareValue);
        }else {
            if (containsWithVariation(value, compareValue)) {
                return true;
            } else if (containsWithVariation(value, ocr.screenText(Ocr.TextEnum.PLAYERSLEFT))) {
                solos = true;
                logAndDisplay("Solo's game mode detected");
                return true;
            }
        }
        return false;
    }


    public static boolean compareStringsOnlyNeed2CharsMatch(String str1, String str2) {
        int matchCount = 0;

        for (int i = 0; i < str1.length(); i++) {
            char ch = str1.charAt(i);
            if (str2.indexOf(ch) != -1) {  // Check if character ch is present in str2
                matchCount++;
                if (matchCount >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSquadsLeftTextShownUnRanked(final BufferedImage gameImage) throws TesseractException {

        BufferedImage squadsLeft;
        if (isCjk(language)){
            squadsLeft = screenProcessToImage.getSquadsLeftUnRankedJP(gameImage);
        }else {
            squadsLeft = screenProcessToImage.getSquadsLeftUnRanked(gameImage);
        }

        String value = ocr.basicProcessOcrScale(squadsLeft).replace(" ", "").trim();

        String compareValue = ocr.screenText(Ocr.TextEnum.SQUADLEFT);
        if (isCjk(language)){
            return compareStringsOnlyNeed2CharsMatch(value, compareValue);
        }else {
            if (containsWithVariation(value, compareValue)) {
                return true;
            } else if (containsWithVariation(value, ocr.screenText(Ocr.TextEnum.PLAYERSLEFT))) {
                solos = true;
                logAndDisplay("Solo's game mode detected");
                return true;
            }
        }
        return false;
    }

    public boolean isUltPercentShown(final BufferedImage subimage) throws IOException {
        OCRMatch screenText = ocr.chatGPT(subimage);
        if (screenText.getConfidence() > 80 && StringUtils.containsIgnoreCase(screenText.getResult(), "%")) {
           return true;
        }
        return false;
    }


    public Integer tryGetIntegerFromSquadsLeft(BufferedImage squadsLeftSubimage){
        try{
            //trim the image to just be the number
            BufferedImage squadsLeftImages = squadsLeftSubimage.getSubimage(0, 0, Double.valueOf(squadsLeftSubimage.getWidth() / 4.5).intValue(), squadsLeftSubimage.getHeight());
            if (isCjk(language)){
                squadsLeftImages = squadsLeftSubimage.getSubimage(0, 0, (squadsLeftSubimage.getWidth() / 3), squadsLeftSubimage.getHeight());
            }
            OCRMatch squadLeftText = ocr.ocrSingle(squadsLeftImages, squadsLeft, true, 190, 220);

            int squadsLeftValue = Integer.parseInt(squadLeftText.getResult());
            return squadsLeftValue;

        }catch (Exception ex){
            //parsing issues e.g. read value isn't a number
        }
        return null;
    }

    public Integer getSquadsLeftUnRanked(BufferedImage gameImage, boolean getPlayerIsDeadButNotWholeTeam) {

        BufferedImage squadsLeftSubimage;

        //get squads left while dead as long as whole team not dead
        if (getPlayerIsDeadButNotWholeTeam){
            if (isCjk(language)) {
                squadsLeftSubimage = screenProcessToImage.getSquadsLeftUnRankedDeadJP(gameImage);
            }else{
                squadsLeftSubimage = screenProcessToImage.getSquadsLeftUnRankedDead(gameImage);
            }
        }else {
            //get different screen location depending on language e.g. "16 SQUADS LEFT"
            if (isCjk(language)) {
                squadsLeftSubimage = screenProcessToImage.getSquadsLeftUnRankedJP(gameImage);
            } else {
                squadsLeftSubimage = screenProcessToImage.getSquadsLeftUnRanked(gameImage);
            }
        }
        return tryGetIntegerFromSquadsLeft(squadsLeftSubimage);
    }


    public Integer getSquadsLeftRanked(BufferedImage gameImage, boolean getPlayerIsDeadButNotWholeTeam) {

        BufferedImage squadsLeftSubimage;

        //get squads left while dead as long as whole team not dead
        if (getPlayerIsDeadButNotWholeTeam){
            if (isCjk(language)) {
                squadsLeftSubimage = screenProcessToImage.getSquadsLeftRankedDeadJP(gameImage);
            }else{
                squadsLeftSubimage = screenProcessToImage.getSquadsLeftRankedDead(gameImage);
            }
        }else {
            //get different screen location depending on language e.g. "16 SQUADS LEFT"
            if (isCjk(language)) {
                squadsLeftSubimage = screenProcessToImage.getSquadsLeftRankedJP(gameImage);
            } else {
                squadsLeftSubimage = screenProcessToImage.getSquadsLeftRanked(gameImage);
            }
        }
        return tryGetIntegerFromSquadsLeft(squadsLeftSubimage);
    }


    public boolean isInMainMenu(BufferedImage subImage) throws TesseractException {
        //default language
        if (language == null){
            ocr.setLanguageEng();
        }
        final String playerDeadTextValue = ocr.basicProcessOcrScale(subImage).replace(" ","").trim();

        return isInMainMenu(playerDeadTextValue);
    }

    public boolean isInMainMenu(String playerDeadTextValue) throws TesseractException {
        //default language
        if (language == null){
            ocr.setLanguageEng();
        }

        if (contains(playerDeadTextValue, ocr.screenText(Ocr.TextEnum.MAINMENU_1_PLAY))
                || contains(playerDeadTextValue, ocr.screenText(Ocr.TextEnum.MAINMENU_2_SEASONAL))
                //|| playerDeadTextValue.contains(ocr.screenText(Ocr.TextEnum.MAINMENU_3_LEGENDS)) //is also in inventory screen
                || contains(playerDeadTextValue, ocr.screenText(Ocr.TextEnum.MAINMENU_4_LOADOUT))
                || contains(playerDeadTextValue, ocr.screenText(Ocr.TextEnum.MAINMENU_5_STORE).replace(" ",""))){
            return true;
        }
        return false;
    }

    public boolean isLobby(BufferedImage submage) throws TesseractException {
        //default language
        if (language == null){
            ocr.setLanguageEng();
        }
        final String playerDeadTextValue = ocr.basicProcessOcrScale(submage).replace(" ","").trim();
        return isLobby(playerDeadTextValue);
    }

    public boolean isLobby(String playerDeadTextValue) throws TesseractException {
        //default language
        if (language == null){
            ocr.setLanguageEng();
        }

        if (containsWithVariation(playerDeadTextValue, ocr.screenText(Ocr.TextEnum.LOBBY))
                || containsWithVariation(playerDeadTextValue, ocr.screenText(Ocr.TextEnum.LOBBY_FINISHED))){ // cant use summary as shown on team dead screen
            return true;
        }
        return false;
    }

    public boolean isPlayerDead(BufferedImage subImage) throws TesseractException {
        final String playerDeadTextValue = ocr.basicProcessOcrScale(subImage).replace(" ","").trim();

        if (containsWithVariation(playerDeadTextValue, ocr.screenText(Ocr.TextEnum.PLAYER_DEAD_1))
                || containsWithVariation(playerDeadTextValue, ocr.screenText(Ocr.TextEnum.PLAYER_DEAD_2))) {
            return true;
        }
        return false;
    }


    public boolean isWholeTeamDead(BufferedImage subImage) throws TesseractException {
        final String playerDeadTextValue = ocr.basicProcessOcrScale(subImage).replace(" ","").trim();

//        if (containsWithVariation(playerDeadTextValue, ocr.screenText(Ocr.TextEnum.WHOLE_TEAM_DEAD))) {
//            return true;
//        }
        return false;
    }

    public boolean getSquadLeftEng(final BufferedImage subimage) throws TesseractException {
        ocr.setLanguageEng();
        String value = ocr.basicProcessOcrScale(subimage).replace(" ", "").trim();
        if (containsWithVariation(value, ocr.screenText(Ocr.TextEnum.SQUADLEFT))) {
            return true;
        }
        else if (containsWithVariation(value, ocr.screenText(Ocr.TextEnum.PLAYERSLEFT))){
            solos = true;
            logAndDisplay("Solo's game mode detected");
            return true;
        }
        return false;
    }



    public boolean getSquadLeftJpn(final BufferedImage subimage) throws TesseractException {
        ocr.setLanguageJpn();
        String value = ocr.basicProcessOcrScale(subimage).replace(" ", "").trim();
        if (containsWithVariation(value, ocr.screenText(Ocr.TextEnum.SQUADLEFT))) {
            return true;
        }
        else if (containsWithVariation(value, ocr.screenText(Ocr.TextEnum.PLAYERSLEFT))){
            solos = true;
            logAndDisplay("Solo's game mode detected");
            return true;
        }
        return false;
    }


    public boolean inShipCheckEnglish(final BufferedImage subimage) throws TesseractException, IOException {
        ocr.setLanguageEng();
        String screenText = ocr.basicProcessOcrScale(subimage);
        SnapshotFromImage.writeImageToFile(subimage, "inShipText");
        if (containsWithVariation(screenText, ocr.screenText(Ocr.TextEnum.PING))
                || containsWithVariation(screenText, ocr.screenText(Ocr.TextEnum.SUGGEST))
                || containsWithVariation(screenText, ocr.screenText(Ocr.TextEnum.LAUNCH))) {

            return true;
        }
        return false;
    }

    public boolean getSquadLeftChs(final BufferedImage subimage) throws TesseractException {
        ocr.setLanguageChs();
        String value = ocr.basicProcessOcrScale(subimage).replace(" ", "").trim();
        if (compareStringsOnlyNeed2CharsMatch(value, ocr.screenText(Ocr.TextEnum.SQUADLEFT))) {
            return true;
        }
        else if (compareStringsOnlyNeed2CharsMatch(value, ocr.screenText(Ocr.TextEnum.PLAYERSLEFT))){
            solos = true;
            logAndDisplay("Solo's game mode detected");
            return true;
        }
        return false;
    }

    public boolean inShipCheckChinese(final BufferedImage subimage) throws TesseractException {
        ocr.setLanguageChs();
        String screenText = ocr.basicProcessOcrScale(subimage);
        if (containsWithVariation(screenText, ocr.screenText(Ocr.TextEnum.PING))
                || containsWithVariation(screenText, ocr.screenText(Ocr.TextEnum.SUGGEST))
                || containsWithVariation(screenText, ocr.screenText(Ocr.TextEnum.LAUNCH))) {
            return true;
        }
        return false;
    }

    public boolean inShipCheckJapanese(final BufferedImage subimage) throws TesseractException {
        ocr.setLanguageJpn();
        String screenText = ocr.basicProcessOcrScale(subimage);
        if (containsWithVariation(screenText, ocr.screenText(Ocr.TextEnum.PING))
                || containsWithVariation(screenText, ocr.screenText(Ocr.TextEnum.SUGGEST))
                || containsWithVariation(screenText, ocr.screenText(Ocr.TextEnum.LAUNCH))) {
            return true;
        }
        return false;
    }

    public MapEnum checkMap(final BufferedImage gameImage) throws TesseractException {

        final BufferedImage champSelectMap = screenProcessToImage.getMapFromChampSelect(gameImage);
        final String mapString = ocr.basicProcessOcrScale(champSelectMap);
//        System.out.println("Map/Ranked String Value: " + mapString);
        if (containsWithVariation(mapString, "STORMPOINT")) {
          return MapEnum.STORMPOINT;
        } else if (containsWithVariation(mapString, "WORLD'SEDGE")) {
            return MapEnum.WORLDSEDGE;
        } else if (containsWithVariation(mapString, "BROKENMOON")) {
            return MapEnum.BROKENMOON;
        } else if (containsWithVariation(mapString, "OLYMPUS")) {
           return MapEnum.OLYMPUS;
        } else if (containsWithVariation(mapString, "KINGSCANYON")) {
            return MapEnum.KINGSCANYON;
        } else if (containsWithVariation(mapString, "DISTRICT")) {
            return MapEnum.DISTRICT;
        }
        if (language == Languages.CHINESE) {
            return checkMapChinese(mapString);
        }
        return null;
    }

    // Simplified Chinese UI text, from the game's localization_schinese file
    private static final String CHS_CHAMPION = "捍卫者";

    public static MapEnum checkMapChinese(final String mapString) {
        if (mapString.contains("风暴")) {
            return MapEnum.STORMPOINT;
        } else if (mapString.contains("世界") || mapString.contains("尽头")) {
            return MapEnum.WORLDSEDGE;
        } else if (mapString.contains("残月")) {
            return MapEnum.BROKENMOON;
        } else if (mapString.contains("奥林") || mapString.contains("匹斯")) {
            return MapEnum.OLYMPUS;
        } else if (mapString.contains("诸王") || mapString.contains("峡谷")) {
            return MapEnum.KINGSCANYON;
        } else if (mapString.contains("电力")) {
            return MapEnum.DISTRICT;
        }
        return null;
    }



    public boolean checkRankedTextLobby(final BufferedImage gameImage) throws TesseractException {
        final BufferedImage champSelectMap = screenProcessToImage.getMapFromChampSelect(gameImage);
        //default to english if unknown
        if (language == null){
            ocr.setLanguageEng();
        }
        final String mapString = ocr.basicProcessOcrScale(champSelectMap);

        //get is Ranked from champ select screen
        if (containsWithVariation(mapString, ocr.screenText(Ocr.TextEnum.RANKED))) {
            return true;
        }
        return false;
    }

    public BufferedImage getGameImage(final boolean isLocalGame, final String hlsUrl) throws IOException, InterruptedException, TimeoutException {
        BufferedImage gameImage;
        if(webDriverUtil != null && !webDriverUtil.isChromeStillRunning()){
            capture = false;
            gameIsRunning = false;
            System.out.println("Chrome no longer running, likely closed by user, terminating session...");
        }
        if (StringUtils.isNotEmpty(hlsUrl)){
            //gameImage = StreamlinkFFMPEGScreenshotUtil.getScreenImage(hlsUrl);
//            gameImage = fFmpegCapture.getFrame();
            gameImage = webDriverUtil.screenShotVideo();
        }else {
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
        }
        return gameImage;
    }

    public BufferedImage getGameImage(final boolean isLocalGame) throws IOException, InterruptedException, TimeoutException {
        return getGameImage(isLocalGame, null);
    }

    public static boolean checkImageResolutionIs169(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate the aspect ratio (width:height)
        double aspectRatio = (double) width / height;

        // Check if the aspect ratio is approximately 16:9 or 16:10
        boolean is16_9 = isApproxEqual(aspectRatio, 16.0 / 9);

        return is16_9;
    }

    public static boolean checkImageResolutionIs169oor1610(int width, int height) {

        // Calculate the aspect ratio (width:height)
        double aspectRatio = (double) width / height;

        // Check if the aspect ratio is approximately 16:9 or 16:10
        boolean is16_9 = isApproxEqual(aspectRatio, 16.0 / 9);
        boolean is16_10 = isApproxEqual(aspectRatio, 16.0 / 10);

        return is16_9 || is16_10;
    }

    public static boolean checkImageResolutionIs1610(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate the aspect ratio (width:height)
        double aspectRatio = (double) width / height;

        // Check if the aspect ratio is approximately 16:9 or 16:10
        boolean is16_10 = isApproxEqual(aspectRatio, 16.0 / 10);

        return is16_10;
    }

    private static boolean isApproxEqual(double a, double b) {
        double epsilon = 0.01; // Tolerance for approximation
        return Math.abs(a - b) < epsilon;
    }

    /**
     * if all corners aren't white indicating looking at minimap, return false
     * because of things like teamspeak a corner may be covered, so now atleast 3 points required
     *
     * @param potentialMinimapImage
     * @return
     */
    public Boolean isMinimapShown(final BufferedImage potentialMinimapImage){
        int threshold = 140;
        //top left
        int found = 4;
        if ((rgbToGray(potentialMinimapImage.getRGB(0, 0), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(1, 0), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(0, 1), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(1, 1), MODE.LIGHTNESS) < threshold)){
            found -= 1;
        }
        //top right
        if ((rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 1, 0), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 2, 0), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 1, 1), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 2, 1), MODE.LIGHTNESS) < threshold)){
            found -= 1;
        }
        //bottom left
        if ((rgbToGray(potentialMinimapImage.getRGB(0, potentialMinimapImage.getHeight() - 1), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(1, potentialMinimapImage.getHeight() - 1), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(0, potentialMinimapImage.getHeight() - 2), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(1, potentialMinimapImage.getHeight() - 2), MODE.LIGHTNESS) < threshold)){
            found -= 1;
        }
        //bottom right
        if ((rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 1, potentialMinimapImage.getHeight() - 1), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 2, potentialMinimapImage.getHeight() - 1), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 1, potentialMinimapImage.getHeight() - 2), MODE.LIGHTNESS) < threshold)
                && (rgbToGray(potentialMinimapImage.getRGB(potentialMinimapImage.getWidth() - 2, potentialMinimapImage.getHeight() - 2), MODE.LIGHTNESS) < threshold)){
            found -= 1;
        }
        if (found >= 3){
            return true;
        }
        return false;
    }

    enum MODE {
        LIGHTNESS, AVERAGE, LUMINOSITY
    }

    private static int rgbToGray(int rgb, MODE mode) {
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

}
