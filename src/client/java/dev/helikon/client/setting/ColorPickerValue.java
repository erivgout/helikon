package dev.helikon.client.setting;

/** Minecraft-free ARGB channel helpers for the ClickGUI color picker. */
public final class ColorPickerValue {
    public static final int CHANNEL_COUNT = 4;

    private ColorPickerValue() {
    }

    /** Channel order is alpha, red, green, then blue. */
    public static int channel(int color, int channel) {
        return color >>> shift(channel) & 0xFF;
    }

    public static int withChannel(int color, int channel, int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Color channel must be in 0..255");
        }
        int shift = shift(channel);
        return color & ~(0xFF << shift) | value << shift;
    }

    /** Maps a horizontal picker coordinate to a closed 0..255 channel value. */
    public static int channelAt(int x, int left, int width) {
        if (width < 2) {
            throw new IllegalArgumentException("Picker width must be at least two pixels");
        }
        return Math.clamp((int) Math.round((x - left) * 255.0D / (width - 1)), 0, 255);
    }

    private static int shift(int channel) {
        if (channel < 0 || channel >= CHANNEL_COUNT) {
            throw new IllegalArgumentException("Unknown ARGB channel: " + channel);
        }
        return (CHANNEL_COUNT - 1 - channel) * 8;
    }
}
