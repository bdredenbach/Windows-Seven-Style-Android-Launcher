package com.nthexperience.desktop;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Predicate;

/** Real title-bar input on both orientations, including snap previews and canceled drags. */
public final class SnapSmoke extends Instrumentation {
    private MainActivity home;
    private View root;
    private View workspace;
    private long down;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        int status = Activity.RESULT_CANCELED;
        try {
            Context target = getTargetContext();
            DesktopStore store = new DesktopStore(target);
            store.prefs.edit().putInt("startup_mode", 2).putBoolean("reduce_motion", true)
                    .putBoolean("sidebar_always_top", true).commit();
            target.getSharedPreferences(StartupExperience.SESSION, Context.MODE_PRIVATE).edit()
                    .putBoolean("seen", true).putBoolean("welcome_seen", true).commit();
            home = (MainActivity) startActivitySync(new Intent(Intent.ACTION_MAIN)
                    .setComponent(new ComponentName(target, MainActivity.class))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            root = home.getWindow().getDecorView();
            rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            runOnMainSync(() -> home.open("@computer"));
            settle();
            DesktopWindow left = window("@computer");
            workspace = (View) left.getParent();
            GadgetSidebar sidebar = (GadgetSidebar) find(workspace, v -> v instanceof GadgetSidebar);
            runOnMainSync(() -> sidebar.visible(true));
            settle();

            // Derive the expected top from the real shell, and independently reserve the
            // visible always-on-top Sidebar from the workspace width.
            runOnMainSync(left::maximize);
            settle();
            Rect work = new Rect(0, (int) left.getY(), workspace.getWidth() - sidebar.getWidth(), workspace.getHeight());
            check(work.width() / 2 >= dp(240), "Landscape leaves room for two Explorer windows");
            runOnMainSync(() -> left.arrange(dp(25), dp(55), dp(300), dp(230)));
            settle();
            Rect floating = bounds(left);
            int edgeY = work.centerY();
            begin(left);
            input(MotionEvent.ACTION_MOVE, dp(4), edgeY);
            checkOutline(dp(1), work.bottom - dp(18), true, "Left outline appears at the workspace edge");
            checkOutline(work.right + dp(4), work.bottom - dp(18), false, "Outline clears the Sidebar");
            input(MotionEvent.ACTION_MOVE, work.centerX(), edgeY);
            checkOutline(dp(1), work.bottom - dp(18), false, "Leaving the edge clears the outline");
            input(MotionEvent.ACTION_MOVE, dp(4), edgeY);
            input(MotionEvent.ACTION_CANCEL, dp(4), edgeY);
            settle();
            check(floating.equals(bounds(left)), "Cancel restores the original floating bounds");
            checkOutline(dp(1), work.bottom - dp(18), false, "Cancel removes the outline");

            drag(left, dp(4), edgeY);
            Rect leftHalf = new Rect(0, work.top, work.right / 2, work.bottom);
            check(leftHalf.equals(bounds(left)), "Left window fills the exact left half");
            runOnMainSync(() -> home.open("@files"));
            settle();
            DesktopWindow right = window("@files");
            runOnMainSync(() -> right.arrange(dp(28), dp(62), dp(300), dp(230)));
            settle();
            drag(right, work.right - dp(4), edgeY);
            Rect rightHalf = new Rect(leftHalf.right, work.top, work.right, work.bottom);
            check(rightHalf.equals(bounds(right)), "Right window fills the exact right half");
            tap(title(left));
            check(left.active() && right.isShown() && rightHalf.equals(bounds(right)),
                    "Left Explorer can take focus while right Explorer stays snapped");
            tap(title(right));
            check(right.active() && left.isShown() && leftHalf.equals(bounds(left)),
                    "Right Explorer can take focus while left Explorer stays snapped");

            begin(left);
            input(MotionEvent.ACTION_MOVE, work.centerX(), edgeY);
            settle();
            check(left.getWidth() == floating.width() && left.getHeight() == floating.height(),
                    "Dragging a snapped window restores its prior floating size during the drag");
            check(bounds(left).contains(work.centerX(), edgeY), "Floating window follows the pointer");
            input(MotionEvent.ACTION_UP, work.centerX(), edgeY);
            settle();
            check(!left.geometry().has("snap") && !left.maximized(), "Free release keeps the window floating");
            Rect moved = bounds(left);
            begin(left);
            input(MotionEvent.ACTION_MOVE, work.centerX(), work.top + dp(4));
            checkOutline(work.centerX(), work.bottom - dp(1), true, "Top outline reaches the end of the workspace");
            checkOutline(work.centerX(), work.bottom + dp(4), false, "Top outline stays clear of the taskbar");
            input(MotionEvent.ACTION_UP, work.centerX(), work.top + dp(4));
            settle();
            check(left.maximized() && work.equals(bounds(left)), "Top release maximizes within the work area");
            begin(left);
            input(MotionEvent.ACTION_MOVE, work.centerX(), edgeY);
            settle();
            check(left.getWidth() == moved.width() && left.getHeight() == moved.height(),
                    "Dragging a maximized window restores its floating size");
            input(MotionEvent.ACTION_CANCEL, work.centerX(), edgeY);
            settle();
            check(left.maximized() && work.equals(bounds(left)), "Cancel restores the maximized placement");
            tap(find(left, v -> "Maximize or restore window".equals(v.getContentDescription())));
            check(moved.equals(bounds(left)), "Restore control returns to the previous floating bounds");

            Thread.sleep(340);
            doubleTap(title(left));
            check(left.maximized() && work.equals(bounds(left)), "Title-bar double-tap still maximizes");
            Thread.sleep(340);
            doubleTap(title(left));
            check(moved.equals(bounds(left)), "Title-bar double-tap restores");

            rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            runOnMainSync(() -> sidebar.visible(false));
            settle();
            runOnMainSync(() -> left.arrange(dp(12), dp(50), dp(290), dp(250)));
            settle();
            Rect portrait = bounds(left);
            int portraitCenter = workspace.getWidth() / 2;
            drag(left, dp(4), workspace.getHeight() / 2);
            boolean canSplit = workspace.getWidth() / 2 >= dp(240);
            check(left.geometry().has("snap") == canSplit, "Portrait side snap requires usable halves");
            if (!canSplit) check(left.getWidth() == portrait.width(), "Narrow portrait preserves floating size");
            drag(left, portraitCenter, dp(4));
            check(left.maximized() && left.getWidth() == workspace.getWidth()
                    && bounds(left).bottom == workspace.getHeight(), "Top snap works in portrait");
            result.putString("stream", "PASS: Aero Snap preview, side-by-side Explorer focus, top snap, drag-away, cancel, window controls and portrait limits.\n");
            status = Activity.RESULT_OK;
        } catch (Throwable failure) {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            result.putString("stream", "FAIL: " + trace);
        }
        finish(status, result);
    }

