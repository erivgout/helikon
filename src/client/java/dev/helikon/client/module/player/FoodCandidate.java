package dev.helikon.client.module.player;

import java.util.Locale;
import java.util.Objects;

/** A safe, already-present hotbar food option described without Minecraft types. */
public record FoodCandidate(int slot, String itemId, int nutrition, float saturation, boolean alwaysEdible) {
    public FoodCandidate {
        if (slot < 0 || slot >= 9) {
            throw new IllegalArgumentException("slot must be a hotbar index");
        }
        itemId = Objects.requireNonNull(itemId, "itemId").trim().toLowerCase(Locale.ROOT);
        if (itemId.isEmpty() || itemId.length() > 255) {
            throw new IllegalArgumentException("itemId must be a non-empty identifier token");
        }
        if (nutrition < 1 || !Float.isFinite(saturation) || saturation < 0.0F) {
            throw new IllegalArgumentException("food values must be finite and positive");
        }
    }

    /** Matches Minecraft's 26.2 saturation gain: nutrition × modifier × 2. */
    public float saturationGain() {
        return nutrition * saturation * 2.0F;
    }
}
