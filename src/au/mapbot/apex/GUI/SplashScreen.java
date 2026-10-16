/*
 * SplashScreen
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.GUI;

import au.mapbot.apex.util.ImageUtil;
import au.mapbot.apex.util.VersionUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;

public class SplashScreen {

    static JProgressBar progressBar;
    static int size = 0;
    static JLabel downloadText2;
    static JFrame splash;
    static DownloadTask downloadTask;

    static boolean downloading;
    static String fileURL;

    public static void main(String[] args) throws Exception{
        System.setProperty("OMP_THREAD_LIMIT", "1");

        // Create the splash screen
        splash = new JFrame();
        splash.setIconImage(ImageUtil.getImageFromResource("/mapboticon25.png"));

        JPanel content = new JPanel(new BorderLayout());
        BufferedImage image = ImageUtil.getImageFromResource("/mapboticonBack.png");
        Icon ic = new ImageIcon(image.getScaledInstance(50,50,0));
        JLabel label = new JLabel(ic); // Replace with your logo path
        JLabel loadingLabel = new JLabel(VersionUtil.UPDATE_CHECK_ENABLED ? "Checking for updates..." : "正在启动...", JLabel.CENTER);

        JPanel imagePanel = new JPanel();
        imagePanel.add(label);

        JPanel mainTextPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        JPanel textDownloadPanel = new JPanel(new GridBagLayout());
        JLabel text1 = new JLabel("A new version is available.");
        JLabel text2 = new JLabel("Current version:");
        JLabel text3 = new JLabel("Latest Available version:");
        JLabel text4 = new JLabel("Do you wish to download (" + size +  "MB) an update?");
        JPanel progressPanel = new JPanel(new GridBagLayout());
        JLabel downloadText1 = new JLabel("Mapbot will restart once download is complete");
        downloadText2 = new JLabel("Downloading: (xxMB / " + size);
        progressBar = new JProgressBar(0,100);
        c.gridy = 0;
        progressPanel.add(downloadText1,c);
        c.gridy = 1;
        progressPanel.add(downloadText2,c);
        c.gridy = 2;
        progressPanel.add(progressBar,c);
        progressPanel.setVisible(false);
        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel2 = new JPanel(new BorderLayout());


        JButton jButton1 = new JButton("yes");
        jButton1.addActionListener(s -> {
            textDownloadPanel.setVisible(false);
            buttonPanel.setVisible(false);
            progressPanel.setVisible(true);
            buttonPanel2.setVisible(true);
            mainTextPanel.validate();
            mainTextPanel.repaint();
//            String fileURL = "https://mapbot.gg/Mapbot-0.9.zip"; // Replace with the URL of the file to download
//            String saveDir = "C:/Users/seanf/Downloads";
            String saveDir = System.getProperty("user.dir");
            downloading = true;
            downloadTask = new DownloadTask(fileURL, saveDir);
            downloadTask.execute();
        });
        JButton jButton2 = new JButton("no");
        jButton2.addActionListener(s -> {
            try {
                launchMainApplication(splash);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        JButton jButton3 = new JButton("cancel");
        jButton3.addActionListener(s -> {
            downloading = false;
            if (downloadTask != null && !downloadTask.isDone()) {
                downloadTask.cancel(true);
            }
            textDownloadPanel.setVisible(true);
            buttonPanel.setVisible(true);
            progressPanel.setVisible(false);
            buttonPanel2.setVisible(false);
            mainTextPanel.validate();
            mainTextPanel.repaint();
        });
        buttonPanel2.add(jButton3);
        buttonPanel.add(jButton1, BorderLayout.WEST);
        buttonPanel.add(jButton2, BorderLayout.EAST);
        c.gridy = 0;
        textDownloadPanel.add(text1,c);
        c.gridy = 1;
        textDownloadPanel.add(text2, c);
        c.gridy = 2;
        textDownloadPanel.add(text3,c);
        c.gridy = 4;
        textDownloadPanel.add(text4,c);
        c.gridy = 0;
        mainTextPanel.add(textDownloadPanel,c);
        c.gridy = 0;
        mainTextPanel.add(progressPanel,c);
        c.gridy = 1;
        mainTextPanel.add(buttonPanel,c);
        mainTextPanel.add(buttonPanel2,c);
        buttonPanel2.setVisible(false);
        mainTextPanel.setVisible(false);

        content.add(imagePanel, BorderLayout.NORTH);
        content.add(mainTextPanel, BorderLayout.CENTER);
        content.add(loadingLabel, BorderLayout.SOUTH);

        splash.getContentPane().add(content);
        splash.setSize(300, 250); // Adjust size as needed
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);

        // Check for updates in a separate thread
        new Thread(() -> {
            if (!VersionUtil.UPDATE_CHECK_ENABLED) {
                try {
                    launchMainApplication(splash);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            try {
                double internetLatestVersion = Double.parseDouble(VersionUtil.getLatestVersionNumberFromInternet());
                double currentVersion = Double.parseDouble(VersionUtil.currentVersion);
                if (internetLatestVersion > currentVersion){
                    fileURL = "Mapbot-" + internetLatestVersion + ".zip"; // Replace with your file URL
                    URL url = new URL("https://mapbot.gg/" + fileURL);
                    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                    int responseCode = httpConn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        int contentLength = httpConn.getContentLength();
                        text2.setText("Current version: " + currentVersion);
                        text3.setText("Latest Available version: " + internetLatestVersion);
                        size = (contentLength/1000000);
                        text4.setText("Download and update? (Size: " + size +"MB)");
                        mainTextPanel.setVisible(true);
                        mainTextPanel.validate();
                        mainTextPanel.repaint();
                    }
                }else{
                    try {
                        launchMainApplication(splash);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }catch (Exception ex){
                try {
                    launchMainApplication(splash);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private static void restartApplication(String fileName) {
        // Implement your application restart logic here
        // This is just a placeholder example
        try {
            //String os = System.getProperty("os.name").toLowerCase();
            String scriptPath = System.getProperty("user.dir") + "/patch.bat";

            //change /c before scriptPath to make window stay open after running for debug
            String[] command = { "cmd.exe", "/c", "start", "cmd.exe", "/c", scriptPath, fileName};
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.dir"))); // Set the working directory to the root folder of the application
            System.out.println("UserDir:" + System.getProperty("user.dir"));
            pb.start();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void launchMainApplication(JFrame splash) throws UnsupportedLookAndFeelException, IOException, ParseException, URISyntaxException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        // Close splash screen
        splash.setVisible(false);
        splash.dispose();

        MainGUI2.main(null);
    }

    private static class DownloadTask extends SwingWorker<Void, Void> {
        private String fileURL;
        private String saveDir;

        public DownloadTask(String fileURL, String saveDir) {
            this.fileURL = fileURL;
            this.saveDir = saveDir;
        }

        @Override
        protected Void doInBackground() {
            try {
                downloadFile(fileURL, saveDir);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error downloading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            return null;
        }

        private void downloadFile(String fileURL, String saveDir) throws IOException {
            URL url = new URL("https://mapbot.gg/"+ fileURL);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            // Always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String fileName = "";
                String disposition = httpConn.getHeaderField("Content-Disposition");
                String contentType = httpConn.getContentType();
                int contentLength = httpConn.getContentLength();

                if (disposition != null) {
                    // Extracts file name from header field
                    int index = disposition.indexOf("filename=");
                    if (index > 0) {
                        fileName = disposition.substring(index + 10, disposition.length() - 1);
                    }
                } else {
                    // Extracts file name from URL
                    fileName = fileURL;
                }

                System.out.println("Content-Type = " + contentType);
                System.out.println("Content-Disposition = " + disposition);
                System.out.println("Content-Length = " + contentLength);
                System.out.println("fileName = " + fileName);

                // Opens input stream from the HTTP connection
                BufferedInputStream inputStream = new BufferedInputStream(httpConn.getInputStream());
                String saveFilePath = saveDir + "/" + fileName;

                // Opens an output stream to save into file
                FileOutputStream outputStream = new FileOutputStream(saveFilePath);

                byte[] buffer = new byte[4096];
                int bytesRead = -1;
                long totalBytesRead = 0;
                int percentCompleted = 0;
                long fileSize = contentLength;

                while ((bytesRead = inputStream.read(buffer)) != -1 && downloading) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    percentCompleted = (int) (totalBytesRead * 100 / fileSize);

                    // Updates progress bar
//                    progressBar.setValue(percentCompleted);
                    if (totalBytesRead > 0) {
                        progressBar.setValue(percentCompleted);
                        int amount = (int)totalBytesRead/1000000;
                        downloadText2.setText("Downloading: (" + amount + "MB / " + size + "MB)");
                        downloadText2.validate();
                        downloadText2.repaint();
                        progressBar.validate();
                        progressBar.repaint();
                    }
                    setProgress(percentCompleted);
                }

                outputStream.close();
                inputStream.close();

                httpConn.disconnect();

                System.out.println("File downloaded");
                if (downloading) {
                    restartApplication(fileName);
                }

//                JOptionPane.showMessageDialog(null, "File downloaded to " + saveFilePath, "Message", JOptionPane.INFORMATION_MESSAGE);
            } else {
                httpConn.disconnect();

                System.out.println("No file to download. Server replied HTTP code: " + responseCode);
                try {
                    launchMainApplication(splash);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        protected void done() {
//            restartApplication();
        }
    }
}