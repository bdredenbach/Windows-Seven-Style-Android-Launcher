package com.nthexperience.desktop;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Perspective window cards above the desktop wallpaper. */
final class FlipStack extends FrameLayout {
    private static final int REAR_CARDS = 4;
    private final List<DesktopWindow> windows;
    private final List<String> apps;
    private final WindowPreviews.Host host;
    private final Function<DesktopWindow, Bitmap> snapshot;
    private final Runnable dismiss;
    private final Bitmap desktopImage;
    private final LruCache<DesktopWindow, Bitmap> images = new LruCache<>(8);
    private final Map<Integer, View> cards = new HashMap<>();
    private final Stage stage;
    private final TextView status;
    private final int count;
    private int selected;
    private float progress;
    private ValueAnimator animation;

    FlipStack(Activity activity, List<DesktopWindow> windows, List<String> apps,
              WindowPreviews.Host host, Function<DesktopWindow, Bitmap> snapshot,
              Bitmap desktopImage, Runnable dismiss) {
        super(activity);
        this.windows = windows;
        this.apps = apps;
        this.host = host;
        this.snapshot = snapshot;
        this.desktopImage = desktopImage;
        this.dismiss = dismiss;
        int tasks = windows.size() + apps.size();
        count = tasks + 1;
        setAccessibilityPaneTitle("Flip 3D");
        setBackgroundColor(0xff102434);
        if (desktopImage != null) {
            ImageView backdrop = new ImageView(activity);
            backdrop.setImageBitmap(desktopImage);
            backdrop.setScaleType(ImageView.ScaleType.FIT_XY);
            backdrop.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(backdrop, new LayoutParams(-1, -1));
        }
        View shade = new View(activity);
        shade.setBackgroundColor(0x9909161e);
        addView(shade, new LayoutParams(-1, -1));

        stage = new Stage();
        LayoutParams scene = new LayoutParams(-1, -1);
        scene.bottomMargin = dp(102);
        addView(stage, scene);
        if (Aero.motion(activity)) {
            stage.setAlpha(0);
            stage.setScaleX(.92f);
            stage.setScaleY(.92f);
            stage.animate().alpha(1).scaleX(1).scaleY(1).setDuration(180).start();
        }

        LinearLayout controls = Aero.column(activity);
        controls.setPadding(dp(6), dp(3), dp(6), dp(6));
        controls.setBackground(Aero.panel(activity, 0xe634566b, 0xf220374b, 8, 0xff7297af));
        status = Aero.text(activity, "", 14, Color.WHITE);
        status.setGravity(Gravity.CENTER);
        status.setSingleLine(true);
        status.setEllipsize(TextUtils.TruncateAt.END);
        status.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
        controls.addView(status, new LinearLayout.LayoutParams(-1, dp(30)));

        LinearLayout buttons = new LinearLayout(activity);
        TextView previous = control("‹", "Previous window", () -> step(-1));
        previous.setEnabled(count > 1);
        buttons.addView(previous, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView open = control("Switch to window", "Switch to window", this::activate);
        buttons.addView(open, new LinearLayout.LayoutParams(0, dp(46), 1));
        TextView next = control("›", "Next window", () -> step(1));
        next.setEnabled(count > 1);
        buttons.addView(next, new LinearLayout.LayoutParams(dp(46), dp(46)));
        buttons.addView(control("×", "Close Flip 3D", dismiss), new LinearLayout.LayoutParams(dp(46), dp(46)));
        controls.addView(buttons);
        int panelWidth = Math.min(420, getResources().getConfiguration().screenWidthDp - 24);
        LayoutParams footer = new LayoutParams(dp(Math.max(200, panelWidth)), -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        footer.bottomMargin = dp(10);
        addView(controls, footer);
        stage.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> {
            if (r-l != or-ol || b-t != ob-ot) {
                clearCards();
                positionCards(0);
            }
        });
        updateStatus();
    }

    private int dp(int value) { return Aero.dp(getContext(), value); }
    private TextView control(String text, String description, Runnable action) {
        TextView button = Aero.text(getContext(), text, text.length() == 1 ? 24 : 13, Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setBackground(Aero.selection(getContext()));
        button.setContentDescription(description);
        button.setFocusable(true);
        button.setOnClickListener(view -> action.run());
        return button;
    }
    private boolean desktop(int index) { return index == windows.size() + apps.size(); }
    private String title(int index) {
        if (desktop(index)) return "Show Desktop";
        return index < windows.size() ? windows.get(index).caption() : host.label(apps.get(index - windows.size()));
    }
    private void updateStatus() {
        status.setContentDescription("Selected window: " + title(selected));
        status.setText(title(selected) + "  •  " + (selected + 1) + " of " + count);
    }
    void step(int direction) {
        if (count < 2) return;
        finishAnimation();
        animateTo(direction);
    }
    void activate() {
        if (animation != null) finishAnimation();
        open(selected);
    }
    private void open(int index) {
        dismiss.run();
        if (desktop(index)) host.showDesktop();
        else if (index < windows.size()) {
            DesktopWindow window = windows.get(index);
            if (window.getParent() != null) window.restore();
        } else host.open(apps.get(index - windows.size()));
    }
    private void animateTo(int direction) {
        if (!Aero.motion(getContext())) {
            settle(direction);
            return;
        }
        ValueAnimator move = ValueAnimator.ofFloat(progress, direction);
        animation = move;
        move.setDuration(210);
        move.addUpdateListener(value -> positionCards((Float) value.getAnimatedValue()));
        move.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator value) {
                animation = null;
                settle(direction);
            }
        });
        move.start();
    }
    private void finishAnimation() {
        if (animation != null) animation.end();
    }
    private void settle(int direction) {
        selected = Math.floorMod(selected + direction, count);
        positionCards(0);
        updateStatus();
    }
    private void clearCards() {
        stage.removeAllViews();
        cards.clear();
    }
    private void positionCards(float offset) {
        progress = offset;
        int width = stage.getWidth(), height = stage.getHeight();
        if (width == 0 || height == 0) return;
        int first = offset < 0 ? -1 : 0;
        int last = first + Math.min(REAR_CARDS + (offset > 0 ? 1 : 0) - first, count - 1);
        for (int slot = last; slot >= first; slot--) {
            int index = Math.floorMod(selected + slot, count);
            View card = cards.get(index);
            if (card == null) {
                card = card(index);
                cards.put(index, card);
            }
            float depth = slot - offset;
            float distance = Math.max(0, depth);
            float scale = (float) Math.pow(.85, distance) + Math.max(0, -depth) * .12f;
            float shift = Math.min(4.8f, depth);
            card.setTranslationX(width * ((count == 1 ? 0 : .14f) - .115f * shift));
            card.setTranslationY(height * (.10f - .075f * shift));
            card.setTranslationZ(dp(12) - dp(2) * depth);
            card.setScaleX(scale);
            card.setScaleY(scale);
            card.setAlpha(Math.max(0, Math.min(1, Math.min(1f + depth, (REAR_CARDS + 1f - depth) * .8f))));
            card.setImportantForAccessibility(depth < -.5f || depth > REAR_CARDS + .2f
                    ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        }
        final int firstSlot = first, lastSlot = last;
        cards.entrySet().removeIf(entry -> {
            int slot = firstSlot + Math.floorMod(entry.getKey() - selected - firstSlot, count);
            if (slot <= lastSlot) return false;
            stage.removeView(entry.getValue());
            return true;
        });
    }
    private View card(int index) {
        DesktopWindow window = index < windows.size() ? windows.get(index) : null;
        boolean privateWindow = window != null && !WindowPreviews.allowed(window);
        Bitmap bitmap = desktop(index) ? desktopImage : null;
        if (window != null && !privateWindow) {
            bitmap = images.get(window);
            if (bitmap == null) {
                bitmap = snapshot.apply(window);
                if (bitmap != null) images.put(window, bitmap);
            }
        }
        View preview;
        if (bitmap != null) {
            ImageView image = new ImageView(getContext());
            image.setImageBitmap(bitmap);
            image.setScaleType(ImageView.ScaleType.FIT_XY);
            preview = image;
        } else {
            TextView fallback = Aero.text(getContext(), title(index) + "\n" + (privateWindow ? "InPrivate — preview hidden"
                    : window == null ? "Android app" : "Preview unavailable"), 14, Aero.INK);
            fallback.setGravity(Gravity.CENTER);
            preview = fallback;
        }
        preview.setBackground(Aero.panel(getContext(), 0xffecf7ff, 0xffc7dce8, 3, 0xff91b9ce));
        preview.setPadding(dp(2), dp(2), dp(2), dp(2));
        preview.setCameraDistance(dp(2200));
        preview.setRotationY(21f);
        preview.setContentDescription("Flip to " + title(index)
                + (privateWindow ? ". InPrivate — preview hidden" : ""));
        preview.setFocusable(true);
        preview.setOnClickListener(view -> open(index));
        float ratio = bitmap == null ? 1.55f : (float) bitmap.getWidth() / bitmap.getHeight();
        int width = Math.round(stage.getWidth() * (stage.getWidth() > stage.getHeight() ? .58f : .65f));
        int height = Math.min(Math.round(width / ratio), Math.round(stage.getHeight() * .65f));
        width = Math.min(width, Math.round(height * ratio));
        stage.addView(preview, new LayoutParams(width, height, Gravity.CENTER));
        return preview;
    }

    @Override protected void onDetachedFromWindow() {
        if (animation != null) {
            animation.removeAllListeners();
            animation.cancel();
            animation = null;
        }
        images.evictAll();
        super.onDetachedFromWindow();
    }

    private final class Stage extends FrameLayout {
        private final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        private float startX, startY;
        private boolean dragging;
        Stage() { super(FlipStack.this.getContext()); }
        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                finishAnimation();
                startX = event.getX();
                startY = event.getY();
                dragging = false;
            }
            if (event.getActionMasked() != MotionEvent.ACTION_MOVE || count < 2) return false;
            float x = Math.abs(event.getX() - startX);
            return x > slop && x > Math.abs(event.getY() - startY);
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            float dx = startX - event.getX();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    dragging |= count > 1 && Math.abs(dx) > slop && Math.abs(dx) > Math.abs(event.getY() - startY);
                    if (dragging && Aero.motion(getContext()))
                        positionCards(Math.max(-.95f, Math.min(.95f, dx / (getWidth() * .34f))));
                    break;
                case MotionEvent.ACTION_UP:
                    if (dragging || count > 1 && Math.abs(dx) > slop * 2)
                        animateTo(Math.abs(dx) >= Math.max(slop * 2, dp(22)) ? (dx > 0 ? 1 : -1) : 0);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    if (progress != 0) animateTo(0);
                    break;
            }
            return true;
        }
        @Override public boolean onGenericMotionEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_SCROLL && count > 1) {
                float scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                if (scroll == 0) scroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
                if (scroll != 0) { step(scroll < 0 ? 1 : -1); return true; }
            }
            return super.onGenericMotionEvent(event);
        }
    }
}
