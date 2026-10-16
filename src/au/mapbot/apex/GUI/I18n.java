/*
 * I18n
 * Simple Chinese / English UI text table with live switching.
 * Choice is stored as "UILang" (zh / en) in Settings/preferences.properties.
 */
package au.mapbot.apex.GUI;

import au.mapbot.apex.util.SaveFileUtil;

import javax.swing.*;
import java.util.*;

public final class I18n {

    public enum Lang { ZH, EN }

    private static final Map<String, String[]> TEXT = new HashMap<>();
    private static final List<Runnable> LISTENERS = new ArrayList<>();
    private static Lang lang;

    private I18n() {}

    private static void put(String key, String zh, String en) {
        TEXT.put(key, new String[]{zh, en});
    }

    static {
        // window / menus
        put("app.edition", "社区维护版", "Community Edition");
        put("menu.about", "关于", "About");
        put("menu.about.item", "关于 Mapbot", "About Mapbot");
        put("menu.disclaimer", "免责声明", "Disclaimer");
        put("menu.settings", "设置", "Settings");
        put("menu.twitch", "Twitch 登录", "Twitch Login");
        put("menu.uilang", "界面语言 / Language", "Language / 界面语言");
        put("menu.customMaps", "导入/替换地图…", "Import / replace maps…");
        put("cm.title", "自定义地图", "Custom maps");
        put("cm.fromGame", "从 Apex 游戏导出地图…", "Export maps from Apex…");
        put("cm.fromFolder", "从文件夹识别导入…", "Import from a folder…");
        put("ex.title", "从 Apex 游戏导出地图", "Export maps from Apex");
        put("ex.note", "Mapbot 会调用 rsx 从游戏文件里导出当前版本的地图，自动认出是哪张地图，然后替换进来，并默认用于定位识别。rsx 需要单独下载（github.com/r-ex/rsx/releases），解压后在下面选 rsx_nogui.exe 或 rsx.exe。导出过程可能需要几分钟。",
                "Mapbot runs rsx to export the current maps from the game files, works out which picture is which map, and swaps them in (also used for tracking). rsx is a separate download (github.com/r-ex/rsx/releases); pick rsx_nogui.exe or rsx.exe below. Exporting can take a few minutes.");
        put("ex.paks", "Apex 资源目录（paks\\Win64）", "Apex paks folder (paks\\Win64)");
        put("ex.rsx", "rsx 程序", "rsx program");
        put("ex.browse", "浏览…", "Browse…");
        put("ex.start", "开始导出", "Start export");
        put("ex.stop", "停止", "Stop");
        put("ex.running", "正在导出，请稍候…", "Exporting, please wait…");
        put("ex.badPaks", "这个文件夹里没有 .rpak 文件，请选择 Apex Legends\\paks\\Win64。", "No .rpak files in this folder. Choose Apex Legends\\paks\\Win64.");
        put("ex.badRsx", "请先选择 rsx_nogui.exe 或 rsx.exe。", "Choose rsx_nogui.exe or rsx.exe first.");
        put("ex.done", "完成。已替换：%s", "Done. Replaced: %s");
        put("ex.missing", "未找到：%s", "Not found: %s");
        put("ex.none", "没有找到可用的地图图片。", "No map pictures were found.");
        put("ex.failed", "导出失败：%s", "Export failed: %s");
        put("ex.folderTitle", "选择包含地图图片的文件夹（例如 rsx 的 exported_files）", "Choose a folder with map pictures (for example rsx's exported_files)");
        put("cm.note", "导入的图片会缩放成 2048×2048，替换软件里显示的地图。请使用和游戏内大地图对齐的整张正方形地图（例如用 rsx 从游戏里导出的地图图片），否则轨迹会画偏。",
                "Imported pictures are scaled to 2048×2048 and replace the map shown in Mapbot. Use a full square map aligned with the in-game map (for example one exported from the game with rsx), otherwise paths will be drawn in the wrong place.");
        put("cm.builtin", "内置地图", "Built-in map");
        put("cm.custom", "已替换：%s", "Replaced: %s");
        put("cm.import", "导入图片…", "Import picture…");
        put("cm.restore", "恢复默认", "Restore default");
        put("cm.detect", "也用于定位识别", "Also use for tracking");
        put("cm.detect.tip", "勾选后，记录对局时也会用这张图匹配小地图定位。只在图片与游戏内地图完全对齐、画面风格一致时勾选，否则定位会出错。从下一次开始记录时生效。",
                "When ticked, recording also matches the minimap against this picture. Only tick it if the picture lines up exactly with the in-game map and looks the same, otherwise tracking will fail. Takes effect the next time you start recording.");
        put("cm.openFolder", "打开文件夹", "Open folder");
        put("cm.chooser", "选择 %s 的地图图片", "Choose a map picture for %s");
        put("cm.images", "图片 (png, jpg, bmp)", "Images (png, jpg, bmp)");
        put("cm.notSquare", "这张图片不是正方形（%d×%d），拉伸成正方形后轨迹可能对不上。仍要导入吗？", "This picture is not square (%d×%d). Stretched to a square, paths may not line up. Import anyway?");
        put("cm.failed", "导入失败：%s", "Import failed: %s");
        put("cm.done", "%s 的地图已替换", "Map for %s replaced");
        put("cm.restored", "%s 已恢复为内置地图", "%s restored to the built-in map");
        put("cm.confirmRestore", "确定把 %s 恢复为内置地图吗？导入的图片会被删除。", "Restore %s to the built-in map? The imported picture will be deleted.");

        // dialogs
        put("about.html",
                "基于 Mapbot 0.94 修改，原作者已停止更新。<br>"
                        + "本版本更新了 Apex 新版地图、勘测信标和圈控台位置，加入简体中文游戏界面识别，并修复了本地对局启动报错。<br>"
                        + "自动更新已关闭。<br><br>"
                        + "原作者：Sean Finocchiaro",
                "Based on Mapbot 0.94; the original author no longer updates it.<br>"
                        + "This build updates the Apex maps, survey beacon and ring console locations, adds Simplified Chinese game UI recognition, and fixes the local game startup error.<br>"
                        + "Auto update is disabled.<br><br>"
                        + "Original author: Sean Finocchiaro");
        put("disclaimer.text",
                "本软件及相关人员与 EA / Respawn 没有任何赞助、隶属或背书关系。所有图片、图标和商标归各自所有者所有。"
                        + "Apex Legends 是 EA 的注册商标，Apex 地图图片和图标归 Electronic Arts 所有。EA 和 Respawn 不为本软件背书，也不对其内容负责。",
                "The application and people involved are not sponsored, affiliated or endorsed by EA/Respawn in any way. "
                        + "All images, icons and trademarks belong to their respective owner. Apex Legends is a registered trademark of EA. "
                        + "Apex map images and icons belong to Electronic Arts. Be aware, EA and Respawn do not endorse this application nor are responsible for this applications content.");
        put("eula.title", "最终用户许可协议", "End User License Agreement");
        put("eula.note", "以下为原作者的许可协议原文（英文）。", "");
        put("eula.agree", "我已阅读并同意该许可协议", "I have read and agree to the EULA");
        put("ok", "确定", "Ok");
        put("cancel", "取消", "Cancel");
        put("close", "关闭", "Close");

        // left panel
        put("sort", "排序", "Sort");
        put("sort.0", "日期 ↓ 新到旧", "Date ↓ newest first");
        put("sort.1", "日期 ↑ 旧到新", "Date ↑ oldest first");
        put("sort.2", "击杀 ↑ 少到多", "Kills ↑ fewest first");
        put("sort.3", "击杀 ↓ 多到少", "Kills ↓ most first");
        put("sort.4", "排名 ↑ 1-20", "Placement ↑ 1-20");
        put("sort.5", "排名 ↓ 20-1", "Placement ↓ 20-1");
        put("sort.6", "时长 ↑ 短到长", "Duration ↑ shortest first");
        put("sort.7", "时长 ↓ 长到短", "Duration ↓ longest first");
        put("btn.delete", "删除", "Delete");
        put("btn.delete.tip", "删除当前选中的对局记录", "Delete the selected recorded game");
        put("btn.export", "导出", "Export");
        put("btn.export.tip", "把当前地图保存为图片，放在程序目录的 Export 文件夹", "Saves current map to the Export folder next to the exe");
        put("btn.eventlog", "事件日志", "Event Log");
        put("section.display", "地图显示", "Map display");
        put("chk.flight", "跳伞轨迹", "Flight trails");
        put("chk.zones", "缩圈", "Zones");
        put("chk.lines", "移动路线", "Paths");
        put("chk.icons", "信标/圈控台", "Beacons/Consoles");
        put("history.line1", "击杀 %d  |  排名 %d", "KILLS: %d  |  PLACEMENT: %d");
        put("history.time", "时长 %s", "TIME: %s");
        put("history.stream", "  |  直播 %s", "  |  STREAM: %s");

        // stats bar
        put("stats.date", "对局时间：%s", "Game Date: %s");
        put("stats.na", "无", "N/A");
        put("stats.kills", "击杀：%d", "Kills: %d");
        put("stats.placement", "排名：%d", "Placement: %d");
        put("stats.live", " 实时记录中", " LIVE");

        // right panel
        put("section.minimap", "最新小地图", "Latest minimap");
        put("section.session", "识别设置", "Detection settings");
        put("section.game", "本局设置", "This game");
        put("lbl.rotation", "小地图旋转", "Minimap rotation");
        put("lbl.aspect", "游戏画面比例", "Aspect ratio");
        put("lbl.streamAspect", "直播画面比例", "Stream aspect ratio");
        put("lbl.gameLang", "游戏语言", "Game language");
        put("lbl.gameType", "模式", "Game type");
        put("lbl.map", "地图", "Map");
        put("opt.auto", "自动识别", "Auto detect");
        put("opt.off", "关", "Off");
        put("opt.on", "开", "On");
        put("opt.lang.en", "英文", "English");
        put("opt.lang.jp", "日文", "Japanese");
        put("opt.lang.zh", "简体中文", "Simplified Chinese");
        put("opt.type.ranked", "排位赛", "Ranked");
        put("opt.type.unranked", "匹配赛", "Unranked");
        put("opt.type.solo", "单人匹配", "Unranked Solos");
        put("opt.type.algs", "ALGS/自定义", "ALGS/Customs");
        put("map.Storm Point", "风暴点", "Storm Point");
        put("map.World's Edge", "世界尽头", "World's Edge");
        put("map.Kings Canyon", "诸王峡谷", "Kings Canyon");
        put("map.Olympus", "奥林匹斯", "Olympus");
        put("map.Broken Moon", "残月", "Broken Moon");
        put("map.E-District", "电力区域", "E-District");
        put("hint.auto", "选“自动识别”时，Mapbot 会根据画面自动判断。", "With \"Auto detect\", Mapbot works these out from the screen.");

        // bottom bar
        put("lbl.stream", "直播/回放链接", "Stream/VOD URL");
        put("stream.tip", "填入 Twitch 直播或回放链接后点“开始记录”；留空则记录本机正在玩的对局", "Enter a Twitch stream/VOD URL then press Start; leave blank to record your local game");
        put("stream.placeholder", "留空 = 记录本机对局", "Blank = record local game");
        put("btn.start", "开始记录", "Start recording");
        put("btn.stop", "停止", "Stop");
        put("btn.start.tip", "链接为空时记录本机 Apex 对局，否则记录填入的直播/回放", "Records the local Apex game if the URL is blank, otherwise the stream/VOD");

        // prompts
        put("prompt.loginBusy", "录制运行中，无法登录 Twitch", "Cannot login while chrome capture is running");
        put("prompt.startBusy", "Twitch 登录窗口打开时无法开始记录", "Cannot start while twitch login chrome window is running");
        put("prompt.saved", "已导出到：%s", "Saved export to: %s");
        put("prompt.chrome", "错误：使用直播/回放记录需要先安装 Chrome", "Error: Chrome must be installed to use Stream/VOD capture");

        // export image
        put("export.stats", "击杀：%d | 排名：%d", "Kills: %d | Placement: %d");

        // welcome screen
        put("intro.title", "欢迎使用 Mapbot", "Welcome to Mapbot");
        put("intro.body",
                "如果你在玩 Apex，启动游戏后点下方的“开始记录”即可。\n\n"
                        + "如果要记录直播或回放，把链接粘贴到下方输入框，再点“开始记录”。\n\n"
                        + "记录完成的对局会出现在左侧列表里，可以随时回看。\n\n"
                        + "界面语言可以在“设置 → 界面语言”里切换。",
                "To get started, launch Apex and press \"Start recording\" below.\n\n"
                        + "To capture a stream/VOD, paste its URL into the field below, then press \"Start recording\".\n\n"
                        + "Recorded games appear in the list on the left once completed, for you to review at any time.\n\n"
                        + "The interface language can be changed under Settings → Language.");
    }

