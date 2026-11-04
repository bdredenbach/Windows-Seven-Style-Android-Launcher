package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/** Shared playback glyphs for the Player and the ten-foot Media Center view. */
final class MediaTransportButton extends View {
    enum Action { PREVIOUS, PLAY, PAUSE, NEXT, VOLUME }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Shader blue = new LinearGradient(0, 1, 0, 23,
            new int[]{0xffb9ecff, 0xff428ccb, 0xff153c81, 0xff397ed4}, null, Shader.TileMode.CLAMP);
    private Action action;

    MediaTransportButton(Context context, Action action, Runnable click) {
        super(context);
        setBackground(Aero.selection(context));
        setFocusable(true);
        setOnClickListener(v -> click.run());
        action(action);
    }

    void action(Action value) {
        if (action == value) return;
        action = value;
        String name = switch (value) {
            case PREVIOUS -> "Previous";
            case PLAY -> "Play";
            case PAUSE -> "Pause";
            case NEXT -> "Next";
            case VOLUME -> "Volume";
        };
        setContentDescription(name);
        setTooltipText(name);
        invalidate();
    }

    @Override public CharSequence getAccessibilityClassName() {
        return android.widget.Button.class.getName();
    }

    @Override protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        boolean primary = action == Action.PLAY || action == Action.PAUSE;
        int size = Math.min(Aero.dp(getContext(), primary ? 42 : 24), Math.min(getWidth(), getHeight()));
        if (size <= 0) return;
        canvas.save();
        canvas.translate((getWidth() - size) / 2f, (getHeight() - size) / 2f);
        canvas.scale(size / 24f, size / 24f);
        if (primary) {
            paint.setShader(blue);
            paint.setAlpha(isEnabled() ? 255 : 125);
            canvas.drawCircle(12, 12, 11, paint);
            paint.setShader(null);
            paint.setColor(isFocused() || isPressed() ? 0xfff6ffff : 0xffa1caef);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(.85f);
            canvas.drawCircle(12, 12, 10.5f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        paint.setColor(isEnabled() ? 0xffeffaff : 0x81c1d5e4);
        switch (action) {
            case PLAY -> triangle(canvas, 9, 6.6f, 17, 12, 9, 17.4f);
            case PAUSE -> {
                canvas.drawRoundRect(8, 7, 11, 17, .5f, .5f, paint);
                canvas.drawRoundRect(13, 7, 16, 17, .5f, .5f, paint);
            }
            case PREVIOUS, NEXT -> {
                if (action == Action.PREVIOUS) {
                    canvas.translate(24, 0);
                    canvas.scale(-1, 1);
                }
                triangle(canvas, 4, 5, 15, 12, 4, 19);
                canvas.drawRect(16, 5, 19, 19, paint);
            }
            case VOLUME -> {
                canvas.drawRect(3, 9, 7, 15, paint);
                path.rewind();
                path.moveTo(6, 9);
                path.lineTo(12, 5);
                path.lineTo(12, 19);
                path.lineTo(6, 15);
                path.close();
                canvas.drawPath(path, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.4f);
                canvas.drawArc(7, 6, 18, 18, -56, 112, false, paint);
                canvas.drawArc(5, 3, 22, 21, -52, 104, false, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        canvas.restore();
    }

    private void triangle(Canvas canvas, float ax, float ay, float bx, float by, float cx, float cy) {
        path.rewind();
        path.moveTo(ax, ay);
        path.lineTo(bx, by);
        path.lineTo(cx, cy);
        path.close();
        canvas.drawPath(path, paint);
    }
}
