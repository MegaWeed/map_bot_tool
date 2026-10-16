/*
 * StreamlinkFFMPEGScreenshotUtil
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.*;

public class StreamlinkFFMPEGScreenshotUtil {

    public static String getHlsUrl(final String streamname) throws IOException, InterruptedException {
        String twitchUrl = "https://www.twitch.tv/" + streamname;
        String[] command = {"streamlink", "--stream-url", twitchUrl, "best"};
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String hlsUrl = reader.readLine();
        process.waitFor();

        if (hlsUrl != null && !hlsUrl.isEmpty()) {
            return hlsUrl;
        }
        return null;
    }

    public static BufferedImage getScreenImage(final String hlsUrl) throws InterruptedException, IOException, TimeoutException {
        String[] ffmpegCommand = {
                "ffmpeg",
                "-loglevel", "error",  // Add this line to suppress verbose output
                "-an",                // Disable audio
                "-r", "1",              // Limit the input FPS to 1 (saves bandwidth)
                "-i", hlsUrl,
                "-t", "0.1",           // Limit the capture time to 0.1 seconds (100ms)
                "-vframes", "1",        // Capture a single frame
                "-f", "image2pipe",
                "-vcodec", "png",
                "-"
        };
        Process ffmpegProcess = new ProcessBuilder(ffmpegCommand).start();

        // Handle errors from FFMPEG
        new Thread(() -> {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    System.err.println(line); // or log it
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Reading image with timeout
        BufferedImage bufferedImage = readImageWithTimeout(ffmpegProcess.getInputStream());

        // Ensure FFMPEG completes
        int exitCode = ffmpegProcess.waitFor();
//        System.out.println("FFMPEG process exited with code: " + exitCode);

        if (bufferedImage != null) {
//            System.out.println("Screenshot captured and loaded into BufferedImage!");
            return bufferedImage;
        } else {
            System.out.println("Failed to capture the image.");
        }
        return null;
    }

    public static BufferedImage readImageWithTimeout(InputStream ffmpegOutput) throws IOException, TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<BufferedImage> future = executor.submit(() -> {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = ffmpegOutput.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            InputStream imageInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            return ImageIO.read(imageInputStream);
        });

        try {
            return future.get(15, TimeUnit.SECONDS); // 10-second timeout
        } catch (TimeoutException e) {
            future.cancel(true);
            //don't exception
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        } finally {
            executor.shutdownNow();
        }
        return null;
    }
}
