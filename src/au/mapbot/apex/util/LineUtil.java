/*
 * LineUtil
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;

import au.mapbot.apex.classes.Line;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class LineUtil {

    public static Line extendLineToEdges(Point p1, Point p2) {
        // Check if the line is vertical (x values are almost the same)
        if (Math.abs(p2.x - p1.x) < 1e-6) {
            // Vertical line, extend from top to bottom
            Point extendedTop = new Point(p1.x, 0);
            Point extendedBottom = new Point(p1.x, 2048);
            return new Line(extendedTop, extendedBottom);
        }

        // Calculate the slope of the line passing through the provided points
        double slope = (double) (p2.y - p1.y) / (p2.x - p1.x);

        // Calculate intersection points with image boundaries
        Point intersectionTop = calculateIntersection(p1, slope, 0);
        Point intersectionBottom = calculateIntersection(p1, slope, 2048);
        Point intersectionLeft = calculateIntersection(p2, slope, 0);
        Point intersectionRight = calculateIntersection(p2, slope, 2048);

        // Find the two intersection points that are within the image bounds
        Point withinBounds1, withinBounds2;
        if (isWithinBounds(intersectionTop) && isWithinBounds(intersectionBottom)) {
            withinBounds1 = intersectionTop;
            withinBounds2 = intersectionBottom;
        } else {
            withinBounds1 = intersectionLeft;
            withinBounds2 = intersectionRight;
        }

        // Create and return the extended line
        return new Line(withinBounds1, withinBounds2);
    }

    private static Point calculateIntersection(Point point, double slope, int x) {
        int y = (int) (point.y + slope * (x - point.x));
        return new Point(x, y);
    }

    private static boolean isWithinBounds(Point point) {
        return point.x >= 0 && point.x <= 2048 && point.y >= 0 && point.y <= 2048;
    }

    public Line getLine(Mat src) {

        int threshold = 35;
        // Convert the image to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // Apply GaussianBlur to reduce noise and improve edge detection
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

        // Apply Canny edge detection
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, threshold, threshold * 3);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
        Imgproc.dilate(edges, edges, kernel);

        // Use the Hough Line Transform to detect lines
        Mat lines = new Mat();
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, 50, 50, 50);

        Mat colorImage = new Mat(src.size(), CvType.CV_8UC3);
        Imgproc.cvtColor(edges, colorImage, Imgproc.COLOR_GRAY2BGR);


        // Analyze the detected lines to find the one that goes from one side to the other

        CircleUtil circleUtil = new CircleUtil();

        List<Line> foundLines = new ArrayList<>();
        for (int i = 0; i < lines.rows(); i++) {
            double[] line = lines.get(i, 0);

            Point startPoint = new Point(line[0], line[1]);
            Point endPoint = new Point(line[2], line[3]);

            if (((startPoint.x == 0 && endPoint.x == src.cols() - 1) ||
                    (endPoint.x == 0 && startPoint.x == src.cols() - 1) ||
                    (startPoint.y == 0 && endPoint.y == src.rows() - 1) ||
                    (endPoint.y == 0 && startPoint.y == src.rows() - 1))
                    && !isCross(startPoint, endPoint, src.cols(), src.rows())) {
                // Draw the line on the original image
                Imgproc.line(colorImage, startPoint, endPoint, new Scalar(0, 255, 0), 3);


                System.out.println("Dropship line found: " + startPoint.x + "," + startPoint.y + "|" + endPoint.x + "," + startPoint.y);
                foundLines.add(new Line(startPoint, endPoint));
            }
        }
        BufferedImage cannyOut = circleUtil.matToBufferedImage(colorImage);

        if (foundLines.size() == 1){
            return foundLines.get(0);
        }else if (foundLines.size() > 1){

            int bestLineIndex = 0;
            double bestLineScore = 0;
            for (int x = 0; x < foundLines.size(); x++){
                double whitenessScore = getLineWhitenessScore(edges, foundLines.get(x));
                if (whitenessScore > bestLineScore){
                    bestLineIndex = x;
                    bestLineScore = whitenessScore;
                }
            }
            return foundLines.get(bestLineIndex);
        }
        return null;
    }

    private boolean isCross(Point startPoint, Point endPoint, int cols, int rows) {
        int  middleX = (rows/2);
        if (endPoint.x == startPoint.x){
            return true;
        }
        int  middleY = (cols/2);
        if (endPoint.y == startPoint.y){
            return true;
        }
        return false;
    }

    private static double getLineWhitenessScore(Mat edges, Line line) {
        List<Point> pointsOnLine = samplePointsOnLine(line.getStart(), line.getEnd());

        int whiteCount = 0;
        for (Point point : pointsOnLine) {
            int x = (int) point.x;
            int y = (int) point.y;
            if (x >= 0 && x < edges.cols() && y >= 0 && y < edges.rows()) {
                double[] pixel = edges.get(y, x);
                // Check if the pixel is white (255 in a binary edge image)
                if (pixel[0] == 255) {
                    whiteCount++;
                }
            }
        }

        // Calculate the whiteness score as a percentage
        return (double) whiteCount / pointsOnLine.size() * 100.0;
    }

    private static List<Point> samplePointsOnLine(Point start, Point end) {
        List<Point> points = new ArrayList<>();
        int numberOfPoints = 100; // Adjust the number of points as needed

        double dx = (end.x - start.x) / (numberOfPoints - 1);
        double dy = (end.y - start.y) / (numberOfPoints - 1);

        for (int i = 0; i < numberOfPoints; i++) {
            double x = start.x + i * dx;
            double y = start.y + i * dy;
            points.add(new Point(x, y));
        }

        return points;
    }
}
