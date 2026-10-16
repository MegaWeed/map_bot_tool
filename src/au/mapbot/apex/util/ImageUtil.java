/*
 * ImageUtil
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ImageUtil {


    public static BufferedImage getImageFromResource(final String path) throws IOException {
        return ImageIO.read(Objects.requireNonNull(ImageUtil.class.getResourceAsStream(path)));
    }
    public static Mat bufferedImage2Mat(final BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED);
    }

    public static Mat bufferedImage2MatGrey(final BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_GRAYSCALE);
    }

    public static Mat resizeMat(final Mat mat, final double width, final double height){
        Mat resizeimage = new Mat();
        Size sz = new Size(width,height);
        Imgproc.resize(mat, resizeimage, sz );
        return resizeimage;
    }

//    public static BufferedImage resizeBufferedImage(BufferedImage img, int newW, int newH) {
//        if (newW <= 0 || newH <= 0) {
//            System.out.println("Resize Image or height non value");
//            return img;
//        }
//
//        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
//        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
//
//        Graphics2D g2d = dimg.createGraphics();
//        g2d.drawImage(tmp, 0, 0, null);
//        g2d.dispose();
//
//        return dimg;
//    }

    public static BufferedImage resizeBufferedImage(BufferedImage img, int newW, int newH) {
        if (newW <= 0 || newH <= 0) {
            System.out.println("Resize Image width or height invalid");
            return img;
        }

        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dimg.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(img, 0, 0, newW, newH, null);
        g2d.dispose();

        return dimg;
    }

    public static BufferedImage rotateAndCrop(BufferedImage src, int angle){

        // Calculate the new dimensions of the rotated image
        int width = src.getWidth();
        int height = src.getHeight();
        double angleRadians = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(angleRadians));
        double cos = Math.abs(Math.cos(angleRadians));
        int newWidth = (int) Math.floor(width * cos + height * sin);
        int newHeight = (int) Math.floor(height * cos + width * sin);

        // Create a new image to hold the rotated result
        BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, src.getType());
        Graphics2D g2d = rotatedImage.createGraphics();

        // Perform the rotation
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - width) / 2, (newHeight - height) / 2);
        at.rotate(angleRadians, width / 2.0, height / 2.0);
        g2d.drawRenderedImage(src, at);
        g2d.dispose();

        // Calculate the size of the largest square that fits within the rotated image
        int squareSize = (int) (width / Math.sqrt(2));

        // Calculate the coordinates to crop the centered square subimage
        int x = (newWidth - squareSize) / 2;
        int y = (newHeight - squareSize) / 2;

        // Crop the centered square subimage
        return rotatedImage;
//        return rotatedImage.getSubimage(x, y, squareSize, squareSize);

    }


    public static BufferedImage rotateImage(BufferedImage inputImage, int degrees) {
        double radians = Math.toRadians(degrees);

        // Calculate the center point of the image
        int centerX = inputImage.getWidth() / 2;
        int centerY = inputImage.getHeight() / 2;

        // Create an AffineTransform and apply rotation
        AffineTransform transform = new AffineTransform();
        transform.rotate(radians, centerX, centerY);

        // Create a new BufferedImage to hold the rotated image
        BufferedImage rotatedImage = new BufferedImage(
                inputImage.getWidth(),
                inputImage.getHeight(),
                inputImage.getType()
        );

        // Get the graphics context of the new image and draw the rotated image
        Graphics2D g = rotatedImage.createGraphics();
        g.setTransform(transform);
        g.drawImage(inputImage, 0, 0, null);
        g.dispose();

        return rotatedImage;
    }

    public static BufferedImage getNonTransparentSubimage(BufferedImage originalImage) {
        boolean found = false;
        int i = 0;
        int x = 0;
        int y = 0;
        int xx = originalImage.getWidth()-1;
        int yy = originalImage.getHeight()-1;
        while (!found && i < originalImage.getHeight()/2){
            if (isTransparent(originalImage,x,y) || isTransparent(originalImage,x,yy) || isTransparent(originalImage,xx,y) || isTransparent(originalImage,xx,yy)){
                //has a transparent
                i++;
                x++;
                y++;
                xx--;
                yy--;
            }else{
                found = true;
            }

        }
        int width = originalImage.getWidth() - x - (originalImage.getWidth()-xx);
        int height = originalImage.getHeight() - y - (originalImage.getHeight()-yy);

        return originalImage.getSubimage(x,y,width,height);
    }

    private static boolean isTransparent(BufferedImage image, int x, int y) {
        int pixel = image.getRGB(x, y);
        return (pixel >> 24) == 0; // Check if the alpha channel is 0 (transparent)
    }


    public static BufferedImage reduceNoiseToBlackWhite(final BufferedImage source, int threshold){
        BufferedImage myBWImage3 = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < source.getWidth(); x++) {
            for (int y = 0; y < source.getHeight(); y++) {
                if (rgbToGray(source.getRGB(x, y), MODE.LIGHTNESS) > threshold) {
                    myBWImage3.setRGB(x, y, 0);
                } else {
                    myBWImage3.setRGB(x, y, 0xffffff);
                }
            }
        }
        return myBWImage3;
    }

    private enum MODE {
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
