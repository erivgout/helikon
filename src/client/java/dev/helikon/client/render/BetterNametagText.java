package dev.helikon.client.render;

import dev.helikon.client.module.render.BetterNametags;

import java.util.Locale;
import java.util.Objects;

/** Minecraft-free composition of bounded local name-tag facts. */
public final class BetterNametagText {
    private BetterNametagText() {
    }

    public static String format(Facts facts, BetterNametags.Options options, boolean friend) {
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(options, "options");
        StringBuilder text = new StringBuilder(facts.name());
        if (options.friendStatus() && friend) {
            text.append(" [Friend]");
        }
        if (options.health()) {
            text.append(String.format(Locale.ROOT, " %.1f/%.1f", facts.health(), facts.maximumHealth()));
        }
        if (options.armor()) {
            text.append(" A:").append(facts.armor());
        }
        if (options.distance()) {
            text.append(String.format(Locale.ROOT, " %.1fm", facts.distance()));
        }
        if (options.heldItem() && !facts.heldItemId().isEmpty()) {
            text.append(" ").append(facts.heldItemId());
        }
        return text.toString();
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
