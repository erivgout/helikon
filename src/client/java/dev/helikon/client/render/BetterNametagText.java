package dev.helikon.client.render;

import dev.helikon.client.module.render.BetterNametags;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Minecraft-free composition of bounded local name-tag facts as stacked,
 * individually colored rows: name, health, armor, then distance/held item.
 */
public final class BetterNametagText {
    private static final double GIZMO_TEXTURE_PIXELS_PER_WORLD_UNIT = 16.0D;

    public static final int COLOR_NAME = 0xFFE5EDF5;
    public static final int COLOR_FRIEND = 0xFF80CBC4;
    public static final int COLOR_HEALTH_HIGH = 0xFF81C784;
    public static final int COLOR_HEALTH_MEDIUM = 0xFFFFD54F;
    public static final int COLOR_HEALTH_LOW = 0xFFE57373;
    public static final int COLOR_ARMOR = 0xFFB0BEC5;
    public static final int COLOR_DETAIL = 0xFF9AA1AB;

    private static final String VANILLA_NAMESPACE = "minecraft:";
    private static final String EMPTY_HAND_ID = "minecraft:air";

    private BetterNametagText() {
    }

    /** Rows ordered top to bottom; only enabled, non-empty facts produce a row. */
    public static List<Line> lines(Facts facts, BetterNametags.Options options, boolean friend) {
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(options, "options");
        List<Line> lines = new ArrayList<>(4);
        boolean markedFriend = options.friendStatus() && friend;
        lines.add(new Line(markedFriend ? facts.name() + " [Friend]" : facts.name(),
                markedFriend ? COLOR_FRIEND : COLOR_NAME));
        if (options.health()) {
            lines.add(new Line(String.format(Locale.ROOT, "♥ %.1f/%.1f", facts.health(), facts.maximumHealth()),
                    healthColor(facts.health() / facts.maximumHealth())));
        }
        if (options.armor() && facts.armor() > 0) {
            lines.add(new Line("Armor " + facts.armor(), COLOR_ARMOR));
        }
        String detail = detailLine(facts, options);
        if (!detail.isEmpty()) {
            lines.add(new Line(detail, COLOR_DETAIL));
        }
        return lines;
    }

    /** Rows-above-base for one line, so the first (name) line renders topmost. */
    public static int stackOffset(int index, int lineCount) {
        if (index < 0 || index >= lineCount) {
            throw new IllegalArgumentException("Invalid name-tag line index");
        }
        return lineCount - 1 - index;
    }

    /**
     * Converts the font's pixel line height and a desired pixel gap into the
     * world-space distance used by Minecraft's billboard-text gizmo renderer.
     */
    public static double worldLineSpacing(float textScale, int fontLineHeight, int gapPixels) {
        if (!Float.isFinite(textScale) || textScale <= 0.0F) {
            throw new IllegalArgumentException("textScale must be positive and finite");
        }
        if (fontLineHeight <= 0) {
            throw new IllegalArgumentException("fontLineHeight must be positive");
        }
        if (gapPixels < 0) {
            throw new IllegalArgumentException("gapPixels must not be negative");
        }
        return (fontLineHeight + gapPixels) * textScale / GIZMO_TEXTURE_PIXELS_PER_WORLD_UNIT;
    }

    static int healthColor(float fraction) {
        if (fraction >= 2.0F / 3.0F) {
            return COLOR_HEALTH_HIGH;
        }
        return fraction >= 1.0F / 3.0F ? COLOR_HEALTH_MEDIUM : COLOR_HEALTH_LOW;
    }

    private static String detailLine(Facts facts, BetterNametags.Options options) {
        StringBuilder text = new StringBuilder();
        if (options.distance()) {
            text.append(String.format(Locale.ROOT, "%.1fm", facts.distance()));
        }
        if (options.heldItem()) {
            String item = displayItem(facts.heldItemId());
            if (!item.isEmpty()) {
                if (!text.isEmpty()) {
                    text.append(" • ");
                }
                text.append(item);
            }
        }
        return text.toString();
    }

    /** Hides the empty hand and drops the vanilla namespace for readability. */
    private static String displayItem(String heldItemId) {
        if (heldItemId.isEmpty() || heldItemId.equals(EMPTY_HAND_ID)) {
            return "";
        }
        return heldItemId.startsWith(VANILLA_NAMESPACE) ? heldItemId.substring(VANILLA_NAMESPACE.length()) : heldItemId;
    }

    /** One rendered name-tag row and its ARGB text color. */
    public record Line(String text, int color) {
        public Line {
            text = Objects.requireNonNull(text, "text").trim();
            if (text.isEmpty()) {
                throw new IllegalArgumentException("Name-tag lines must not be blank");
            }
        }
    }

    public record Facts(String name, float health, float maximumHealth, int armor, double distance, String heldItemId) {
        public Facts {
            name = require(name, "name");
            heldItemId = heldItemId == null ? "" : heldItemId.trim();
            if (!Float.isFinite(health) || !Float.isFinite(maximumHealth) || maximumHealth <= 0.0F || health < 0.0F
                    || !Double.isFinite(distance) || distance < 0.0D || armor < 0) {
                throw new IllegalArgumentException("Invalid name-tag facts");
            }
        }
        private static String require(String value, String field) {
            String result = java.util.Objects.requireNonNull(value, field).trim();
            if (result.isEmpty()) {
                throw new IllegalArgumentException(field + " must not be blank");
            }
            return result;
        }
    }
}
