/*
 * BrowserImage
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.classes;

import java.awt.image.BufferedImage;

public class BrowserImage {
    public enum BrowserEnum {
        FIREFOX,
        CHROME;
    }

    public BrowserImage(BufferedImage screenImage, BrowserEnum browser) {
        this.screenImage = screenImage;
        this.browser = browser;
    }

    private BufferedImage screenImage;
    private BrowserEnum browser;

    public BufferedImage getScreenImage() {
        return screenImage;
    }

    public void setScreenImage(BufferedImage screenImage) {
        this.screenImage = screenImage;
    }

    public BrowserEnum getBrowser() {
        return browser;
    }

    public void setBrowser(BrowserEnum browser) {
        this.browser = browser;
    }
}
