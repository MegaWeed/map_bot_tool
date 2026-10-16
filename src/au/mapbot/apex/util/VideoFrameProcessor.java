package au.mapbot.apex.util;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VideoFrameProcessor {

    //THIS IS THE ONE
    private final AtomicReference<BufferedImage> currentFrame = new AtomicReference<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean running = true;
    private final String streamUrl;

    public VideoFrameProcessor(final String streamUrl) throws IOException, InterruptedException {
        this.streamUrl = StreamlinkFFMPEGScreenshotUtil.getHlsUrl(streamUrl);
        startFrameProcessingWithGrabber();
    }

    private void startFrameProcessingWithGrabber() {
        new Thread(() -> {
            while (running) {
                try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(streamUrl)) {
                    avutil.av_log_set_level(avutil.AV_LOG_ERROR);

                    // Set pixel format and options
//                    grabber.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // Force standard format
                    grabber.setPixelFormat(avutil.AV_PIX_FMT_NONE);
                    grabber.setFormat("hls");
                    grabber.setVideoCodecName("h264");
                    grabber.setVideoOption("fflags", "discardcorrupt");
                    grabber.setVideoOption("hls_flags", "discardcorrupt");
                    grabber.setVideoOption("skip_frame", "noref");
                    grabber.setOption("flags", "low_delay");
                    grabber.setOption("vf", "format=yuv420p");
                    grabber.setOption("threads", "1");
                    grabber.setOption("pixel_format", "yuvj420p"); // Set pixel format
                    grabber.setOption("colorspace", "bt709"); // Set color space

                    FFmpegLogCallback.set(); // Optional: FFmpeg log callbacks
                    grabber.start();

                    // Log stream properties for debugging
                    System.out.println("Width: " + grabber.getImageWidth() +
                            ", Height: " + grabber.getImageHeight() +
                            ", Frame Rate: " + grabber.getFrameRate() +
                            ", Pixel Format: " + grabber.getPixelFormat());

                    try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                        long lastTime = System.currentTimeMillis();
                        int frameCount = 0;

                        while (running) {
                            Frame frame = grabber.grabImage();
                            if (frame != null) {
                                BufferedImage bufferedImage = converter.convert(frame);
                                if (bufferedImage != null) {
                                    currentFrame.set(bufferedImage);
                                } else {
                                    System.err.println("Frame buffer is null; skipping...");
                                }
                                frameCount++;

                                // Calculate FPS every second
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastTime >= 1000) {
                                    System.out.println("FPS: " + frameCount + " | StreamRate: " + grabber.getFrameRate());
                                    frameCount = 0; // Reset frame count
                                    lastTime = currentTime; // Reset timer
                                }
                                Thread.sleep(10);
                            } else {
                                System.err.println("Frame is null; stream may have issues.");
                                Thread.sleep(100); // Prevent busy looping
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Stream grabber encountered an error: " + e.getMessage());
                    e.printStackTrace();
                    // Retry after a delay
                    try {
                        System.out.println("Retrying stream connection in 5 seconds...");
                        Thread.sleep(5000);
                    } catch (InterruptedException interruptedException) {
                        System.err.println("Retry delay interrupted.");
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }).start();
    }

    public BufferedImage getFrame() {
        lock.readLock().lock();
        try {
            BufferedImage original = currentFrame.get();
            if (original == null) {
                return null; // No frame to copy
            }
            // Create a copy of the BufferedImage
            BufferedImage copy = new BufferedImage(
                    original.getWidth(),
                    original.getHeight(),
                    original.getType()
            );
            copy.getGraphics().drawImage(original, 0, 0, null);
            return copy;
        } finally {
            lock.readLock().unlock();
        }
    }

    public BufferedImage convertFrameToBufferedImage(Frame frame) {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.convert(frame);
    }

    public void close() throws FFmpegFrameGrabber.Exception {
        System.out.println("FrameGrabber Stopped/Released");
        running = false;
    }

    public static void main(String[] args) {
        try {
            String hlsUrl = StreamlinkFFMPEGScreenshotUtil.getHlsUrl("stormen"); // Replace with your HLS URL
//            startFrameProcessingWithGrabber(hlsUrl);
            //commented because of static

            // Example usage: Fetch and process the latest frame every second
            while (true) {
//                BufferedImage frame = getFrame();
//                if (frame != null) {
//                    System.out.println("Current frame captured!");
                    // Process the frame here (e.g., display or analyze it)
//                } else {
                    System.out.println("Waiting for a frame...");
//                }
                Thread.sleep(1000); // Adjust the delay as needed
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
