/*
 * OCRMatch
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.classes;

public class OCRMatch {

    private String result;
    private int confidence;

    private int thresholdFound;

    public OCRMatch(String result, int confidence, int thresholdFound) {
        this.result = result;
        this.confidence = confidence;
        this.thresholdFound = thresholdFound;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public int getThresholdFound() {
        return thresholdFound;
    }

    public void setThresholdFound(int thresholdFound) {
        this.thresholdFound = thresholdFound;
    }
}
