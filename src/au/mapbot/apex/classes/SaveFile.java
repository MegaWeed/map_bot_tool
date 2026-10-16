/*
 * SaveFile
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.classes;

import au.mapbot.apex.util.CircleUtil;
import org.opencv.core.Point;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class SaveFile {

    private int index;
    private String streamName;
    private long durationMilliseconds;
    private Date date;
    private MapEnum map;
    private List<PointList> pointsList;

    private Integer landed;

    private Line dropship;

    private List<Integer> killList;

    private List<String> killClipList;

    private List<DetectedCircle> circleList;

    private int placement;

    private Set<Point> beaconsPoints;
    private Set<Point> rcPoints;
    private Set<Point> evoPoints;

    private String gameType;

    public SaveFile() {
        this.pointsList = new ArrayList<>();
        this.killList = new ArrayList<>();
        this.circleList = new ArrayList<>();
        this.beaconsPoints = new HashSet<>();
        this.rcPoints = new HashSet<>();
        this.evoPoints = new HashSet<>();
        this.killClipList = new ArrayList<>();

        this.streamName = "";
        this.setLanded(0);
    }

    public Line getDropship() {
        return dropship;
    }

    public void setDropship(Line dropship) {
        this.dropship = dropship;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<DetectedCircle> getCircleList() {
        return circleList;
    }

    public void setCircleList(List<DetectedCircle> circleList) {
        this.circleList = circleList;
    }

    public void addZone(final DetectedCircle circle) {
        this.circleList.add(circle);
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public long getDurationMilliseconds() {
        return durationMilliseconds;
    }

    public String getDurationMillisecondsFormatted() {
        return formatDateTime(durationMilliseconds);
    }

    public void setDurationMilliseconds(long durationMilliseconds) {
        this.durationMilliseconds = durationMilliseconds;
    }

    public Date getDate() {
        return date;
    }

    public String getFormattedDateString() {
        SimpleDateFormat formatter = au.mapbot.apex.GUI.I18n.isZh() ? new SimpleDateFormat("yyyy-MM-dd HH:mm") : new SimpleDateFormat("yyyy-MMM-dd HH:mm", Locale.ENGLISH);
        return formatter.format(date);
    }

    public Integer getLanded() {
        return landed;
    }

    public void setLanded(Integer landed) {
        this.landed = landed;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public MapEnum getMap() {
        return map;
    }

    public void setMap(MapEnum map) {
        this.map = map;
    }

    public List<PointList> getPointsList() {
        return pointsList;
    }

    public void setPointsList(List<PointList> pointsList) {
        this.pointsList = pointsList;
    }

    public void addPoint(PointList point) {
        this.pointsList.add(point);
    }

    public List<Integer> getKillList() {
        return killList;
    }

    public void setKillList(List<Integer> killList) {
        this.killList = killList;
    }

    public void addKill(final Integer point) {
        this.killList.add(point);
    }

    public int getPlacement() {
        return placement;
    }

    public void setPlacement(int placement) {
        this.placement = placement;
    }

    public Set<Point> getBeaconsPoints() {
        return beaconsPoints;
    }

    public void setBeaconsPoints(Set<Point> beaconsPoints) {
        this.beaconsPoints = beaconsPoints;
    }

    public Set<Point> getRcPoints() {
        return rcPoints;
    }

    public void setRcPoints(Set<Point> rcPoints) {
        this.rcPoints = rcPoints;
    }

    public Set<Point> getEvoPoints() {
        return evoPoints;
    }

    public void setEvoPoints(Set<Point> evoPoints) {
        this.evoPoints = evoPoints;
    }

    public List<String> getKillClipList() {
        return killClipList;
    }

    public void setKillClipList(List<String> killClipList) {
        this.killClipList = killClipList;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public static String formatDateTime(long durationMilliseconds){
        long fullTotalSeconds = durationMilliseconds/1000;
        long currentMinutes = TimeUnit.SECONDS.toMinutes(fullTotalSeconds) -
                (TimeUnit.SECONDS.toHours(fullTotalSeconds)* 60);
        long currentSeconds = TimeUnit.SECONDS.toSeconds(fullTotalSeconds) -
                (TimeUnit.SECONDS.toMinutes(fullTotalSeconds) * 60);
        String currentSecondsLeadZero= "";
        if (currentSeconds<10){
            currentSecondsLeadZero = "0";
        }
        return currentMinutes + ":" + currentSecondsLeadZero + currentSeconds;
    }
}
