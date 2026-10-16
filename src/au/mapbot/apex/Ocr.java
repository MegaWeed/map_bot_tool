/*
 * OCR
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex;

import au.mapbot.apex.classes.OCRMatch;
import au.mapbot.apex.util.CircleUtil;
import au.mapbot.apex.util.ImageUtil;
import com.sun.jna.Pointer;
import net.sourceforge.tess4j.*;
import net.sourceforge.tess4j.util.ImageIOHelper;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Ocr {

    Tesseract tesseract;

    Tesseract tesseractEng = new Tesseract();

    Tesseract tesseractJpn = new Tesseract();

    Tesseract tesseractChs = new Tesseract();

    final ITessAPI.TessBaseAPI handle = TessAPI1.TessBaseAPICreate();

    public enum Language {
        ENG,JPN,CHS;
    }


    public static Language currentLang = Language.ENG;

    public Language getCurrentLang() {
        return currentLang;
    }

    public Ocr() {
        tesseractEng.setDatapath("./tessdata");
        tesseractEng.setLanguage("engbest");
        tesseract = tesseractEng;
        tesseractJpn.setDatapath("./tessdata");
        tesseractJpn.setLanguage("jpnbest");
        tesseractChs.setDatapath("./tessdata");
        tesseractChs.setLanguage("chi_simbest");

        //this should hopefully weed out bad results
        //https://stackoverflow.com/questions/31902592/tesseract-choiceiterator-confidence-threshold
//        tesseract.setVariable("matcher_bad_match_pad", "0.40");
//        tesseract.setVariable("debug_file","/dev/null");
//        tesseract.setVariable("save_best_choices", "T");

//        tesseract.setPageSegMode(1);
//        tesseract.setOcrEngineMode(1);

        String datapath = "./tessdata";
        String language = "engbest";
        TessAPI1.TessBaseAPIInit3(handle, datapath, language);
//        TessAPI1.TessBaseAPISetVariable(handle, "tessedit_char_whitelist", "0123456789B");
        TessAPI1.TessBaseAPISetVariable(handle,"debug_file","/dev/null");
        TessAPI1.TessBaseAPISetPageSegMode(handle, ITessAPI.TessPageSegMode.PSM_SINGLE_WORD);
    }

    public void cleanup() {
        tesseract = null; // Allow for garbage collection
        tesseractEng = null; // Allow for garbage collection
        tesseractJpn = null; // Allow for garbage collection
        tesseractChs = null; // Allow for garbage collection
        if (handle != null) {

                TessAPI1.TessBaseAPIDelete(handle);
        }
    }

    public void setLanguageEng(){
        tesseract = tesseractEng;
        currentLang = Language.ENG;
    }

    public void setLanguageJpn(){
        tesseract = tesseractJpn;
        currentLang = Language.JPN;
    }

    public void setLanguageChs(){
        tesseract = tesseractChs;
        currentLang = Language.CHS;
    }

    public OCRMatch ocrWordsGetBest(final BufferedImage textimage) throws IOException {

        // Convert BufferedImage to Mat
        Mat inputImage = ImageUtil.bufferedImage2Mat(textimage);

        int newWidth = (textimage.getWidth() * 2);
        int newHeight = (textimage.getHeight() * 2);
        Imgproc.resize(inputImage, inputImage, new Size(newWidth, newHeight));

        // Convert the input image to grayscale
        Mat grayImage = new Mat();
        Imgproc.cvtColor(inputImage, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Threshold the grayscale image to turn pixels darker than 170 to black
        int thresholdValue = 140;
        OCRMatch best = new OCRMatch("0",0, 0);
        while (thresholdValue <= 220) {
            Mat thresholdedGrayImage = new Mat();
            Imgproc.threshold(grayImage, thresholdedGrayImage, thresholdValue, 255, Imgproc.THRESH_BINARY_INV);

            try {
                BufferedImage imagetoRead = Mat2BufImg(thresholdedGrayImage);
                List<Word> words = tesseract.getWords(imagetoRead, ITessAPI.TessPageIteratorLevel.RIL_WORD);

                if (words.size() > 0) {
                    int confidence = Math.round(words.get(0).getConfidence());
                    if (confidence > best.getConfidence()) {
                        best = new OCRMatch(words.get(0).getText(), confidence, thresholdValue);
                    }

                }
                if (best.getConfidence() > 94){
                    return best;
                }
                thresholdedGrayImage.release();

            } catch (NumberFormatException e) {
                System.err.println("Error while performing OCR: " + e.getMessage());
            }
            thresholdValue += 5;
        }
        inputImage.release();

//        System.out.println("Recognized Number: " + best.getResult() + " | Confidence: " + best.getConfidence() + " | Threshold Found: " + best.getThresholdFound());
        return best;
    }

    public static Mat convertTo3Channels(Mat inputImage) {
        // Check if the input image has only one channel
        if (inputImage.channels() == 1) {
            // Create a new 3-channel Mat
            Mat bgrImage = new Mat(inputImage.rows(), inputImage.cols(), CvType.CV_8UC3);

            // Convert the single-channel image to 3 channels
            Imgproc.cvtColor(inputImage, bgrImage, Imgproc.COLOR_GRAY2BGR);

            return bgrImage;
        }

        // If the input image already has 3 channels, return it as is
        return inputImage;
    }

    public Mat isolateNumberMat(BufferedImage originalImage) throws IOException {
        // Preprocess the image

        Mat src = ImageUtil.bufferedImage2Mat(originalImage);
//        int newWidth = (int) (src.width() * 8);
//        int newHeight = (int) (src.height() * 8);
//        Imgproc.resize(src, src, new Size(newWidth, newHeight));

        Mat grayImage = new Mat();
        Imgproc.cvtColor(convertTo3Channels(src), grayImage, Imgproc.COLOR_BGR2GRAY);
        Mat grayImageBlur = new Mat();
//        Imgproc.GaussianBlur(grayImage, grayImageBlur, new Size(5, 5), 0);

        // Thresholding to enhance contrast
//        Imgproc.threshold(grayImage, grayImage, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        Mat thresholdImage = new Mat();

        Imgproc.threshold(grayImageBlur, thresholdImage, 150, 255, Imgproc.THRESH_BINARY);

        // Find contours
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(thresholdImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Filter contours based on size or other criteria
        OCRMatch bestResult = new OCRMatch("",0,0);
        Mat bestMat = new Mat();
        for (MatOfPoint contour : contours) {
            Rect boundingRect = Imgproc.boundingRect(contour);
            // Add your filtering criteria here
            if (boundingRect.width > 3 && boundingRect.height > 10) {
//                Mat roi = originalImage.submat(boundingRect);
                Mat roi = src.submat(boundingRect);

                // Use Tesseract for OCR
                try {
                    CircleUtil cu = new CircleUtil();
//                    Ocr ocr = new Ocr();
                    BufferedImage image = cu.matToBufferedImage(roi);
                    OCRMatch result = ocrSingle(image,1,false,150,150);
                    if (result.getConfidence() > bestResult.getConfidence()){
                        bestResult = result;
                        bestMat = roi;
                    }
                    System.out.println("Extracted Number: " + result.getResult() + " | Confidence: " + result.getConfidence() + " | Threshold: " + result.getThresholdFound());
                    bestResult.getConfidence();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bestMat;
    }

    public OCRMatch isolateNumberKillImage(BufferedImage originalImage) throws IOException {
        // Preprocess the image

        Mat src = ImageUtil.bufferedImage2Mat(originalImage);
        int newWidth = (int) (src.width() * 8);
        int newHeight = (int) (src.height() * 8);
        Imgproc.resize(src, src, new Size(newWidth, newHeight));

        Mat grayImage = new Mat();
        Imgproc.cvtColor(src, grayImage, Imgproc.COLOR_BGR2GRAY);
        Mat grayImageBlur = new Mat();
        Imgproc.GaussianBlur(grayImage, grayImageBlur, new Size(5, 5), 0);

        // Thresholding to enhance contrast
//        Imgproc.threshold(grayImage, grayImage, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        Mat thresholdImage = new Mat();

        Imgproc.threshold(grayImageBlur, thresholdImage, 150, 255, Imgproc.THRESH_BINARY);

        // Find contours
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(thresholdImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Filter contours based on size or other criteria
        OCRMatch bestResult = new OCRMatch("",0,0);
        for (MatOfPoint contour : contours) {
            Rect boundingRect = Imgproc.boundingRect(contour);
            // Add your filtering criteria here
            if (boundingRect.width > 3 && boundingRect.height > 10) {
//                Mat roi = originalImage.submat(boundingRect);
                Mat roi = src.submat(boundingRect);

                // Use Tesseract for OCR
                try {
                    CircleUtil cu = new CircleUtil();
//                    Ocr ocr = new Ocr();
                    BufferedImage image = cu.matToBufferedImage(roi);
                    OCRMatch result = ocrSingle(image,1,false,150,220);
                    if (result.getConfidence() > bestResult.getConfidence()){
                        bestResult = result;
                    }
                    //System.out.println("Extracted Number: " + result.getResult() + " | Confidence: " + result.getConfidence() + " | Threshold: " + result.getThresholdFound());
                    bestResult.getConfidence();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bestResult;
    }

    public OCRMatch ocrSingle(final BufferedImage textimage, int expectedValue, boolean expectDown, int startThreshold, int endThreshold) throws IOException {
        int thresholdIncrement = 10;

        Date date = new Date();
        // Convert BufferedImage to Mat
        Mat inputImage = ImageUtil.bufferedImage2Mat(textimage);

        int newWidth = (textimage.getWidth() * 2);
        int newHeight = (textimage.getHeight() * 2);
        Imgproc.resize(inputImage, inputImage, new Size(newWidth, newHeight));

        // Convert the input image to grayscale
        Mat grayImage = new Mat();
        Imgproc.cvtColor(convertTo3Channels(inputImage), grayImage, Imgproc.COLOR_BGR2GRAY);

        // Threshold the grayscale image to turn pixels darker than 170 to black
        int thresholdValue = computeHalfwayBrightness(textimage) - 20;
        OCRMatch best = new OCRMatch("0",0, 0);

        //int thresholdValue = startThreshold;
        //int upperThreshold = endThreshold;
        int upperThreshold = thresholdValue + 20;

        while (thresholdValue <= upperThreshold) {
            Mat thresholdedGrayImage = new Mat();
            Imgproc.threshold(grayImage, thresholdedGrayImage, thresholdValue, 255, Imgproc.THRESH_BINARY_INV);

            try {
                BufferedImage imagetoRead = Mat2BufImg(thresholdedGrayImage);
                List<Word> words = tesseractEng.getWords(imagetoRead, ITessAPI.TessPageIteratorLevel.RIL_WORD);
                if (words.size() > 0) {
                    for (Word word: words) {
                        String valueString = word.getText();
                        if (valueString.matches("^[0-9]{1,3}(%?)$")) {
                            int valueInt = Integer.parseInt(valueString);
                            int confidence = Math.round(word.getConfidence());

                            if (expectDown) {
                                if (valueInt <= expectedValue && valueInt >= expectedValue - 3) {
                                    if (confidence >= best.getConfidence()) {
                                        best = new OCRMatch(word.getText(), confidence, thresholdValue);
                                    }
                                }
                            } else {
                                if (valueInt >= expectedValue && valueInt <= expectedValue + 3) {
                                    if (confidence > best.getConfidence()) {
                                        best = new OCRMatch(word.getText(), confidence, thresholdValue);
                                    }
                                }
                            }
                            if (confidence > best.getConfidence() + 10) {
                                best = new OCRMatch(word.getText(), confidence, thresholdValue);
                            }
                        }
                    }
                }
                if (best.getConfidence() > 94){
//                    System.out.println("Recognized Number(single): " + best.getResult() + " | Confidence: " + best.getConfidence() + " | Threshold Found: " + best.getThresholdFound());
//                    System.out.println("Threshold Found (single) (from "+ startThreshold +" to "+ upperThreshold +"): " + thresholdValue + " | average+half brightness: " + computeHalfwayBrightness(textimage));
                    return best;
                }

            } catch (NumberFormatException e) {
                System.err.println("Error while performing OCR: " + e.getMessage());
            }
            thresholdValue += thresholdIncrement;
        }
//        System.out.println("Recognized Number(single): " + best.getResult() + " | Confidence: " + best.getConfidence() + " | Threshold Found: " + best.getThresholdFound());
//        System.out.println("ocrSingle time: " + (new Date().getTime() - date.getTime()));
//        System.out.println("Threshold Found (single) (from "+ startThreshold +" to "+ upperThreshold +"): " + thresholdValue + " | average+half brightness: " + computeHalfwayBrightness(textimage));
        return best;
    }

    public OCRMatch chatGPT(final BufferedImage textimage) throws IOException {

        Date date = new Date();
        // Convert BufferedImage to Mat
        // Convert BufferedImage to Mat
        Mat inputImage = ImageUtil.bufferedImage2Mat(textimage);

        int newWidth = (textimage.getWidth() * 2);
        int newHeight = (textimage.getHeight() * 2);
        Imgproc.resize(inputImage, inputImage, new Size(newWidth, newHeight));

        // Convert the input image to grayscale
        Mat grayImage = new Mat();
        if (inputImage.channels() == 1) {
            grayImage = inputImage.clone(); // Directly use the single-channel image
        } else {
            Imgproc.cvtColor(inputImage, grayImage, Imgproc.COLOR_RGB2GRAY);
        }
        //Imgproc.cvtColor(inputImage, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Threshold the grayscale image to turn pixels darker than 170 to black
//        int averge = computeAverageBrightness(textimage);
//        int thresholdValue = 140;
        int thresholdValue = computeHalfwayBrightness(textimage);
        OCRMatch best = new OCRMatch("0",0, 0);
//        while (thresholdValue <= 220) {
            Mat thresholdedGrayImage = new Mat();
            Imgproc.threshold(grayImage, thresholdedGrayImage, thresholdValue, 255, Imgproc.THRESH_BINARY_INV);

            try {
                BufferedImage imagetoRead = Mat2BufImg(thresholdedGrayImage);
                List<Word> words = tesseractEng.getWords(imagetoRead, ITessAPI.TessPageIteratorLevel.RIL_WORD);

                if (words.size() > 0) {
                    if (words.get(0).getText().matches("^(([0-9])(?))+(%?)$")) {
                        int confidence = Math.round(words.get(0).getConfidence());
                        if (confidence > 92) {
//                            System.out.println("Recognized Number: " + words.get(0).getText() + " | Confidence: " + words.get(0).getConfidence() + " | Threshold: " + thresholdValue);
//                            System.out.println("Threshold Found (chatgpt) (from "+ 140 +" to "+ 220 +"): " + thresholdValue + " | average+half brightness: " + computeHalfwayBrightness(textimage));

                            return new OCRMatch(words.get(0).getText(), confidence, thresholdValue);
                        }
                        if (confidence > best.getConfidence()) {
                            best = new OCRMatch(words.get(0).getText(), confidence, thresholdValue);
                        }
                    }
                }

            } catch (NumberFormatException e) {
                System.err.println("Error while performing OCR: " + e.getMessage());
            }
//            thresholdValue += 10;
//        }
//        System.out.println("Recognized Number: " + best.getResult() + " | Confidence: " + best.getConfidence() + " | Threshold Found: " + best.getThresholdFound());
//        System.out.println("chatGPT time: " + (new Date().getTime() - date.getTime()));
//        System.out.println("Threshold Found (chatgpt) (from "+ 140 +" to "+ 220 +"): " + thresholdValue + " | average+half brightness: " + computeHalfwayBrightness(textimage));
        return best;
    }

//    public static Mat isolateAndPrepareText(Mat grayImage) {
//        Imgproc.cvtColor(grayImage, grayImage, Imgproc.COLOR_BGR2GRAY);
//
//        // Step 2: Apply thresholding to create a binary image
//        Mat binaryImage = new Mat();
//        Imgproc.threshold(grayImage, binaryImage, 180, 255, Imgproc.THRESH_BINARY);
//
//        // Step 3: Use morphological operations to clean up the text and remove noise
//        int kernelSize = 3;
//        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
//        Mat cleanedImage = new Mat();
//        Imgproc.morphologyEx(binaryImage, cleanedImage, Imgproc.MORPH_CLOSE, kernel);
//
//        // Step 4: Invert the binary image for Tesseract OCR
//        Mat invertedImage = new Mat();
//        Core.bitwise_not(cleanedImage, invertedImage);
//
//        // Step 5: Perform contour detection to identify bounding boxes around text regions
//        List<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//        Imgproc.findContours(invertedImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//
//        // Step 6: Filter out small bounding boxes and fill holes
//        Mat finalImage = Mat.zeros(invertedImage.size(), CvType.CV_8UC1);
//        int minAreaThreshold = 50; // Adjust this threshold based on your image size
//        for (MatOfPoint contour : contours) {
//            double contourArea = Imgproc.contourArea(contour);
//            if (contourArea > minAreaThreshold) {
//
//                Imgproc.drawContours(finalImage, List.of(contour), 0, new Scalar(255), -1);
//            }
//        }
//        return invertedImage;
//    }

    // Helper method to convert BufferedImage to Mat
    public static Mat bufferedImageToMat(BufferedImage bi) {
        int width = bi.getWidth();
        int height = bi.getHeight();
        Mat mat = new Mat(height, width, CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    // Helper method to convert OpenCV Mat to BufferedImage
    public static BufferedImage Mat2BufImg(Mat mat) {
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] bytes = new byte[bufferSize];
        mat.get(0, 0, bytes);

        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_BYTE_GRAY);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(bytes, 0, targetPixels, 0, bytes.length);

        return image;
    }


    public String basicProcessOcrScale(final BufferedImage textImage) throws TesseractException {
        return basicProcessOcrScale(textImage, false);
    }
    public String basicProcessOcrScale(final BufferedImage textImage, final Boolean digitsOnly) throws TesseractException {
        Date date = new Date();
        BufferedImage resizedImage = textImage;
        while (resizedImage.getHeight() < 50){
            int newWidth = Math.max(1, (int) (resizedImage.getWidth() * 1.5));
            int newHeight = Math.max(1, (int) (resizedImage.getHeight() * 1.5));
            resizedImage = ImageUtil.resizeBufferedImage(resizedImage, newWidth, newHeight);
        }
        List<String> configs = new ArrayList<>();
        if (digitsOnly) {
            configs.add("digits");
        }
        tesseract.setConfigs(configs);
        final String rawText = tesseract.doOCR(resizedImage);
        String tidyText = rawText.trim().replaceAll(" ","");
        if (digitsOnly) {
            tidyText = tidyText.replaceAll("s","5");
        }
//        System.out.println("basicProcessOcrScale time: " + (new Date().getTime() - date.getTime()));
        return tidyText;
    }

    public int computeAverageBrightness(BufferedImage image) {
        int totalBrightness = 0;
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int brightness = (r + g + b) / 3;
                totalBrightness += brightness;
            }
        }

        double averageBrightness = (double) totalBrightness / (width * height);
        return Double.valueOf(averageBrightness).intValue()+43;
    }

    public int computeHalfwayBrightness(BufferedImage image) {
        int totalBrightness = 0;
        int maxBrightness = 0;
        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate total brightness and find maximum brightness
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int brightness = (r + g + b) / 3;
                totalBrightness += brightness;

                // Update maximum brightness if needed
                if (brightness > maxBrightness) {
                    maxBrightness = brightness;
                }
            }
        }

        // Calculate average brightness
        double averageBrightness = (double) totalBrightness / (width * height);

        // Calculate the halfway point between average and maximum brightness
//        int halfwayBrightness = (int) ((averageBrightness + maxBrightness) / 2);

        //skew further to max brightness
        int halfwayBrightness = (int) ((averageBrightness + maxBrightness + maxBrightness) / 3);

        return halfwayBrightness;
    }

    public String screenText(TextEnum textEnum) {
        return TextEnum.screenTexts(textEnum, currentLang);
    }

    public enum TextEnum {
        SQUADLEFT, PLAYERSLEFT, MAINMENU_1_PLAY, MAINMENU_2_SEASONAL, MAINMENU_3_LEGENDS, MAINMENU_4_LOADOUT, MAINMENU_5_STORE, PLAYER_DEAD_1, PLAYER_DEAD_2, ROUND_CLOSING_pt1, ROUND_CLOSING_pt2, FINAL_ROUND, LOBBY, LOBBY_FINISHED, RANKED, PING, SUGGEST, LAUNCH;
;
        public static String screenTexts(TextEnum textEnum, Language language) {
            switch (language) {
                case ENG:
                    switch (textEnum) {
                        case SQUADLEFT:
                            return "SQUADSLEFT";
                        case PLAYERSLEFT:
                            return "PLAYERSLEFT";
                        case PLAYER_DEAD_1: //season 24 WAS SPECTATE NOW LIVE
                            return "LIVE";
                        case PLAYER_DEAD_2: //season 24 WAS DEATH RECAP NOW SUMMARY
                            return "SUMMARY";
                            //TODO cant tell when whole team dead in season 24
//                        case WHOLE_TEAM_DEAD:
//                            return "SUMMARY";
                        case ROUND_CLOSING_pt1:
                            return "ROUND";
                        case ROUND_CLOSING_pt2:
                            return "CLOSING";
                        case FINAL_ROUND:
                            return "FINAL";
                        case MAINMENU_1_PLAY:
                            return "PLAY";
                        case MAINMENU_2_SEASONAL: //season 22 was SEASONAL now EVENT
                            return "EVENT";
                        case MAINMENU_3_LEGENDS: // season 22 was LEGENDS now CHALLENGES
                            return "CHALLENGES";
                        case MAINMENU_4_LOADOUT: // season 22 was LOADOUT now PASSES
                            return "PASSES";
                        case MAINMENU_5_STORE: // season 22 was STORE now LEGEND LOCKER
                            return "LEGEND LOCKER";
                        case LOBBY:
                            return "LOBBY";
                        case LOBBY_FINISHED:
                            return "MATCH";
                        case RANKED:
                            return "RANKED";
                        case PING:
                            return "PING";
                        case SUGGEST:
                            return "SUGGES";
                        case LAUNCH:
                            return "LAUNCH";

                    }

                case JPN:
                    switch (textEnum) {
                        case SQUADLEFT:
                            return "部隊残存";
                        case PLAYERSLEFT:
                            return "部隊残存"; //TODO need japanese text for solos
                        case PLAYER_DEAD_1:
                            return "ライブ";//住む";
                        case PLAYER_DEAD_2:
                            return "リザルト";//"概要";
//                        case WHOLE_TEAM_DEAD:
//                            return "リザルト";
                        case ROUND_CLOSING_pt1:
                            return "ラウンド";
                        case ROUND_CLOSING_pt2:
                            return "縮小中";
                        case FINAL_ROUND:
                            return "最後の";
                        case MAINMENU_1_PLAY:
                            return "プレイ";
                        case MAINMENU_2_SEASONAL: // was シーズン限定 now EVENT
                            return "イベント";
                        case MAINMENU_3_LEGENDS: // was レジェンド now  CHALLENGES
                            return "チャレンジ";
                        case MAINMENU_4_LOADOUT: //was ロードアウト  now PASSES
                            return "パス";
                        case MAINMENU_5_STORE: //was ストア now LEGEND LOCKER
                            return "レジェンドロッカー";
                        case LOBBY:
                            return "ロビー";
                        case LOBBY_FINISHED:
                            return "マッチ";
                        case RANKED:
                            return "ランクリーグ";
                        case PING:
                            return "ピン";
                        case SUGGEST:
                            return "提案";
                        case LAUNCH:
                            return "出";
                    }

                case CHS:
                    // text taken from the game's localization_schinese file
                    switch (textEnum) {
                        case SQUADLEFT:
                            return "剩余小队";
                        case PLAYERSLEFT:
                            return "剩余玩家";
                        case PLAYER_DEAD_1:
                            return "实况";
                        case PLAYER_DEAD_2:
                            return "总结";
                        case ROUND_CLOSING_pt1:
                            return "回合";
                        case ROUND_CLOSING_pt2:
                            return "即将结束";
                        case FINAL_ROUND:
                            return "最终";
                        case MAINMENU_1_PLAY:
                            return "开始";
                        case MAINMENU_2_SEASONAL:
                            return "活动";
                        case MAINMENU_3_LEGENDS:
                            return "挑战";
                        case MAINMENU_4_LOADOUT:
                            return "通行证";
                        case MAINMENU_5_STORE:
                            return "传奇储物柜";
                        case LOBBY:
                            return "大厅";
                        case LOBBY_FINISHED:
                            return "比赛";
                        case RANKED:
                            return "排位";
                        case PING:
                            return "标记";
                        case SUGGEST:
                            return "建议";
                        case LAUNCH:
                            return "开启";
                    }
            }
            return "";
        }
    }
}
