package dev.helikon.client.hud;

/** Shared grid snapping for HUD editor elements and the Active Modules list. */
public final class HudEditorGrid {
    public static final int SIZE = 8;

    private HudEditorGrid() {
    }

    public static int snap(int value, int minimum, int maximum) {
        if (minimum < 0 || maximum < minimum) {
            throw new IllegalArgumentException("Invalid snap range");
        }
        int bounded = Math.clamp(value, minimum, maximum);
        if (bounded - minimum < SIZE / 2) {
            return minimum;
        }
        if (maximum - bounded < SIZE / 2) {
            return maximum;
        }
        int snapped = minimum + Math.round((bounded - minimum) / (float) SIZE) * SIZE;
        return Math.clamp(snapped, minimum, maximum);
    }
}
