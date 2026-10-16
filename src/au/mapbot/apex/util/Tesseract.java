/*
 * Tesseract
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;

public class Tesseract {
    public static void initTesseract(){
        Loader.load(opencv_java.class);
    }
}
