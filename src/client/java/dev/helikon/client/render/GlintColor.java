package dev.helikon.client.render;

/** Minecraft-free HSV color conversion used by the locally rendered item-glint tint. */
public final class GlintColor {
    private GlintColor() {
    }

    /** Returns an opaque rainbow color for a non-negative millisecond timestamp and positive cycle rate. */
    public static int rainbow(long nowMillis, double cyclesPerSecond) {
        if (nowMillis < 0L || !Double.isFinite(cyclesPerSecond) || cyclesPerSecond <= 0.0D) {
            throw new IllegalArgumentException("time and cycle rate must be finite and non-negative/positive");
        }
        double hue = (nowMillis / 1_000.0D * cyclesPerSecond) % 1.0D;
        double scaled = hue * 6.0D;
        int sector = (int) Math.floor(scaled);
        double fraction = scaled - sector;
        int rising = channel(fraction);
        int falling = channel(1.0D - fraction);
        return switch (sector) {
            case 0 -> argb(255, rising, 0);
            case 1 -> argb(falling, 255, 0);
            case 2 -> argb(0, 255, rising);
            case 3 -> argb(0, falling, 255);
            case 4 -> argb(rising, 0, 255);
            default -> argb(255, 0, falling);
        };
    }

    private static int channel(double value) {
        return (int) Math.round(value * 255.0D);
    }

    private static int argb(int red, int green, int blue) {
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