    public static Lang get() {
        if (lang == null) {
            String saved = "";
            try {
                saved = SaveFileUtil.getProperty("UILang");
            } catch (Exception ignored) {
            }
            if ("en".equalsIgnoreCase(saved)) {
                lang = Lang.EN;
            } else if ("zh".equalsIgnoreCase(saved)) {
                lang = Lang.ZH;
            } else {
                lang = "zh".equals(Locale.getDefault().getLanguage()) ? Lang.ZH : Lang.EN;
            }
        }
        return lang;
    }

    public static boolean isZh() {
        return get() == Lang.ZH;
    }

    public static void set(Lang newLang) {
        if (newLang == get()) {
            return;
        }
        lang = newLang;
        try {
            SaveFileUtil.writeProperty("UILang", newLang == Lang.ZH ? "zh" : "en");
        } catch (Exception ignored) {
        }
        Runnable apply = () -> {
            for (Runnable r : new ArrayList<>(LISTENERS)) {
                r.run();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            apply.run();
        } else {
            SwingUtilities.invokeLater(apply);
        }
    }

    /** Runs r now and again every time the language changes. */
    public static void onChange(Runnable r) {
        LISTENERS.add(r);
        r.run();
    }

    public static String t(String key) {
        String[] v = TEXT.get(key);
        if (v == null) {
            return key;
        }
        return v[isZh() ? 0 : 1];
    }

    public static String f(String key, Object... args) {
        return String.format(t(key), args);
    }

    /** Display name of a map given its MapEnum text. */
    public static String map(String mapText) {
        if (mapText == null || mapText.isEmpty()) {
            return t("opt.auto");
        }
        String[] v = TEXT.get("map." + mapText);
        return v == null ? mapText : v[isZh() ? 0 : 1];
    }

    // ---- event log ------------------------------------------------------

    private static final String[][] EVENT_PREFIX = {
            {"Language manually set to: ", "游戏语言已手动设为："},
            {"Minimap rotation manually set to: ", "小地图旋转已手动设为："},
            {"Map manually set to: ", "地图已手动设为："},
            {"Stream Ratio manually set to: ", "直播画面比例已手动设为："},
            {"Recording locally played game of apex", "正在记录本机 Apex 对局"},
            {"Using VOD/Stream: ", "正在记录直播/回放："},
            {"Resetting per game values", "重置本局数据"},
            {"resetting internal values if not manually set in UI", "重置未手动设置的识别值"},
            {"aspect ratio not manually set, resetting detected value", "画面比例未手动设置，重置识别值"},
            {"stream ratio not manually set, resetting detected value", "直播画面比例未手动设置，重置识别值"},
            {"minimap rotation not manually set, resetting detected value", "小地图旋转未手动设置，重置识别值"},
            {"gameType not manually set, resetting detected value", "模式未手动设置，重置识别值"},
            {"Image aspectRatio detected as ", "识别到画面比例："},
            {"Could not detect a valid aspectRatio from: ", "无法识别画面比例，尺寸："},
            {"Detected currently on Main Menu", "当前在主菜单"},
            {"Detected currently on Lobby", "当前在大厅"},
            {"Detected map being played in Lobby or champ select: ", "在大厅/选人界面识别到地图："},
            {"Map set by GUI: ", "地图由界面设置："},
            {"Player in dropship, game has started", "玩家已在运输船上，对局开始"},
            {"Ranked Game has started", "排位赛开始"},
            {"Unranked Game has started", "匹配赛开始"},
            {"UnRanked Game has started", "匹配赛开始"},
            {"Ranked game detected", "识别为排位赛"},
            {"Unranked game detected", "识别为匹配赛"},
            {"Solo's game mode detected", "识别为单人模式"},
            {"English Language Detected", "识别到英文游戏界面"},
            {"Japanese Language Detected", "识别到日文游戏界面"},
            {"Chinese Language Detected", "识别到中文游戏界面"},
            {"Stream content is in 16:9", "直播画面为 16:9"},
            {"Stream content is in 16:10", "直播画面为 16:10（被拉伸为 16:9）"},
            {"Stream content is in 4:3", "直播画面为 4:3（被拉伸为 16:9）"},
            {"minimap Rotation is on", "小地图旋转：开"},
            {"minimap Rotation is off", "小地图旋转：关"},
            {"minimap zoom level detected at: ", "小地图缩放级别："},
            {"minimap zoom reset", "小地图缩放已重置"},
            {"Minimap shown on screen", "画面中出现小地图"},
            {"No Minimap shown on screen", "画面中没有小地图"},
            {"Minimap Zone Data Found", "从小地图读到缩圈数据"},
            {"Map Zone Data Found", "从大地图读到缩圈数据"},
            {"Zone Data Updated from MiniMap Data: ", "缩圈数据已从小地图更新："},
            {"Zone Data Updated from Map Data: ", "缩圈数据已从大地图更新："},
            {"map detected as: ", "识别到地图："},
            {"Need to collect more map data to detect map being played", "需要更多画面数据来识别地图"},
            {"Map being played unknown, cannot get location until map recognised", "地图未识别，识别前无法定位"},
            {"Player location detected: ", "玩家位置："},
            {"Spectating Player location detected: ", "观战玩家位置："},
            {"Low confidence, not using data point ", "置信度低，舍弃该点 "},
            {"Outlier point removed", "已移除异常点"},
            {"Player has landed", "玩家已落地"},
            {"Player has Landed", "玩家已落地"},
            {"Player has been respawned", "玩家已复活"},
            {"Player Died, Will still get zone data while spectating", "玩家阵亡，观战期间仍会记录缩圈"},
            {"Whole Team Dead, Will still get zone data while spectating", "全队阵亡，观战期间仍会记录缩圈"},
            {"Squads Left changed from: ", "剩余小队："},
            {"Round changed from: ", "回合变化："},
            {"Ring is Closing", "圈正在缩小"},
            {"Ring 5 is closing", "第 5 圈正在缩小"},
            {"Ring 4 is closing", "第 4 圈正在缩小"},
            {"Ring 3 is closing", "第 3 圈正在缩小"},
            {"Ring 2 is closing", "第 2 圈正在缩小"},
            {"Ring 1 is closing", "第 1 圈正在缩小"},
            {"YOU ARE THE CHAMPION", "你是捍卫者！"},
            {"Game has ended", "对局结束"},
            {"Detected champ select screen, previous game must have ended", "检测到选人界面，上一局已结束"},
            {"Saving game results to file", "正在保存对局结果"},
            {"saving completed game", "正在保存已完成的对局"},
            {"Saving Game", "正在保存对局"},
            {"Saving Complete", "保存完成"},
            {"Kill clip:", "击杀片段："},
    };

    private static final String[][] EVENT_INFIX = {
            {" (detected by match time)", "（按对局时间判断）"},
            {" with confidence score: ", "，置信度："},
            {" with score: ", "，得分："},
            {" to final round", " → 最终回合"},
            {", defaulting to 16:9", "，默认 16:9"},
            {" (location to far from previous point), Score:", "（离上一个点太远），得分："},
            {" (and squished to be shown in 16:9 by stream)", ""},
            {" to ", " → "},
    };

    /** Translates an English event-log line for display; logs on disk stay English. */
    public static String event(String english) {
        if (!isZh() || english == null) {
            return english;
        }
        // keep a leading "mm:ss - " game time prefix
        String prefix = "";
        String body = english;
        if (body.length() > 8 && body.charAt(2) == ':' && body.startsWith(" - ", 5)) {
            prefix = body.substring(0, 8);
            body = body.substring(8);
        }
        String best = null;
        String bestZh = null;
        for (String[] p : EVENT_PREFIX) {
            if (body.startsWith(p[0]) && (best == null || p[0].length() > best.length())) {
                best = p[0];
                bestZh = p[1];
            }
        }
        if (best == null) {
            return english;
        }
        String rest = body.substring(best.length());
        for (String[] p : EVENT_INFIX) {
            rest = rest.replace(p[0], p[1]);
        }
        rest = rest.replace("STORMPOINT", t("map.Storm Point"))
                .replace("Storm Point", t("map.Storm Point"))
                .replace("World's Edge", t("map.World's Edge"))
                .replace("Kings Canyon", t("map.Kings Canyon"))
                .replace("Olympus", t("map.Olympus"))
                .replace("Broken Moon", t("map.Broken Moon"))
                .replace("E-District", t("map.E-District"));
        return prefix + bestZh + rest;
    }
}
