package dev.helikon.client.setting;

/**
 * Minecraft-free geometry and value mapping for the ClickGUI numeric sliders
 * shared by {@link NumberSetting} and {@link IntegerSetting} rows. Kept free of
 * Minecraft classes so the drag, click, and increment/decrement rules stay
 * unit-testable; the screen only supplies pixel coordinates and applies the
 * result through the setting's own validated {@code set} path.
 */
public final class NumberSlider {
    /** Approximate number of slider steps spanning a range for drag and nudge snapping. */
    private static final int STEPS = 100;
    private static final int MAX_DECIMALS = 6;

    private NumberSlider() {
    }

    /** The 0..1 position of {@code value} inside the inclusive range. */
    public static double fraction(double value, double minimum, double maximum) {
        if (maximum <= minimum) {
            return 0.0D;
        }
        return Math.clamp((value - minimum) / (maximum - minimum), 0.0D, 1.0D);
    }

    /** Maps a value to the handle's horizontal pixel inside {@code [left, left + width)}. */
    public static int handleX(double value, double minimum, double maximum, int left, int width) {
        if (width < 1) {
            throw new IllegalArgumentException("Slider width must be at least one pixel");
        }
        return left + (int) Math.round(fraction(value, minimum, maximum) * (width - 1));
    }

    /**
     * Maps a horizontal track coordinate to a value clamped inside the range.
     * Integral sliders round to the nearest whole number; decimal sliders snap
     * to a range-relative grid so their displayed text stays clean while the
     * text field remains available for exact entry.
     */
    public static double valueAt(int x, int left, int width, double minimum, double maximum, boolean integral) {
        if (width < 2) {
            throw new IllegalArgumentException("Slider width must be at least two pixels");
        }
        double position = Math.clamp((x - left) / (double) (width - 1), 0.0D, 1.0D);
        return snap(minimum + position * (maximum - minimum), minimum, maximum, integral);
    }

    /**
     * Increases or decreases the value by one step in the given direction
     * (positive raises, negative lowers), clamped to the range. Integral
     * sliders step by one; decimal sliders step by a range-relative amount.
     */
    public static double nudge(double value, double minimum, double maximum, boolean integral, int direction) {
        if (direction == 0) {
            return snap(value, minimum, maximum, integral);
        }
        double step = step(minimum, maximum, integral);
        double moved = value + (direction > 0 ? step : -step);
        return snap(moved, minimum, maximum, integral);
    }

    private static double step(double minimum, double maximum, boolean integral) {
        if (integral) {
            return 1.0D;
        }
        double range = maximum - minimum;
        return range > 0.0D ? range / STEPS : 0.0D;
    }

    private static double snap(double value, double minimum, double maximum, boolean integral) {
        double clamped = Math.clamp(value, minimum, maximum);
        if (integral) {
            return Math.clamp(Math.rint(clamped), minimum, maximum);
        }
        double range = maximum - minimum;
        if (!(range > 0.0D)) {
            return clamped;
        }
        double factor = Math.pow(10, decimalPlaces(range));
        double rounded = Math.rint(clamped * factor) / factor;
        return Math.clamp(rounded, minimum, maximum);
    }

    /** Chooses a decimal precision that yields roughly {@link #STEPS} steps across the range. */
    private static int decimalPlaces(double range) {
        if (!(range > 0.0D) || !Double.isFinite(range)) {
            return 0;
        }
        int digits = (int) Math.ceil(2.0D - Math.log10(range));
        return Math.clamp(digits, 0, MAX_DECIMALS);
    }
}
