package au.mapbot.apex.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TwitchLogin {

    private Process chromeProcess;
    private volatile boolean isChromeRunning = false;

    public void launchChromeForLogin(Integer id) {

        String userDataDir = System.getProperty("user.dir") + "/Settings/Chrome";
        if (id != null) {
            userDataDir += id.toString();
        }
        String profileDirectory = "Mapbot";

        List<String> command = new ArrayList<>();
        String chromePath = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
        File file = new File(chromePath);
        if (!file.exists()) {
            chromePath = "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe";
            file = new File(chromePath);
        }
        if (!file.exists()) {
            return;
        }
        command.add(chromePath);
        command.add("--user-data-dir=" + userDataDir);
        command.add("--profile-directory=" + profileDirectory);
        command.add("https://www.twitch.tv/login");

        try {
            chromeProcess = new ProcessBuilder(command).start();
            isChromeRunning = true;

            // Start a thread to monitor when the Chrome window closes
            new Thread(() -> {
                try {
                    chromeProcess.waitFor(); // Blocks until Chrome exits
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    isChromeRunning = false;
                    System.out.println("Chrome window closed.");
                }
            }).start();

            System.out.println("Chrome launched successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isChromeWindowOpen() {
        return isChromeRunning;
    }
}
