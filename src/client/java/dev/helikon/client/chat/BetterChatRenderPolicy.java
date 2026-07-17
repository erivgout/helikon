package dev.helikon.client.chat;

/** Minecraft-free calculations for BetterChat's bounded display settings. */
public final class BetterChatRenderPolicy {
    public static final int VANILLA_HISTORY_LIMIT = 100;
    public static final int VANILLA_VISIBLE_TICKS = 200;
    public static final double VANILLA_FADE_MULTIPLIER = 10.0D;
    public static final double VANILLA_LINE_HEIGHT = 9.0D;
    public static final double COMPACT_LINE_HEIGHT = 7.0D;

    private BetterChatRenderPolicy() {
    }

    public static int historyLimit(boolean enabled, double configuredLimit) {
        if (!enabled) {
            return VANILLA_HISTORY_LIMIT;
        }
        return Math.clamp((int) Math.round(configuredLimit), VANILLA_HISTORY_LIMIT, 2_000);
    }

    /** Returns the full alpha-calculator lifetime, including its fully visible and fading periods. */
    public static int totalLifetimeTicks(boolean enabled, double visibleSeconds, double fadeSeconds) {
        if (!enabled) {
            return VANILLA_VISIBLE_TICKS;
        }
        return visibleBeforeFadeTicks(visibleSeconds) + fadeTicks(fadeSeconds);
    }

    /** Converts an adjustable fade period into Minecraft's verified alpha-calculator multiplier. */
    public static double fadeMultiplier(boolean enabled, double visibleSeconds, double fadeSeconds) {
        if (!enabled) {
            return VANILLA_FADE_MULTIPLIER;
        }
        return (double) totalLifetimeTicks(true, visibleSeconds, fadeSeconds) / fadeTicks(fadeSeconds);
    }

    public static double lineHeight(boolean enabled, boolean compact, double vanillaLineHeight) {
        return enabled && compact ? COMPACT_LINE_HEIGHT : vanillaLineHeight;
    }

    private static int visibleBeforeFadeTicks(double seconds) {
        return Math.clamp((int) Math.round(seconds * 20.0D), 20, 12_000);
    }

    private static int fadeTicks(double seconds) {
        return Math.clamp((int) Math.round(seconds * 20.0D), 1, 1_200);
    }
}
