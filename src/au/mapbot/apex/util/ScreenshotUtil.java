/*
 * ScreenshotUtil
 * v0.9
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.util;

import au.mapbot.apex.classes.BrowserImage;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ScreenshotUtil {

    private Robot robot = new Robot();

    public ScreenshotUtil() throws AWTException {
    }

    public BrowserImage captureWindowToBufferedImage(final String window) {
        final WindowInfo w = getSpecificWindow(window);
        if (w == null) {
            return null;
        }

        final int width = w.rect.right - w.rect.left;
        final int height = w.rect.bottom - w.rect.top;

        if (w.title.contains("Chrome")){
            return new BrowserImage(robot.createScreenCapture(new Rectangle(w.rect.left, w.rect.top, width, height)), BrowserImage.BrowserEnum.CHROME);

        }else if (w.title.contains("Firefox")){
            return new BrowserImage(robot.createScreenCapture(new Rectangle(w.rect.left, w.rect.top, width, height)), BrowserImage.BrowserEnum.FIREFOX);

        }

        return new BrowserImage(robot.createScreenCapture(new Rectangle(w.rect.left, w.rect.top, width, height)), BrowserImage.BrowserEnum.CHROME);
    }

    public WindowInfo getSpecificWindow(final String windowName){
        final java.util.List<WindowInfo> inflList = new ArrayList<WindowInfo>();
        final java.util.List<Integer> order = new ArrayList<Integer>();
        int top = User32.instance.GetTopWindow(0);
        while (top != 0) {
            order.add(top);
            top = User32.instance.GetWindow(top, User32.GW_HWNDNEXT);
        }

        User32.instance.EnumWindows(new WndEnumProc() {
            public boolean callback(int hWnd, int lParam) {
                WindowInfo info = getWindowInfo(hWnd);
                inflList.add(info);
                return true;
            }

        }, 0);
        Collections.sort(inflList, new Comparator<WindowInfo>() {
            public int compare(WindowInfo o1, WindowInfo o2) {
                return order.indexOf(o1.hwnd) - order.indexOf(o2.hwnd);
            }
        });

        //return correct window
        //with way firefox tabs work, may create second process for same window
        //so need to find the largest one which should be the visible window
        int width = 0;
        int height = 0;
        WindowInfo returnWindow = null;
        for (WindowInfo w : inflList) {
            if (w.title.contains(windowName)){
                if(w.rect.right - w.rect.left > width &&
                w.rect.bottom - w.rect.top > height){
                    width = w.rect.right - w.rect.left;
                    height = w.rect.bottom - w.rect.top;
                    returnWindow = w;
                }
            }
        }
        if (returnWindow != null) {
            //TODO
//            int style = User32.instance.GetWindowLong(returnWindow.hwnd, User32.GWL_STYLE);
//            boolean isFullscreen = (style & User32.WS_POPUP) != 0; // Check if WS_POPUP style is set
//            returnWindow.isFullscreen = isFullscreen; // Assuming you have isFullscreen field in WindowInfo
            return returnWindow;
        }

        return null;
    }

    public void listAllWindows() {
        final java.util.List<WindowInfo> inflList = new ArrayList<WindowInfo>();
        final List<Integer> order = new ArrayList<Integer>();
        int top = User32.instance.GetTopWindow(0);
        while (top != 0) {
            order.add(top);
            top = User32.instance.GetWindow(top, User32.GW_HWNDNEXT);
        }

        User32.instance.EnumWindows(new WndEnumProc() {
            public boolean callback(int hWnd, int lParam) {
                WindowInfo info = getWindowInfo(hWnd);
                inflList.add(info);
                return true;
            }

        }, 0);
        Collections.sort(inflList, new Comparator<WindowInfo>() {
            public int compare(WindowInfo o1, WindowInfo o2) {
                return order.indexOf(o1.hwnd) - order.indexOf(o2.hwnd);
            }
        });
        for (WindowInfo w : inflList) {
            System.out.println(w);
        }
    }

    private WindowInfo getWindowInfo(int hWnd) {
        RECT r = new RECT();
        User32.instance.GetWindowRect(hWnd, r);
        byte[] buffer = new byte[1024];
        User32.instance.GetWindowTextA(hWnd, buffer, buffer.length);
        String title = Native.toString(buffer);
        WindowInfo info = new WindowInfo(hWnd, r, title);
        return info;
    }



    public interface WndEnumProc extends StdCallLibrary.StdCallCallback {
        boolean callback(int hWnd, int lParam);
    }

    public interface User32 extends StdCallLibrary {
        public static final String SHELL_TRAY_WND = "Shell_TrayWnd";
        public static final int WM_COMMAND = 0x111;
        public static final int MIN_ALL = 0x1a3;
        public static final int MIN_ALL_UNDO = 0x1a0;

        int GetWindowLong(int hWnd, int nIndex); // Add GetWindowLong method

        int GWL_STYLE = -16; // Constant for getting the window style

        int WS_POPUP = 0x80000000; // WS_POPUP style constant
        final User32 instance = (User32) Native.loadLibrary("user32", User32.class);

        boolean EnumWindows(WndEnumProc wndenumproc, int lParam);

        boolean IsWindowVisible(int hWnd);

        int GetWindowRect(int hWnd, RECT r);

        void GetWindowTextA(int hWnd, byte[] buffer, int buflen);

        int GetTopWindow(int hWnd);

        int GetWindow(int hWnd, int flag);

        boolean ShowWindow(int hWnd);

        boolean BringWindowToTop(int hWnd);

        int GetActiveWindow();

        boolean SetForegroundWindow(int hWnd);

        int FindWindowA(String winClass, String title);

        long SendMessageA(int hWnd, int msg, int num1, int num2);

        final int GW_HWNDNEXT = 2;
    }

    public static class RECT extends Structure {
        public int left, top, right, bottom;

        @Override
        protected List<String> getFieldOrder() {
            List<String> order = new ArrayList<>();
            order.add("left");
            order.add("top");
            order.add("right");
            order.add("bottom");
            return order;
        }
    }

    public static class WindowInfo {
        int hwnd;
        public RECT rect;
        String title;

        public Boolean isFullscreen;

        public WindowInfo(int hwnd, RECT rect, String title) {
            this.hwnd = hwnd;
            this.rect = rect;
            this.title = title;
        }

        public String toString() {
            return String.format("(%d,%d)-(%d,%d) : \"%s\"", rect.left, rect.top, rect.right, rect.bottom, title);
        }
    }
}