    private void rotate(int orientation) throws Exception {
        runOnMainSync(() -> home.setRequestedOrientation(orientation));
        boolean landscape = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        long end = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < end) {
            if (root.getWidth() > 0 && (root.getWidth() > root.getHeight()) == landscape) {
                settle();
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("The emulator did not rotate");
    }

    private void drag(DesktopWindow window, int x, int y) throws Exception {
        begin(window);
        input(MotionEvent.ACTION_MOVE, x, y);
        input(MotionEvent.ACTION_UP, x, y);
        settle();
    }

    private void begin(DesktopWindow window) throws Exception {
        Thread.sleep(320);
        Rect rect = screenBounds(title(window));
        int[] origin = new int[2];
        workspace.getLocationOnScreen(origin);
        down = SystemClock.uptimeMillis();
        input(MotionEvent.ACTION_DOWN, rect.centerX() - origin[0], rect.centerY() - origin[1]);
    }

    private void tap(View view) throws Exception {
        Rect rect = screenBounds(view);
        int[] origin = new int[2];
        workspace.getLocationOnScreen(origin);
        down = SystemClock.uptimeMillis();
        input(MotionEvent.ACTION_DOWN, rect.centerX() - origin[0], rect.centerY() - origin[1]);
        input(MotionEvent.ACTION_UP, rect.centerX() - origin[0], rect.centerY() - origin[1]);
        settle();
    }

    private void doubleTap(View view) throws Exception {
        Rect rect = screenBounds(view);
        int[] origin = new int[2];
        workspace.getLocationOnScreen(origin);
        for (int i = 0; i < 2; i++) {
            down = SystemClock.uptimeMillis();
            input(MotionEvent.ACTION_DOWN, rect.centerX() - origin[0], rect.centerY() - origin[1]);
            input(MotionEvent.ACTION_UP, rect.centerX() - origin[0], rect.centerY() - origin[1]);
            if (i == 0) Thread.sleep(70);
        }
        settle();
    }

    private void input(int action, int x, int y) {
        int[] origin = new int[2];
        workspace.getLocationOnScreen(origin);
        MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action,
                origin[0] + x, origin[1] + y, 0);
        try {
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            check(getUiAutomation().injectInputEvent(event, true), "Touch event " + action + " was injected");
        } finally {
            event.recycle();
        }
    }

    private void checkOutline(int x, int y, boolean expected, String message) throws Exception {
        settle();
        int[] origin = new int[2];
        workspace.getLocationOnScreen(origin);
        Bitmap screenshot = getUiAutomation().takeScreenshot();
        check(screenshot != null, "Screenshot is available");
        try {
            int color = screenshot.getPixel(origin[0] + x, origin[1] + y);
            boolean matches = Math.abs(Color.red(color) - 182) < 6
                    && Math.abs(Color.green(color) - 232) < 6 && Color.blue(color) > 249;
            check(matches == expected, message + " (pixel #" + Integer.toHexString(color) + ")");
        } finally {
            screenshot.recycle();
        }
    }

    private void settle() throws Exception {
        waitForIdleSync();
        Thread.sleep(100);
        waitForIdleSync();
    }

    private int dp(int value) { return Aero.dp(home, value); }

    private DesktopWindow window(String id) {
        DesktopWindow window = (DesktopWindow) find(root, v -> v instanceof DesktopWindow
                && id.equals(((DesktopWindow) v).toolId));
        check(window != null && window.body instanceof VistaExplorer, "Explorer opens for " + id);
        return window;
    }

    private static View title(DesktopWindow window) {
        return find(window, v -> v instanceof TextView && v.getContentDescription() != null
                && v.getContentDescription().toString().endsWith("Drag to move window."));
    }

    private static Rect bounds(View view) {
        int x = Math.round(view.getX()), y = Math.round(view.getY());
        return new Rect(x, y, x + view.getWidth(), y + view.getHeight());
    }

    private static Rect screenBounds(View view) {
        Rect result = new Rect();
        check(view != null && view.getGlobalVisibleRect(result), "Title or control is visible");
        return result;
    }

    private static View find(View root, Predicate<View> predicate) {
        if (predicate.test(root)) return root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), predicate);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
