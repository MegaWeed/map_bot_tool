/*
 * VersionUtil
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;

import au.mapbot.apex.classes.MBLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class VersionUtil {

    public static String currentVersion = "0.94";

    // The original update server is no longer maintained; an update from it would overwrite local changes.
    public static final boolean UPDATE_CHECK_ENABLED = false;

    public static Boolean isLatestVersion(){
        if (!UPDATE_CHECK_ENABLED) {
            return true;
        }
        try {
            String versionFromInternet = getLatestVersionNumberFromInternet();
            if (!versionFromInternet.isEmpty()) {
                if (Double.parseDouble(currentVersion) < Double.parseDouble(versionFromInternet)) {
                    return false;
                }
            }
        }catch(Exception ex){
            MBLogger.warning("Unable to check for new versions", ex);
        }
        return true;
    }

    public static String getLatestVersionNumberFromInternet() {
        if (!UPDATE_CHECK_ENABLED) {
            return "";
        }
        try {
            URL url = new URL("https://www.mapbot.gg/currentVersion.txt");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(url.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                return inputLine;
            }

            in.close();
        }catch (Exception ex){

        }
        return "";
    }
}
