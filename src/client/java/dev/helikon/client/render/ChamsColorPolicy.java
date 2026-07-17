package dev.helikon.client.render;

/**
 * Minecraft-free color selection for the Chams occlusion-visible silhouette.
 * Friends keep their configured color; otherwise the health gradient (when
 * enabled for a living target) or the base color is used. Every result is
 * forced opaque because the vanilla entity-outline pass treats a zero color as
 * "no outline".
 */
public final class ChamsColorPolicy {
    private static final int OPAQUE_MASK = 0xFF000000;

    private ChamsColorPolicy() {
    }

    /**
     * @param friend        whether the target is a locally saved friend
     * @param friendColor   configured ARGB friend color
     * @param healthColor   whether health-based coloring is enabled
     * @param living        whether the target exposes a health value
     * @param healthFraction current health divided by max health, clamped to [0, 1]
     * @param baseColor     configured ARGB base color
     * @return an opaque ARGB color for the outline pass
     */
    public static int colorFor(boolean friend, int friendColor, boolean healthColor, boolean living,
                               double healthFraction, int baseColor) {
        if (friend) {
            return opaque(friendColor);
        }
        if (healthColor && living) {
            return opaque(healthGradient(healthFraction));
        }
        return opaque(baseColor);
    }

    /** Interpolates green (full health) to red (empty health) for a clamped fraction. */
    public static int healthGradient(double healthFraction) {
        double fraction = clamp(healthFraction);
        int red = (int) Math.round((1.0D - fraction) * 255.0D);
        int green = (int) Math.round(fraction * 255.0D);
        return OPAQUE_MASK | (red << 16) | (green << 8);
    }

    private static double clamp(double fraction) {
        if (!Double.isFinite(fraction)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, fraction));
    }

    private static int opaque(int color) {
        return OPAQUE_MASK | color;
    }
}
