package com.nthexperience.desktop;

/** Edge targeting in workspace coordinates, with the top edge taking priority at corners. */
final class AeroSnap {
    enum Placement { FLOATING, MAXIMIZED, LEFT, RIGHT }

    static boolean canSplit(int width, int height, int minimumWidth, int minimumHeight) {
        return width / 2 >= minimumWidth && height >= minimumHeight;
    }

    static Placement target(float x, float y, int width, int top, int height, int edge,
                            int minimumWidth, int minimumHeight) {
        if (x < 0 || x > width || y < 0 || y >= height) return Placement.FLOATING;
        if (y <= top + edge) return Placement.MAXIMIZED;
        if (canSplit(width, height - top, minimumWidth, minimumHeight)) {
            if (x <= edge) return Placement.LEFT;
            if (x >= width - edge) return Placement.RIGHT;
        }
        return Placement.FLOATING;
    }
}
