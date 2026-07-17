package dev.helikon.client.module.render;

import java.util.Locale;

/**
 * Minecraft-free formatting and coloring for the local player's health readout.
 * All inputs are already-observed client values; nothing here touches Minecraft
 * classes so the display rules stay unit-testable.
 */
public final class HealthReadout {
    private static final int LOW_COLOR = 0xFFFF5555;
    private static final int MID_COLOR = 0xFFFFEE55;
    private static final int HIGH_COLOR = 0xFF55FF55;
    private static final String UNAVAILABLE = "--";

    private HealthReadout() {
    }

    /**
     * Builds the compact readout text, for example {@code "18/20 +4"}.
     * Returns {@link #UNAVAILABLE} when the observed health is not a usable value.
     */
    public static String text(float health, float maxHealth, float absorption,
                              boolean showMax, boolean showAbsorption, boolean showDecimals) {
        if (!Float.isFinite(health) || health < 0.0F) {
            return UNAVAILABLE;
        }
        StringBuilder builder = new StringBuilder(number(health, showDecimals));
        if (showMax && Float.isFinite(maxHealth) && maxHealth > 0.0F) {
            builder.append('/').append(number(maxHealth, showDecimals));
        }
        if (showAbsorption && Float.isFinite(absorption) && absorption > 0.0F) {
            builder.append(" +").append(number(absorption, showDecimals));
        }
        return builder.toString();
    }

    /**
     * Returns a fully opaque ARGB color interpolated from red (empty) through
     * yellow (half) to green (full) based on the remaining health fraction.
     */
    public static int color(float health, float maxHealth) {
        float fraction = fraction(health, maxHealth);
        if (fraction <= 0.5F) {
            return lerp(LOW_COLOR, MID_COLOR, fraction / 0.5F);
        }
        return lerp(MID_COLOR, HIGH_COLOR, (fraction - 0.5F) / 0.5F);
    }

    static float fraction(float health, float maxHealth) {
        if (!Float.isFinite(health) || !Float.isFinite(maxHealth) || maxHealth <= 0.0F || health <= 0.0F) {
            return 0.0F;
        }
        return Math.min(1.0F, health / maxHealth);
    }

    private static String number(float value, boolean showDecimals) {
        if (showDecimals) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return Integer.toString(Math.round(value));
    }

    private static int lerp(int from, int to, float t) {
        float clamped = Math.clamp(t, 0.0F, 1.0F);
        int a = channel(from, 24, to, clamped);
        int r = channel(from, 16, to, clamped);
        int g = channel(from, 8, to, clamped);
        int b = channel(from, 0, to, clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int shift, int to, float t) {
        int start = (from >> shift) & 0xFF;
        int end = (to >> shift) & 0xFF;
        return Math.round(start + (end - start) * t);
    }
}
