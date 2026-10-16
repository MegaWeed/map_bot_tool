/*
 * WebDriverUtil
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;

import au.mapbot.apex.GUI.MainGUI2;
import au.mapbot.apex.SnapshotFromImage;
import au.mapbot.apex.classes.MBLogger;
import au.mapbot.apex.classes.MessagePromptEnum;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
//import org.openqa.selenium.devtools.DevTools;
//import org.openqa.selenium.devtools.v122.runtime.Runtime;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.Duration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.WebDriver;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebDriverUtil {

    private WebDriver driver;
    private String webpage;

    private boolean isYoutubeStarted = false;

    private HttpServer server;
    File destination;
    private Date refreshStreamTimer;

    public int getPortToUse(int startPort) {
        // Check if the port is already in use
        for (int i = 0; i<30;i++){
            Random random = new Random();
            int randomNumber = 9100 + random.nextInt(9800 - 9100 + 1); // (max - min + 1)

            try (ServerSocket serverSocket = new ServerSocket(randomNumber)) {
                // Port is available, close the test socket
                serverSocket.close();
                System.out.println("Using Port " + startPort);
                return randomNumber;

            } catch (IOException e) {
                // If an exception is thrown, the port is in use.
                System.err.println("Port " + startPort + " is already in use. Trying a different port.");
            }
        }
        return startPort;
    }

    public static TwitchVodInfo extractTwitchVodInfo(String url) {
        Pattern pattern = Pattern.compile("twitch\\.tv/videos/(\\d+)(?:\\?t=(\\d+h)?(\\d+m)?(\\d+s)?)?");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            String videoId = matcher.group(1);
            String timestamp = "";

            if (matcher.group(2) != null) timestamp += matcher.group(2);
            if (matcher.group(3) != null) timestamp += matcher.group(3);
            if (matcher.group(4) != null) timestamp += matcher.group(4);

            // Optional: Convert timestamp to a total number of seconds
            // int totalSeconds = convertToTotalSeconds(timestamp);
            // System.out.println("Timestamp in seconds: " + totalSeconds);

            return new TwitchVodInfo(videoId, timestamp.isEmpty() ? null : timestamp);
        }
        pattern = Pattern.compile("twitch\\.tv/([^/]+)");
        matcher = pattern.matcher(url);
        if (matcher.find()) {
            String channel = matcher.group(1);
            // Optional: Convert timestamp to a total number of seconds
            // int totalSeconds = convertToTotalSeconds(timestamp);
            // System.out.println("Timestamp in seconds: " + totalSeconds);

            return new TwitchVodInfo(channel);
        }

        return new TwitchVodInfo(url);
    }

    // Optional method to convert the timestamp to total seconds
    // public static int convertToTotalSeconds(String timestamp) {
    //     // Implementation to convert timestamp to total seconds
    //     // ...
    // }

    public static class TwitchVodInfo {

        private final String channel;

        private final String videoId;
        private final String timestamp; // in the format "2h35m8s", could be converted to total seconds

        public TwitchVodInfo(String channel) {
            this.videoId = null;
            this.timestamp = null;
            this.channel = channel;
        }
        public TwitchVodInfo(String videoId, String timestamp) {
            this.videoId = videoId;
            this.timestamp = timestamp;
            this.channel = null;
        }

        public String getVideoId() {
            return videoId;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getChannel() {
            return channel;
        }

        public String getString(){
            if (channel!=null){
                return channel;
            }
            if (timestamp != null){
                return videoId + timestamp;
            }
            return videoId;
        }
    }

    public WebDriverUtil(boolean isHeadless, String twitchPage) throws IOException {
        MBLogger.log("Init WebDriverUtil, isHeadless: " + isHeadless + " | Stream: " + twitchPage);

        if (twitchPage.contains("yout")) {
            driver = createDriver(isHeadless);
            if (driver != null) {
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
                driver.manage().window().setSize(new Dimension(1228, 862));
                webpage = twitchPage;
                try {
                    driver.get(twitchPage);
                    SetWindowPosition.forceWindowToStayOnBottom();
                } catch (Exception ex) {
                    System.out.println("WebDriverUtil configuration failed");
                    MBLogger.warning("WebDriverUtil configuration failed", ex);
                }
            }
        } else {
            int portToUse = findAvailablePort(8001);
            int retryCount = 0;
            boolean serverStarted = false;

            while (retryCount < 5 && !serverStarted) {
                try {
                    server = HttpServer.create(new InetSocketAddress(portToUse), 0);
                    server.createContext("/", new MyHandler());
                    server.setExecutor(null);
                    server.start();
                    System.out.println("WebServer started on port: " + portToUse);
                    serverStarted = true;
                } catch (IOException e) {
                    MBLogger.warning("Port " + portToUse + " in use. Retrying with a new port...", e);
                    portToUse = findAvailablePort(portToUse + 1);
                    retryCount++;
                    try {
                        Random random = new Random();
                        Thread.sleep(random.nextInt(1000 - 500 + 1) + 500);
                    } catch (InterruptedException ignored) {}
                }
            }

            driver = createDriver(isHeadless);
            if (driver != null) {
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
                driver.manage().window().setSize(new Dimension(1228, 862));
                webpage = twitchPage;
            }

            if (!serverStarted) {
                throw new IOException("Failed to start server after multiple attempts.");
            }

            // Set up WebDriver for Twitch stream handling
            try {
                TwitchVodInfo tvi = extractTwitchVodInfo(twitchPage);
                if (tvi != null) {
                    if (tvi.getChannel() != null) {
                        driver.get("http://localhost:" + portToUse + "/stream.html?channel=" + tvi.getChannel());
                    } else if (StringUtils.isNotBlank(tvi.getTimestamp())) {
                        driver.get("http://localhost:" + portToUse + "/stream.html?video=" + tvi.getVideoId() + "&t=" + tvi.getTimestamp());
                    } else {
                        driver.get("http://localhost:" + portToUse + "/stream.html?video=" + tvi.getVideoId());
                    }
                } else {
                    driver.get("http://localhost:" + portToUse + "/stream.html?channel=" + twitchPage);
                }

                //XXX once headless turned on need to reenable or will error
//                    if (!isHeadless) {
                    SetWindowPosition.forceWindowToStayOnBottom();
//                    }
            } catch (Exception ex) {
                System.out.println("WebDriverUtil configuration failed");
                MBLogger.warning("WebDriverUtil configuration failed", ex);
            }
        }
    }

    /**
     * Checks if a port is available, and returns the next available port if it is in use.
     */
    private int findAvailablePort(int startingPort) {
        int port = startingPort;
        while (port < 9999) { // Set an upper bound to avoid infinite loops
            try (ServerSocket socket = new ServerSocket(port)) {
                return port;
            } catch (IOException e) {
                port++; // Try the next port
            }
        }
        throw new RuntimeException("No available ports found in range 8001-9999.");
    }


    // Shutdown method to close the WebDriver gracefully
    public void shutdown() {
        System.out.println("Shutting down...");

        if (driver != null) {
            try {
                driver.quit();
                System.out.println("WebDriver closed gracefully.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (server != null) {
            try {
                server.stop(0);
                System.out.println("Server stopped gracefully");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (destination != null) {
            try {
                System.out.println("Cleaning up user data folder: " + destination);
                FileUtils.deleteDirectory(destination);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // Read the HTML file from the classpath
            byte[] response;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("EmbedStream.html");
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                response = baos.toByteArray();
            }

            // Send the response headers
            t.sendResponseHeaders(200, response.length);

            // Write the HTML file to the response body
            try (OutputStream os = t.getResponseBody()) {
                os.write(response);
            }
        }
    }

    //method to check that chrome is running in case it was closed by a user instead of pressing stop
    public boolean isChromeStillRunning(){
        try {
            driver.getTitle();
        }catch (NoSuchSessionException ex) {
            return false;
        }catch (WebDriverException  ex) {
            return false;
        }
        return true;
    }


    private WebDriver createDriver(boolean isHeadless) {
        System.setProperty("webdriver.chrome.logfile", "chrome.log");

        // Set ChromeDriver log level to WARNING
        System.setProperty("webdriver.chrome.verboseLogging", "false"); // false means no debug spam
        System.setProperty("webdriver.chrome.loglevel", "WARNING");
        WebDriverManager.chromedriver().setup();

        try {
            MBLogger.log("attempting to detect Chrome and Driver Versions");
            // 1. Check ChromeDriver version
            try {
                String driverVersion = io.github.bonigarcia.wdm.WebDriverManager.chromedriver().getDownloadedDriverVersion();
                MBLogger.log("ChromeDriver version from WebDriverManager.chromedriver(): " + driverVersion);
            } catch (Exception ex) {
                MBLogger.warning("Failed to get ChromeDriver version", ex);
            }

            // 2. Try reading from registry
            String[] registryPaths = {
                    "HKLM\\SOFTWARE\\Google\\Chrome\\BLBeacon",
                    "HKLM\\SOFTWARE\\WOW6432Node\\Google\\Chrome\\BLBeacon"
            };

            boolean found = false;

            for (String regPath : registryPaths) {
                try {
                    Process process = new ProcessBuilder("reg", "query", regPath, "/v", "version").start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains("version")) {
                            String[] parts = line.trim().split("\\s+");
                            String version = parts[parts.length - 1];
                            MBLogger.log("Chrome version from registry: " + version);
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                } catch (IOException e) {
                    System.out.println("Registry read error: " + e.getMessage());
                }
            }

            if (!found) {
                MBLogger.warning("Chrome version not found in registry.", null);
            }

            // 3. (Fallback) Try reading file version from chrome.exe
            if (!found) {
                String chromePath = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
                File file = new File(chromePath);
                if (!file.exists()) {
                    chromePath = "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe";
                    file = new File(chromePath);
                }

                if (file.exists()) {
                    try {
                        Process process = new ProcessBuilder("powershell",
                                "(Get-Item '" + chromePath + "').VersionInfo.ProductVersion").start();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String version = reader.readLine();
                        if (version != null && !version.trim().isEmpty()) {
                            MBLogger.log("Chrome version (file): " + version.trim());
                            found = true;
                        }
                    } catch (IOException e) {
                        MBLogger.warning("File version read failed: ", e);
                    }
                } else {
                    MBLogger.log("Chrome not found. (Not installed at default path)");
                }
            }
        } catch (Exception e) {
            MBLogger.warning("Error reading Chrome version", e);
        }

        for (int attempt = 1; attempt <= 5; attempt++) {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--no-sandbox");
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--window-size=1280,720");
            options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

            try {
                String userDataDir = "--user-data-dir=" + System.getProperty("user.dir") + "/Settings/Chrome0";
                if (isHeadless){
                    userDataDir = "--user-data-dir=" + System.getProperty("user.dir") + "/Settings/Chrome" + (server.getAddress().getPort()-8001);;
                }
                MBLogger.log(userDataDir);
                String profileDirectory = "--profile-directory=" + "Mapbot";
                MBLogger.log(profileDirectory);

                if (StringUtils.isNotBlank(profileDirectory)) {
                    options.addArguments("--start-maximized");

                    options.addArguments("--start-maximized", userDataDir, profileDirectory);
                } else {
                    options.addArguments("--start-maximized");
                }
            } catch (Exception ex) {
                MBLogger.warning("Unable to read user-data-dir and profile-directory properties (optional chrome arguments)", ex);
            }

            int debugPort = getPortToUse(9222); // Choose unique port numbers
            MBLogger.log("Chromium debug port being used: " + debugPort);
            options.addArguments("--remote-debugging-port=" + debugPort); // Add new debugging port

            try {
                MBLogger.log("ChromeDriver options: " + options.toString());
                ChromeDriver cd = new ChromeDriver(options);

                try{
                    MBLogger.log("Chrome capabilities:" + cd.getCapabilities());
                }catch (Exception ex){
                    MBLogger.warning("Failed to get Chrome Capabilities", ex);
                }

                return cd;
            } catch (Exception ex) {
                MBLogger.warning("Failed to launch ChromeDriver on attempt " + attempt + " with port " + debugPort, ex);
                if (attempt < 5) {
                    try {
                        Random random = new Random();
                        Thread.sleep(random.nextInt(1000 - 500 + 1) + 500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        MainGUI2.setTextPrompt(au.mapbot.apex.GUI.I18n.t("prompt.chrome"));
        return null;
    }

    public BufferedImage screenShotVideo(){
        // Navigate to the Twitch page
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        if (driver.getWindowHandles().size() > 1) {
            MBLogger.log("Login popup detected. Skipping screenshot until popup is closed.");
            return null; // or wait a few seconds and retry later
        }

        boolean isYoutube = false;
        String videoElement = ".video-player video";
        if (webpage.contains("youtu")){
            isYoutube = true;
            videoElement = "video.video-stream";
        }

        WebElement videoPlayer = null;

        try {
            if (webpage.contains("youtu")){
                videoPlayer = driver.findElement(By.cssSelector(videoElement));
            }else {
                videoPlayer = driver.findElement(By.cssSelector("iframe[src*='twitch.tv']"));
            }
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(videoElement)));
//            videoPlayer = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(videoElement)));
        } catch (TimeoutException e) {
            e.printStackTrace();
            MBLogger.warning("Failed to get video element \"" + videoElement +"\" from browser - timeout: ",e);
            videoPlayer = null;
        }

        if (videoPlayer!= null) {
            if (webpage.contains("youtu") && !isYoutubeStarted) {
                try {
                    WebElement playButton = wait.until(ExpectedConditions.elementToBeClickable(By.className("ytp-large-play-button")));
                    playButton.click();
                    System.out.println("click video to start");
                } catch (Exception ex) {

                    MBLogger.warning("Failed to press start on youtube video",ex);
                    // do nothing if this fails, user will have to manually click
                }

                isYoutubeStarted = true;
            }else {
//                if (!isStreamOffline()) {
//                    try {
//                        if (refreshStreamTimer == null) {
//                            refreshStreamTimer = new Date();
//                        }
//
//                        Date timeNow = new Date();
//                        if (timeNow.getTime() - refreshStreamTimer.getTime() > 240000) { //4 minutes
//                            refreshStreamTimer = timeNow;
//                            videoPlayer.click(); // Click on the iframe to ensure it has focus
//                            Actions actions = new Actions(driver);
//                            actions.sendKeys(Keys.SPACE).perform(); // Send spacebar to toggle play/pause
//                            System.out.println("Successfully pause sending spacebar");
//                            Thread.sleep(500); // sleep 0.5 seconds
//                            actions.sendKeys(Keys.SPACE).perform(); // Send spacebar to toggle play/pause
//                            System.out.println("Successfully play twitch stream sending spacebar");
//                        }
//                    } catch (Exception ex) {
//                        MBLogger.warning("Failed to startStop running video", ex);
//                    }
                    try {
                        // Get console logs
                        List<LogEntry> logs = driver.manage().logs().get(LogType.BROWSER).getAll();
                        for (LogEntry entry : logs) {
                            if (entry.getMessage().contains("PIPELINE_ERROR_DECODE")) {
                                System.out.println("Decode error detected! Reloading...");
                                driver.navigate().refresh();
                                break;
                            }
                        }
                    } catch (Exception e) {
                        MBLogger.warning("Failed to restart stream from decoder error", e);
                    }
//                }
            }

//             Scroll the video player element into view
//            Actions actions = new Actions(driver);
//            actions.moveToElement(videoPlayer).perform();

            org.openqa.selenium.Point location = videoPlayer.getLocation();

            // Take a screenshot of the video player area
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            try {
                BufferedImage fullScreenshot = ImageIO.read(screenshotFile);
                int lX = 0;
                int lXMax = 0;
                if (location.getX()<0){
                    lXMax = Math.abs(location.getX());
                }else{
                    lX = location.getX();
                }
                int lY = 0;
                int lYMax = 0;
                if (location.getY()<0){
                    lYMax = Math.abs(location.getY());
                }else{
                    lY = location.getY();
                }
                BufferedImage gameImg = fullScreenshot.getSubimage(
                        lX, lY, videoPlayer.getSize().getWidth()-lXMax, videoPlayer.getSize().getHeight()-lYMax);

                //remove black bars on side of image because twitch must be 16:9
                int sixteenNineRatioWidth = SnapshotFromImage.getWidthFromHeight16to9AspectRatio(gameImg.getHeight());
                if (gameImg.getWidth() > sixteenNineRatioWidth){
                     return gameImg.getSubimage(
                            ((gameImg.getWidth()-sixteenNineRatioWidth)/2),
                             0,
                             sixteenNineRatioWidth,
                             gameImg.getHeight());
                }
                return gameImg;
            } catch (Exception ex) {
                MBLogger.warning("Failed to read screenshot from web browser",ex);
                return null;
            }finally {
                screenshotFile.delete();
            }
        }
        MBLogger.log("Returned Null from Webdriver - screenShotVideo");
        return null;
    }

    boolean isStreamOffline() {
        try {
            // Locate the Twitch iframe
            WebElement iframe = driver.findElement(By.cssSelector("iframe[src*='twitch.tv']"));
            driver.switchTo().frame(iframe); // Switch to the iframe

            // Check for the offline message
            WebElement offlineMessage = driver.findElement(By.xpath("//div[contains(@class, 'channel-status-info--offline')]//p[contains(text(), 'Offline')]"));
            boolean isOffline = offlineMessage.isDisplayed();

            // Switch back to the main document
            driver.switchTo().defaultContent();

            return isOffline;
        } catch (NoSuchElementException e) {
            driver.switchTo().defaultContent(); // Ensure we exit the iframe even if there's an error
            return false; // Offline message not found, stream is still live
        }
    }
}
