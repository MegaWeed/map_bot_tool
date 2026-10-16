/*
 * CustomMaps
 * Lets players replace the built-in map images with their own.
 * Imported images live in ./CustomMaps/<Name>.png (resized to 2048x2048 on import).
 * By default an imported map is only used for display; it is also used for
 * position matching when "CustomMapDetect.<Name>=1" is set in preferences.
 */
package au.mapbot.apex.util;

import au.mapbot.apex.classes.MapEnum;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class CustomMaps {

    public static final int SIZE = 2048;
    public static final File DIR = new File("./CustomMaps");

    /** Maps the player can replace, in GUI order. Storm Point's two enums share one image. */
    public static final MapEnum[] MAPS = {MapEnum.STORMPOINT, MapEnum.WORLDSEDGE, MapEnum.KINGSCANYON,
            MapEnum.OLYMPUS, MapEnum.BROKENMOON, MapEnum.DISTRICT};

    private static final java.util.Map<String, Object[]> CACHE = new java.util.HashMap<>();

    private CustomMaps() {}

    /** File name of the built-in resource, e.g. "Olympus" for /maps/Olympus.png. */
    public static String baseName(MapEnum map) {
        String res = map.getMapResourceString();
        return res.substring(res.lastIndexOf('/') + 1, res.lastIndexOf('.'));
    }

    public static File file(MapEnum map) {
        return new File(DIR, baseName(map) + ".png");
    }

    public static boolean hasCustom(MapEnum map) {
        return file(map).isFile();
    }

    public static boolean useForDetection(MapEnum map) {
        try {
            return "1".equals(SaveFileUtil.getProperty("CustomMapDetect." + baseName(map)));
        } catch (Exception e) {
            return false;
        }
    }

    public static void setUseForDetection(MapEnum map, boolean use) {
        try {
            SaveFileUtil.writeProperty("CustomMapDetect." + baseName(map), use ? "1" : "0");
        } catch (Exception ignored) {
        }
    }

    /**
     * Loads the map image for a map. Uses the player's image when one was imported
     * (for detection only if they opted in), otherwise the built-in one.
     */
    public static BufferedImage load(MapEnum map, boolean forDetection) throws IOException {
        if (hasCustom(map) && (!forDetection || useForDetection(map))) {
            try {
                File f = file(map);
                synchronized (CACHE) {
                    Object[] hit = CACHE.get(f.getPath());
                    if (hit != null && (Long) hit[0] == f.lastModified()) {
                        return (BufferedImage) hit[1];
                    }
                }
                BufferedImage img = ImageIO.read(f);
                if (img != null) {
                    img = normalise(img);
                    synchronized (CACHE) {
                        CACHE.put(f.getPath(), new Object[]{f.lastModified(), img});
                    }
                    return img;
                }
            } catch (IOException ignored) {
                // fall back to the built-in map
            }
        }
        return ImageUtil.getImageFromResource(map.getMapResourceString());
    }

    public static BufferedImage loadBuiltIn(MapEnum map) throws IOException {
        return ImageUtil.getImageFromResource(map.getMapResourceString());
    }

    /** Copies the chosen picture in as the replacement for this map. */
    public static void importImage(MapEnum map, File source) throws IOException {
        BufferedImage img = ImageIO.read(source);
        if (img == null) {
            throw new IOException("unsupported image");
        }
        DIR.mkdirs();
        ImageIO.write(normalise(img), "png", file(map));
    }

    public static void restore(MapEnum map) {
        File f = file(map);
        if (f.isFile()) {
            f.delete();
        }
    }

    /** Scales to 2048x2048 RGBA, the same size and layout as the built-in maps, so tracking coordinates line up. */
    static BufferedImage normalise(BufferedImage img) {
        if (img.getWidth() == SIZE && img.getHeight() == SIZE && img.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            return img;
        }
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, SIZE, SIZE, null);
        g.dispose();
        return out;
    }
}
