/*
 * GameMapExporter
 * Pulls the current map images straight out of the player's Apex install using rsx
 * (https://github.com/r-ex/rsx, run as a separate program), then works out which
 * picture is which map by comparing it with the built-in maps.
 */
package au.mapbot.apex.util;

import au.mapbot.apex.classes.MBLogger;
import au.mapbot.apex.classes.MapEnum;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class GameMapExporter {

    private static final int FEATURE = 64;
    /** Minimum similarity to a built-in map, and how far ahead of the runner-up it must be. */
    private static final double MIN_SCORE = 0.5;
    private static final double MIN_MARGIN = 0.15;

    private GameMapExporter() {}

    public static final class Result {
        public final Map<MapEnum, File> found = new LinkedHashMap<>();
        public final Map<MapEnum, Double> scores = new HashMap<>();
    }

    // ---- locating Apex and rsx ---------------------------------------------

    /** Saved path, else the usual Steam / EA install places. */
    public static File findPaksDir() {
        File saved = savedFile("ApexPaksDir");
        if (saved != null && isPaksDir(saved)) {
            return saved;
        }
        List<File> steamRoots = new ArrayList<>();
        steamRoots.add(new File("C:\\Program Files (x86)\\Steam"));
        steamRoots.add(new File("C:\\Program Files\\Steam"));
        for (char drive = 'C'; drive <= 'Z'; drive++) {
            for (String dir : new String[]{"Steam", "SteamLibrary", "steam", "Program Files (x86)\\Steam", "Games\\Steam"}) {
                steamRoots.add(new File(drive + ":\\" + dir));
            }
        }
        // libraries listed in Steam's own config
        for (File root : new ArrayList<>(steamRoots)) {
            File vdf = new File(root, "steamapps\\libraryfolders.vdf");
            if (vdf.isFile()) {
                try {
                    for (String line : Files.readAllLines(vdf.toPath(), StandardCharsets.UTF_8)) {
                        String t = line.trim();
                        if (t.startsWith("\"path\"")) {
                            String p = t.substring(6).trim().replace("\"", "").replace("\\\\", "\\");
                            steamRoots.add(new File(p));
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }
        for (File root : steamRoots) {
            File paks = new File(root, "steamapps\\common\\Apex Legends\\paks\\Win64");
            if (isPaksDir(paks)) {
                return paks;
            }
        }
        for (String ea : new String[]{"C:\\Program Files\\EA Games\\Apex\\paks\\Win64", "C:\\Program Files (x86)\\Origin Games\\Apex\\paks\\Win64"}) {
            if (isPaksDir(new File(ea))) {
                return new File(ea);
            }
        }
        return null;
    }

    public static boolean isPaksDir(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        String[] rpaks = dir.list((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".rpak"));
        return rpaks != null && rpaks.length > 0;
    }

    /** Saved path, else rsx next to Mapbot in a tools folder. */
    public static File findRsx() {
        File saved = savedFile("RsxPath");
        if (saved != null && saved.isFile()) {
            return saved;
        }
        for (String p : new String[]{"tools\\rsx\\rsx_nogui.exe", "tools\\rsx\\rsx.exe", "rsx\\rsx_nogui.exe", "rsx\\rsx.exe"}) {
            File f = new File(p);
            if (f.isFile()) {
                return f.getAbsoluteFile();
            }
        }
        return null;
    }

    public static void remember(File paksDir, File rsx) {
        try {
            if (paksDir != null) {
                SaveFileUtil.writeProperty("ApexPaksDir", paksDir.getAbsolutePath());
            }
            if (rsx != null) {
                SaveFileUtil.writeProperty("RsxPath", rsx.getAbsolutePath());
            }
        } catch (IOException ignored) {
        }
    }

    private static File savedFile(String key) {
        try {
            String v = SaveFileUtil.getProperty(key);
            return v == null || v.isEmpty() ? null : new File(v);
        } catch (Exception e) {
            return null;
        }
    }

    // ---- export ------------------------------------------------------------

    /**
     * Exports ui images from the game's map packs (then the shared ui packs if some maps are
     * still missing) and picks out the map pictures.
     */
    public static Result exportFromGame(File paksDir, File rsx, Consumer<String> log, Process[] running) throws IOException, InterruptedException {
        File work = new File(CustomMaps.DIR, "_export_tmp");
        deleteTree(work);
        work.mkdirs();
        Result result = new Result();
        try {
            String[] all = paksDir.list((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".rpak") && !n.contains("("));
            if (all == null) {
                all = new String[0];
            }
            Arrays.sort(all);

            // The map ui images (uiia) live in each map's own mp_rr_<map> rpak; try those first.
            String[] knownMaps = {"mp_rr_tropic_island", "mp_rr_desertlands", "mp_rr_canyonlands",
                    "mp_rr_olympus", "mp_rr_divided_moon", "mp_rr_district"};
            List<String> knownPaks = new ArrayList<>();
            List<String> otherMapPaks = new ArrayList<>();
            List<String> uiPaks = new ArrayList<>();
            for (String n : all) {
                String lower = n.toLowerCase(Locale.ROOT);
                if (lower.startsWith("mp_rr_") && !lower.contains("loadscreen")) {
                    boolean known = false;
                    for (String k : knownMaps) {
                        if (lower.startsWith(k)) {
                            known = true;
                            break;
                        }
                    }
                    (known ? knownPaks : otherMapPaks).add(n);
                } else if (lower.startsWith("ui") || lower.startsWith("common")) {
                    uiPaks.add(n);
                }
            }

            List<List<String>> stages = new ArrayList<>();
            stages.add(knownPaks);
            stages.add(otherMapPaks);
            stages.add(uiPaks);
            for (int i = 0; i < stages.size(); i++) {
                List<String> paks = stages.get(i);
                if (paks.isEmpty()) {
                    continue;
                }
                File out = new File(work, "stage" + i);
                out.mkdirs();
                log.accept("rsx: " + paks.size() + " rpak");
                runRsx(rsx, paksDir, paks, out, log, running);
                Result stage = classifyFolder(out, log);
                for (Map.Entry<MapEnum, File> e : stage.found.entrySet()) {
                    Double old = result.scores.get(e.getKey());
                    if (old == null || stage.scores.get(e.getKey()) > old) {
                        result.found.put(e.getKey(), e.getValue());
                        result.scores.put(e.getKey(), stage.scores.get(e.getKey()));
                    }
                }
                if (result.found.size() == CustomMaps.MAPS.length) {
                    break;
                }
            }
            // copy in before the temp folder goes away
            for (Map.Entry<MapEnum, File> e : result.found.entrySet()) {
                CustomMaps.importImage(e.getKey(), e.getValue());
            }
            return result;
        } finally {
            deleteTree(work);
        }
    }

    private static void runRsx(File rsx, File paksDir, List<String> paks, File outDir, Consumer<String> log, Process[] running) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(rsx.getAbsolutePath());
        cmd.add("-nogui");
        cmd.add("-export");
        cmd.add("--exporttypes");
        cmd.add("uiia");
        cmd.add("--exportdir");
        cmd.add(outDir.getAbsolutePath());
        for (String p : paks) {
            cmd.add(new File(paksDir, p).getAbsolutePath());
        }
        MBLogger.log("Running rsx: " + rsx + " on " + paks.size() + " rpak files from " + paksDir);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(rsx.getAbsoluteFile().getParentFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        running[0] = process;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isBlank()) {
                    log.accept(line);
                }
            }
        }
        if (!process.waitFor(60, TimeUnit.MINUTES)) {
            process.destroyForcibly();
        }
        running[0] = null;
        MBLogger.log("rsx exited with " + process.exitValue());
    }

    // ---- recognising map pictures -------------------------------------------

    /** Looks through a folder of exported pictures and picks the best match for each map. */
    public static Result classifyFolder(File folder, Consumer<String> log) throws IOException {
        Map<MapEnum, double[]> refs = new LinkedHashMap<>();
        for (MapEnum map : CustomMaps.MAPS) {
            refs.put(map, feature(CustomMaps.loadBuiltIn(map)));
        }
        Result result = new Result();
        List<File> files = new ArrayList<>();
        collectImages(folder, files, 0);
        for (File f : files) {
            Dimension d = imageSize(f);
            if (d == null || d.width < 1024 || Math.abs(d.width - d.height) > d.width / 50) {
                continue;
            }
            BufferedImage img;
            try {
                img = ImageIO.read(f);
            } catch (Exception e) {
                continue;
            }
            if (img == null) {
                continue;
            }
            double[] feat = feature(img);
            MapEnum best = null;
            double bestScore = -1;
            double second = -1;
            for (Map.Entry<MapEnum, double[]> ref : refs.entrySet()) {
                double s = dot(feat, ref.getValue());
                if (s > bestScore) {
                    second = bestScore;
                    bestScore = s;
                    best = ref.getKey();
                } else if (s > second) {
                    second = s;
                }
            }
            MBLogger.log(String.format("Map picture %s: best %s %.2f, next %.2f", f.getName(), best, bestScore, second));
            if (best != null && bestScore >= MIN_SCORE && bestScore - second >= MIN_MARGIN) {
                Double old = result.scores.get(best);
                if (old == null || bestScore > old) {
                    result.found.put(best, f);
                    result.scores.put(best, bestScore);
                }
                log.accept(f.getName() + " → " + best.getText() + String.format(" (%.2f)", bestScore));
            }
        }
        return result;
    }

    private static void collectImages(File dir, List<File> out, int depth) {
        File[] list = dir.listFiles();
        if (list == null || depth > 8) {
            return;
        }
        for (File f : list) {
            if (f.isDirectory()) {
                collectImages(f, out, depth + 1);
            } else {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".bmp")) {
                    out.add(f);
                }
            }
        }
    }

    private static Dimension imageSize(File f) {
        try (ImageInputStream in = ImageIO.createImageInputStream(f)) {
            if (in == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return new Dimension(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            return null;
        }
    }

    /** 64x64 greyscale, zero mean, unit length: a dot product gives the correlation. */
    static double[] feature(BufferedImage img) {
        BufferedImage small = new BufferedImage(FEATURE, FEATURE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, FEATURE, FEATURE);
        // shrink in steps so large maps average properly
        BufferedImage src = img;
        while (src.getWidth() > FEATURE * 4) {
            int w = src.getWidth() / 2;
            BufferedImage half = new BufferedImage(w, w, BufferedImage.TYPE_INT_RGB);
            Graphics2D hg = half.createGraphics();
            hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            hg.drawImage(src, 0, 0, w, w, null);
            hg.dispose();
            src = half;
        }
        g.drawImage(src, 0, 0, FEATURE, FEATURE, null);
        g.dispose();
        double[] v = new double[FEATURE * FEATURE];
        double mean = 0;
        for (int y = 0; y < FEATURE; y++) {
            for (int x = 0; x < FEATURE; x++) {
                v[y * FEATURE + x] = small.getRaster().getSample(x, y, 0);
                mean += v[y * FEATURE + x];
            }
        }
        mean /= v.length;
        double norm = 0;
        for (int i = 0; i < v.length; i++) {
            v[i] -= mean;
            norm += v[i] * v[i];
        }
        norm = Math.sqrt(norm) + 1e-9;
        for (int i = 0; i < v.length; i++) {
            v[i] /= norm;
        }
        return v;
    }

    private static double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }

    private static void deleteTree(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteTree(c);
            }
        }
        f.delete();
    }
}
