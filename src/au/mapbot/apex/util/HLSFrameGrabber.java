/*
 * HLSFrameGrabber
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.*;

public class HLSFrameGrabber {
    private final String hlsURL;
    private final FFmpegFrameGrabber ffmpegFrameGrabber;
    private BufferedImage currentFrame;
    private final Object frameLock = new Object();
    private boolean running;

    public HLSFrameGrabber(String stream) throws IOException, InterruptedException {
        // Initialize the frame grabber
        hlsURL = StreamlinkFFMPEGScreenshotUtil.getHlsUrl(stream);
        FFmpegLogCallback.set();
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        System.out.println("Creating FrameGrabber");

//        URL url = new URL(hlsURL);
//        URLConnection connection = url.openConnection();
//        InputStream inputStream = connection.getInputStream();
        // Read from the input stream (here, just printing out raw data for demonstration)
//        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

//        ffmpegFrameGrabber = new FFmpegFrameGrabber(inputStream,Integer.MAX_VALUE - 8);
        ffmpegFrameGrabber =  new FFmpegFrameGrabber(hlsURL);
        ffmpegFrameGrabber.setFrameRate(5); // Limit frame rate to 2 FPS
        ffmpegFrameGrabber.setImageWidth(1664);
        ffmpegFrameGrabber.setImageHeight(936);
        ffmpegFrameGrabber.setPixelFormat(avutil.AV_PIX_FMT_BGR24); // Ensure format matches the conversion needs
        //ffmpegFrameGrabber.setNumBuffers(0);
        ffmpegFrameGrabber.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // Use modern format
        ffmpegFrameGrabber.setOption("pixel_format", "yuv420p");
        ffmpegFrameGrabber.setOption("colorspace", "bt709");
        ffmpegFrameGrabber.setOption("color_range", "jpeg");

//        ffmpegFrameGrabber.setOption("fflags", "nobuffer"); // Avoid buffering
//        ffmpegFrameGrabber.setOption("live_start_index", "0"); // Start at the live edge
//        ffmpegFrameGrabber.setOption("seek_target", "keyframe");


        // Start the background frame fetching thread
        running = true;
        startBackgroundFrameFetcher();


    }

    private void startBackgroundFrameFetcher() {
        Thread frameFetcherThread = new Thread(() -> {
            try {
                ffmpegFrameGrabber.start();

                double frameRate = ffmpegFrameGrabber.getFrameRate(); // Get the stream's frame rate
                long targetFrameInterval = (long) (1000 / frameRate); // Target frame interval in milliseconds

                long startTime = System.currentTimeMillis(); // Start time for FPS calculation
                int frameCount = 0; // Frame count within the time interval

                while (running) {
                    long frameStartTime = System.currentTimeMillis(); // Start time for this frame
                    Frame frame = null;
                    try {
                        if (ffmpegFrameGrabber != null) {
                            do {
                                frame = ffmpegFrameGrabber.grabFrame();
                            } while (frame != null && frame.image == null); // Skip non-image frames

                            if (frame != null) {
                                frameCount++; // Increment frame count
//                                System.out.println("Grabbed Frame Timestamp: " + ffmpegFrameGrabber.getTimestamp());
                                BufferedImage newFrame = convertFrameToBufferedImage(frame);
                                synchronized (frameLock) {
                                    currentFrame = newFrame;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace(); // Log error, but continue fetching frames
                    } finally {
                        if (frame != null) {
                            frame.close();
                        }
                    }

                    // Calculate time taken for this frame and adjust sleep duration
                    long frameEndTime = System.currentTimeMillis();
                    long processingTime = frameEndTime - frameStartTime;
                    long sleepTime = targetFrameInterval - processingTime;

                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime); // Sleep for the remaining time to match frame rate
                    }

                    // FPS calculation every second
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime >= 1000) { // 1 second elapsed
                        System.out.println("FPS: " + frameCount + " | FrameRate: " + frameRate);
                        frameCount = 0; // Reset frame count
                        startTime = currentTime; // Reset start time
                    }
                }

                ffmpegFrameGrabber.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.out.println("FrameGrabber fetchThread Started");
        frameFetcherThread.setDaemon(true); // Ensure the thread stops when the program exits
        frameFetcherThread.start();
    }


    private void startBackgroundFrameFetcherRestartPerGrab() {
        Thread frameFetcherThread = new Thread(() -> {
            try {
                while (running) {
                    try {
                        if (ffmpegFrameGrabber != null) {
                            ffmpegFrameGrabber.start();

                            Frame frame = null;
                            do {
                                frame = ffmpegFrameGrabber.grabFrame();

                            } while (frame != null && frame.image == null); // Skip non-image frames

                            if (frame != null) {
                                System.out.println("Grabbed Frame Timestamp: " + (ffmpegFrameGrabber.getTimestamp()));
                                BufferedImage newFrame = convertFrameToBufferedImage(frame);
                                synchronized (frameLock) {
                                    currentFrame = newFrame;
                                }
                            }
                            ffmpegFrameGrabber.stop();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();  // Log error, but continue fetching frames
                    }
                    //only fetch new frame every 300ms
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.out.println("FrameGrabber fetchThread Started");
        frameFetcherThread.setDaemon(true);  // Ensure the thread stops when the program exits
        frameFetcherThread.start();
    }

    public BufferedImage getFrame() {
        synchronized (frameLock) {
//            System.out.println("Retrieve latest frame");
            return currentFrame;
        }
    }

    public void close() throws FFmpegFrameGrabber.Exception {
        if (ffmpegFrameGrabber != null) {
            System.out.println("FrameGrabber Stopped/Released");
            running = false;
            ffmpegFrameGrabber.release();
        }
    }

    public BufferedImage convertFrameToBufferedImage(Frame frame) {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.convert(frame);
    }

    public String getHlsURL() {
        return hlsURL;
    }
}
