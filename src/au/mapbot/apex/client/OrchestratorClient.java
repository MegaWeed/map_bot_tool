/*
 * OrchestratorClient
 * Reconstructed from the compiled 0.94 build (the original source was not shipped).
 * Talks to the optional local orchestrator service; every failure is swallowed.
 */
package au.mapbot.apex.client;

import au.mapbot.apex.classes.SaveFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OrchestratorClient {

    private static final String BASE_URL = "http://localhost:8080";
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    public static String createTwitchClip(final String stream) {
        try {
            URL url = new URL(BASE_URL + "/api/createclip?stream=" + URLEncoder.encode(stream, StandardCharsets.UTF_8));
            HttpURLConnection con = open(url, "GET");
            System.out.println("Response Code: " + con.getResponseCode());
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            return response.toString();
        } catch (Exception e) {
            System.out.println("Failed to call orchestrator for createClip, catch and don't fail");
            return "";
        }
    }

    public static void saveToOrchestrator(final SaveFile saveFile) {
        try {
            HttpURLConnection con = open(new URL(BASE_URL + "/api/savegame"), "POST");
            byte[] body = new ObjectMapper().writeValueAsString(saveFile).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = con.getOutputStream()) {
                os.write(body, 0, body.length);
            }
            System.out.println("Response Code: " + con.getResponseCode());
        } catch (Exception e) {
            System.out.println("Failed to call orchestrator for save, catch and don't fail");
        }
    }

    private static HttpURLConnection open(final URL url, final String method) throws java.io.IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
        con.setConnectTimeout(CONNECT_TIMEOUT);
        con.setReadTimeout(READ_TIMEOUT);
        return con;
    }
}
