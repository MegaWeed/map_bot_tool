/*
 * SaveFileUtil
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;

import au.mapbot.apex.classes.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.opencv.core.Point;

import java.io.*;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class SaveFileUtil {

    private static InputStream getFileFromResourceAsStream(String fileName) {

        // The class loader that loaded the class
        ClassLoader classLoader = SaveFileUtil.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

    public static List<SaveFile> parseFile() throws IOException, ParseException, URISyntaxException {

        final String[] HEADERS = {"duration","date","map","stream","points","kills","placement","zones","landpoint","beacons","ringConsoles","evos","killClips","gameType"};

        File userHome = new File("./Saves");
        File propertiesFile = new File(userHome, "saves.csv");
//        ClassLoader classLoader = SaveFileUtil.class.getClassLoader();
        FileInputStream inputStream = new FileInputStream(propertiesFile);
//        InputStream inputStream = classLoader.getResourceAsStream(propertiesFile);
//        InputStream is = getFileFromResourceAsStream("./Saves/saves.csv");
        final Reader in = new InputStreamReader(inputStream);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .setSkipHeaderRecord(true)
                .build();

        final Iterable<CSVRecord> records = csvFormat.parse(in);

        final List<SaveFile> saveFile = new ArrayList<>();
        int index = 0;
        for (CSVRecord record : records) {
            final SaveFile save = new SaveFile();
            save.setDurationMilliseconds(Integer.parseInt(record.get("duration")));
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
            save.setDate(formatter.parse(record.get("date")));
            save.setMap(MapEnum.parseFromDisplayNameString(record.get("map")));
            final String stream = record.get("stream");
            if (StringUtils.isNotBlank(stream)){
                save.setStreamName(stream);
            }
            final String points = record.get("points");

            final List<String> pointsList = Arrays.asList(points.split(","));
            for (int i = 0; i < pointsList.size(); i++){
                String s = pointsList.get(i);
                final String[] pointSplit = s.split("-");
                final PointList point = new PointList();
                point.setPoint(new Point(Double.parseDouble(pointSplit[1]), Double.parseDouble(pointSplit[2])));
                point.setTimeInMatch(Integer.parseInt(pointSplit[0]));
                save.getPointsList().add(point);
            }
            try {
                final String landpointString = record.get("landpoint");
                if (landpointString != null) {
                    String[] x;
                    String[] semi = landpointString.split(";");
                    String[] dash = landpointString.split("-");

                    if (semi.length==3){
                        x = semi;
                    }else{
                        if (dash.length==3){
                            x = dash;
                        }else{
                            x = semi;
                        }
                    }

                    if (x.length == 3){
                        Integer landPoint = Integer.parseInt(x[0]);
                        save.setLanded(landPoint);
                        String[] start = x[1].split(",");
                        Point startPoint = new Point(Double.parseDouble(start[0]), Double.parseDouble(start[1]));
                        String[] end = x[2].split(",");
                        Point endPoint = new Point(Double.parseDouble(end[0]), Double.parseDouble(end[1]));
                        save.setDropship(new Line(startPoint, endPoint));
                    }else{
                        Integer landPoint = Integer.parseInt(landpointString);
                        save.setLanded(landPoint);
                    }
                }
            }catch (Exception ex){
            }
            final String kills = record.get("kills");
            final List<String> killList = Arrays.asList(kills.split(","));
            for (final String s : killList){

                if (StringUtils.isNotBlank(s)) {
                    save.addKill(Integer.parseInt(s));
                }
            }
            save.setPlacement(Integer.parseInt(record.get("placement")));

            //need to try catch to support older versions
            try {
                final String zones = record.get("zones");
                final List<String> circleList = Arrays.asList(zones.split(","));
                for (final String s : circleList) {

                    if (StringUtils.isNotBlank(s)) {
                        List<String> x = Arrays.asList(s.split("-"));
                        DetectedCircle circle
                                = new DetectedCircle(new Point(Double.parseDouble(x.get(1)), Double.parseDouble(x.get(2))),
                                Integer.parseInt(x.get(0)),
                                1,
                                1,
                                1,
                                false);
                        save.addZone(circle);
                    }
                }
            }catch (Exception ex){
            }

            //need to try catch to support older versions
            try {
                final String beacons = record.get("beacons");
                final List<String> beaconList = Arrays.asList(beacons.split(","));
                for (int i = 0; i < beaconList.size(); i++){
                    String s = beaconList.get(i);
                    final String[] beaconSplit = s.split("-");
                    final Point point = new Point(Double.parseDouble(beaconSplit[0]), Double.parseDouble(beaconSplit[1]));
                    save.getBeaconsPoints().add(point);
                }
            }catch (Exception ex){
            }

            //need to try catch to support older versions
            try {
                final String rcs = record.get("ringConsoles");
                final List<String> rcsList = Arrays.asList(rcs.split(","));
                for (int i = 0; i < rcsList.size(); i++){
                    String s = rcsList.get(i);
                    final String[] rcsSplit = s.split("-");
                    final Point point = new Point(Double.parseDouble(rcsSplit[0]), Double.parseDouble(rcsSplit[1]));
                    save.getRcPoints().add(point);
                }
            }catch (Exception ex){
            }

            //need to try catch to support older versions
            try {
                final String evos = record.get("evos");
                final List<String> evosList = Arrays.asList(evos.split(","));
                for (int i = 0; i < evosList.size(); i++){
                    String s = evosList.get(i);
                    final String[] evosSplit = s.split("-");
                    final Point point = new Point(Double.parseDouble(evosSplit[0]), Double.parseDouble(evosSplit[1]));
                    save.getEvoPoints().add(point);
                }
            }catch (Exception ex){
            }

            //need to try catch to support older versions
            try {
                final String killClipString = record.get("killClips");
                final List<String> KillClips = Arrays.asList(killClipString.split(","));
                save.setKillClipList(KillClips);
            }catch (Exception ex){
            }

            //need to try catch to support older versions
            try {
                final String gameTypeString = record.get("gameType");
                save.setGameType(gameTypeString);
            }catch (Exception ex){
            }

            save.setIndex(index);
            index++;
            saveFile.add(save);
        }
        in.close();
        inputStream.close();
        return saveFile;
    }

    public static void saveToFile(final SaveFile saveFile) {

        final SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
        final StringBuilder output = new StringBuilder();
        output.append("\r\n");

        output.append("\"").append(saveFile.getDurationMilliseconds()).append("\","); //duration
        output.append("\"").append(formatter.format(saveFile.getDate())).append("\","); //date
        output.append("\"").append(saveFile.getMap().getText()).append("\","); //map
        output.append("\"").append(saveFile.getStreamName()).append("\","); //stream
        output.append("\""); //points
        for (int i = 0; i < saveFile.getPointsList().size(); i++){
            final Point point = saveFile.getPointsList().get(i).getPoint();
            final long time = saveFile.getPointsList().get(i).getTimeInMatch();
            output.append(time).append("-").append(point.x).append("-").append(point.y);
            if (i==saveFile.getPointsList().size()-1){
                output.append("\"");
            }else{
                output.append(",");
            }
        }
        output.append(",");
        output.append("\""); //kills
        for (int i = 0; i < saveFile.getKillList().size(); i++) {
            final Integer point = saveFile.getKillList().get(i);
            output.append(point);
            if (i == saveFile.getKillList().size() - 1) {
                output.append("\"");
            } else {
                output.append(",");
            }
        }

        if (saveFile.getKillList().isEmpty()){
            output.append("\"");
        }
        output.append(",");
        output.append("\"").append(saveFile.getPlacement()).append("\""); //placement

        output.append(",");
        output.append("\""); //zones
        for (int i = 0; i < saveFile.getCircleList().size(); i++) {
            final DetectedCircle point = saveFile.getCircleList().get(i);
             output.append(point.getRadius()).append("-").append(point.getCenter().x).append("-").append(point.getCenter().y);
            if (i == saveFile.getCircleList().size() - 1) {
                output.append("\"");
            } else {
                output.append(",");
            }
        }
        if (saveFile.getCircleList().isEmpty()){
            output.append("\"");
        }


        output.append(",");
        int landedValue = 0;
        if (saveFile.getLanded() != null){
            landedValue =saveFile.getLanded();
        }
        output.append("\"");
        output.append(landedValue); //landpoint
        if (saveFile.getDropship()!=null){ //dropshipLine
            output.append(";");
            output.append(saveFile.getDropship().getStart().x).append(",").append(saveFile.getDropship().getStart().y);
            output.append(";");
            output.append(saveFile.getDropship().getEnd().x).append(",").append(saveFile.getDropship().getEnd().y);
        }
        output.append("\"");
        output.append(",");
        output.append("\""); //beacons
        if (saveFile.getBeaconsPoints() != null && !saveFile.getBeaconsPoints().isEmpty()) {
            int size = saveFile.getBeaconsPoints().size();
            int i = 0;
            for (Point beaconPoint : saveFile.getBeaconsPoints()) {
                output.append(beaconPoint.x).append("-").append(beaconPoint.y);
                if (i == size - 1) {
                    output.append("\"");
                } else {
                    output.append(",");
                }
                i++;
            }
        }else{
            output.append("\"");
        }
        output.append(",");
        output.append("\""); //ring consoles

        if (saveFile.getRcPoints() != null && !saveFile.getRcPoints().isEmpty()) {
            int size = saveFile.getRcPoints().size();
            int i = 0;
            for (Point rcPoint : saveFile.getRcPoints()) {
                output.append(rcPoint.x).append("-").append(rcPoint.y);
                if (i == size - 1) {
                    output.append("\"");
                } else {
                    output.append(",");
                }
                i++;
            }
        }else{
            output.append("\"");
        }
        output.append(",");
        output.append("\""); //evo juicers

        if (saveFile.getEvoPoints() != null && !saveFile.getEvoPoints().isEmpty()) {
            int size = saveFile.getEvoPoints().size();
            int i = 0;
            for (Point evoPoint : saveFile.getEvoPoints()) {
                output.append(evoPoint.x).append("-").append(evoPoint.y);
                if (i == size - 1) {
                    output.append("\"");
                } else {
                    output.append(",");
                }
                i++;
            }
        }else{
            output.append("\"");
        }

        output.append(",");
        output.append("\""); //kill clips

        if (saveFile.getKillClipList() != null && !saveFile.getKillClipList().isEmpty()) {
            int size = saveFile.getKillClipList().size();
            int i = 0;
            for (String killClip : saveFile.getKillClipList()) {
                output.append(killClip);
                if (i == size - 1) {
                    output.append("\"");
                } else {
                    output.append(",");
                }
                i++;
            }
        }else{
            output.append("\"");
        }
        output.append(",");
        output.append("\""); //gametype
        output.append(saveFile.getGameType());
        output.append("\"");

        File userHome = new File(".");
        File propertiesFile = new File(userHome, "/Saves/saves.csv");

        try (FileOutputStream fileOutputStream = new FileOutputStream(propertiesFile, true)) {
            fileOutputStream.write(output.toString().getBytes());
        } catch (IOException e) {
            System.err.println(e);
        }

//        try (final OutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get("src/main/resources/Saves/saves.csv"), StandardOpenOption.APPEND))) {
//            out.write(output.toString().getBytes());
//        } catch (IOException e) {
//            System.err.println(e);
//        }
    }

    public static List<SaveFile> delete(Date date) throws IOException, ParseException, URISyntaxException {
        try {
            List<SaveFile> saves = parseFile();
            for (SaveFile sf : saves) {
                if (sf.getDate().getTime() == date.getTime()) {

                    File userHome = new File("./Saves");
                    File csvFile = new File(userHome, "saves.csv");

                    // Read the content of the CSV file into a list
                    List<String> lines = new ArrayList<>();
                    try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            lines.add(line);
                        }
                    }

                    // Modify the list by removing lines with the specified timestamp
                    List<String> modifiedLines = new ArrayList<>();
                    final SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);

                    String formattedDateTime = "\"" + formatter.format(date) + "\"";
                    for (String line : lines) {
                        String[] columns = line.split(",");
                        try {
                            String dateValue = columns[1].trim();  // Assuming 'date' column is the second column (index 1)

                            if (!dateValue.equals(formattedDateTime)) {
                                modifiedLines.add(line);
                            } else {
                                saves.remove(sf);
                            }
                        }catch (Exception ex){
                            //probably an already empty line, dont add
                            System.err.println(ex);

                        }
                    }

                    try (FileOutputStream fileOutputStream = new FileOutputStream(csvFile, false)) {
                        for (String line : modifiedLines) {
                            fileOutputStream.write(line.getBytes());
                            fileOutputStream.write(System.lineSeparator().getBytes()); // Add a newline after each entry
                        }
                    } catch (IOException e) {
                        System.err.println(e);
                    }

                }
            }
            return saves;
        }catch (Exception ex){
            System.err.println(ex);
        }
        return null;
    }


    public void log(String string){
    }


    public static int getPropertiesTop() throws IOException {
        File userHome = new File("./Settings");
        File propertiesFile = new File(userHome, "preferences.properties");
        Properties props = new Properties();
        props.load(new FileInputStream(propertiesFile));
        String prop =  props.get("browserFirefoxGameScreenTopPixels").toString();
        return Integer.parseInt(prop);
    }

    public static void writePropertiesTop(int newTop) throws IOException {
        File userHome = new File("./Settings");
        File propertiesFile = new File(userHome, "preferences.properties");
        Properties props = new Properties();
        props.load(new FileInputStream(propertiesFile));
        props.setProperty("browserFirefoxGameScreenTopPixels", String.valueOf(newTop));
        OutputStream out = new FileOutputStream(propertiesFile, false);
        props.store(out,null);
    }

    public static String getProperty(String propName) throws IOException {
        File userHome = new File("./Settings");
        File propertiesFile = new File(userHome, "preferences.properties");
        Properties props = new Properties();
        props.load(new FileInputStream(propertiesFile));
        if (props.containsKey(propName)) {
            return props.getProperty(propName);
        } else {
            // Property doesn't exist, return blank
            return "";
        }
    }


    public static void writeProperty(String propName, String propValue) throws IOException {
        File userHome = new File("./Settings");
        File propertiesFile = new File(userHome, "preferences.properties");
        Properties props = new Properties();
        props.load(new FileInputStream(propertiesFile));
        props.setProperty(propName, propValue);
        OutputStream out = new FileOutputStream(propertiesFile, false);
        props.store(out,null);
    }
}
