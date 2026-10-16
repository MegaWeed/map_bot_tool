/*
 * ProcessThread
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex;

import au.mapbot.apex.GUI.MainGUI2;
import au.mapbot.apex.classes.MBLogger;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.io.IOException;

public class ProcessThread extends Thread {

    public ProcessThread(){
        try {
            mainProcess = new MainProcess();
        }catch (Exception ex){
            System.out.println(ex);
        }
    }

    public ProcessThread(String vodStream){
        try {
            vodStreamPassed = vodStream;
            mainProcess = new MainProcess();
        }catch (Exception ex){
            System.out.println(ex);
        }
    }
    public Boolean running = true;
    public MainProcess mainProcess;

    public String vodStreamPassed = "";
    public boolean headless = false;

    public void run() {
        this.setPriority(Thread.MIN_PRIORITY);
        System.out.println(this.getName() + ": New Thread is running...");
        running = true;

        try {
            String streamVodName;
            if (vodStreamPassed.isEmpty()) {
                streamVodName = MainGUI2.streamInputTextField.getText();
            }else{
                streamVodName = vodStreamPassed;
            }
            boolean isLocalGame = streamVodName.isEmpty();
            mainProcess.process(isLocalGame, streamVodName, headless);
        } catch (IOException ex) {
            MBLogger.warning("Main Thread IOException", ex);
            throw new RuntimeException(ex);
        } catch (TesseractException e) {
            MBLogger.warning("Main Thread TesseractException", e);
            throw new RuntimeException(e);
        } catch (AWTException e) {
            MBLogger.warning("Main Thread AWTException", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            MBLogger.warning("Main Thread InterruptedException", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            MBLogger.warning("Main Thread Default Failure: ", e);
            throw new RuntimeException(e);
        }
    }

    public void end() {
        mainProcess.stop();
    }

    public MainProcess.PassData getData(){
        return mainProcess.getData();
    }

    public void setLanguage(MainProcess.Languages language) {
        mainProcess.setManualLanguage(language);
    }

    public void setGameType(String gameTypeOption) {
        mainProcess.setGameType(gameTypeOption);
    }

    public void setStreamAspectRatio(MainProcess.AspectRatios ratio) {
        mainProcess.setStreamAspectRatio(ratio);
    }

    public void setAspectRatio(MainProcess.AspectRatios ratio) {
        mainProcess.setAspectRatio(ratio);
    }

    public void setMinimapRotation(Boolean rotation) {
        mainProcess.setMinimapRotation(rotation);
    }

    public void setMap(String mapOption) {
        mainProcess.setMap(mapOption);
    }
}