/*
 * PointList
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.classes;

import org.opencv.core.Point;

public class PointList {

    private Point point;
    private long timeInMatch;

    public PointList() {
    }

    public Point getPoint() {
        return point;
    }

    public Boolean isDeadPoint(){
        if (point!= null){
            if (point.x == 0 && point.y == 0){
                return true;
            }
        }
        return false;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public long getTimeInMatch() {
        return timeInMatch;
    }

    public void setTimeInMatch(long timeInMatch) {
        this.timeInMatch = timeInMatch;
    }
}
