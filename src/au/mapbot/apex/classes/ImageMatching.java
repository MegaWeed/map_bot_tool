/*
 * ImageMatching
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.classes;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

public class ImageMatching {
    private Mat mat;

    private Core.MinMaxLocResult minMaxLocResult;
    private Double score;

    private Point point;
    private int index;

    private org.opencv.core.Point matchLoc;

    public ImageMatching() {

    }
    public ImageMatching(final Mat mat, final int index) {
        this.mat = mat;
        this.index = index;
    }

    public Mat getMat() {
        return mat;
    }

    public void setMat(Mat mat) {
        this.mat = mat;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Point getMatchLoc() {
        return matchLoc;
    }

    public void setMatchLoc(Point matchLoc) {
        this.matchLoc = matchLoc;
    }

    public Core.MinMaxLocResult getMinMaxLocResult() {
        return minMaxLocResult;
    }

    public void setMinMaxLocResult(Core.MinMaxLocResult minMaxLocResult) {
        this.minMaxLocResult = minMaxLocResult;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }
}
