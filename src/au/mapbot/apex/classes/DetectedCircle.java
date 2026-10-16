/*
 * DetectedCircle
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.classes;

import org.opencv.core.Point;

import java.util.Objects;

public class DetectedCircle {
    Point center;
    int radius;
    int extendValue;
    double score;
    int zone;
    boolean closing;

    boolean perfect;

    public DetectedCircle(){

    }
    public DetectedCircle(Point center, int radius, double score, int extendValue, int zone, boolean closing) {
        this.center = center;
        this.radius = radius;
        this.score = score;
        this.extendValue = extendValue;
        this.zone = zone;
        this.closing = closing;
        this.perfect = false;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getExtendValue() {
        return extendValue;
    }

    public void setExtendValue(int extendValue) {
        this.extendValue = extendValue;
    }

    public int getZone() {
        return zone;
    }

    public void setZone(int zone) {
        this.zone = zone;
    }

    public boolean isClosing() {
        return closing;
    }

    public void setClosing(boolean closing) {
        this.closing = closing;
    }

    public boolean isPerfect() {
        return perfect;
    }


    public void setPerfect(boolean perfect) {
        this.perfect = perfect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetectedCircle that = (DetectedCircle) o;
        return radius == that.radius && center.x == that.center.x && center.y == that.center.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, radius);
    }
}
