package dev.helikon.client.render;

/** Minecraft-free pixel pattern for Helikon's procedural local cape texture. */
public final class LocalCapeTexturePattern {
    public static final int WIDTH = 64;
    public static final int HEIGHT = 32;

    private LocalCapeTexturePattern() {
    }

    /** Returns one opaque ARGB pixel in the fixed vanilla cape texture layout. */
    public static int argbAt(int x, int y, int primaryArgb, int accentArgb) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            throw new IllegalArgumentException("Cape pixel must be inside the " + WIDTH + "x" + HEIGHT + " texture");
        }
        int primary = opaque(primaryArgb);
        if (x == 0 || x == WIDTH - 1 || y == 0 || y == HEIGHT - 1) {
            return darken(primary);
        }
        return isHelikonEmblem(x, y) ? opaque(accentArgb) : primary;
    }

    /** Converts the pure ARGB representation into {@code NativeImage.setPixelABGR}'s component order. */
    public static int toAbgr(int argb) {
        int alpha = argb & 0xFF000000;
        int red = (argb >>> 16) & 0xFF;
        int green = argb & 0x0000FF00;
        int blue = (argb & 0x000000FF) << 16;
        return alpha | blue | green | red;
    }

    private static boolean isHelikonEmblem(int x, int y) {
        return (x == 28 || x == 35) && y >= 8 && y <= 23
                || y >= 14 && y <= 17 && x >= 28 && x <= 35;
    }

    private static int opaque(int argb) {
        return 0xFF000000 | (argb & 0x00FFFFFF);
    }

    private static int darken(int argb) {
        int red = ((argb >>> 16) & 0xFF) * 3 / 4;
        int green = ((argb >>> 8) & 0xFF) * 3 / 4;
        int blue = (argb & 0xFF) * 3 / 4;
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
