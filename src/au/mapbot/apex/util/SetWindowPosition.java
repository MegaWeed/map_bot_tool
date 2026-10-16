/*
 * SetWindowPosition
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;
import au.mapbot.apex.classes.MBLogger;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import java.awt.*;


public class SetWindowPosition {
    private static final User32 user32 = User32.INSTANCE;

    public static void forceWindowToStayOnBottom(){
        try {
            ScreenshotUtil screenshotUtil = new ScreenshotUtil();
            ScreenshotUtil.WindowInfo windowInfo = screenshotUtil.getSpecificWindow("Mapbot Embed Twitch Stream or VOD - Google Chrome");

            Pointer pointer = new Pointer(Pointer.nativeValue(new Pointer(windowInfo.hwnd)));
            WinDef.HWND hwnd = new WinDef.HWND(pointer);

            //https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowpos
            user32.SetWindowPos(
                    hwnd,
                    new WinDef.HWND(Pointer.createConstant(-1)),
                    0, 0, 0, 0,
                    User32.SWP_NOACTIVATE
            );

            //https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-showwindow
            user32.ShowWindow(hwnd, User32.SW_SHOWNOACTIVATE);
        } catch (Exception ex) {
            MBLogger.warning("Unable to use HWND to disable window autofocus and prevent force to front function", ex);
        }
    }
}