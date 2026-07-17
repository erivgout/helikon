package dev.helikon.client.module.player;

import java.util.Objects;

/** Minecraft-free armor facts used by AutoArmor's deterministic selection policy. */
public record ArmorCandidate(int menuSlot, ArmorSlot equipmentSlot, double armor, double toughness,
                             double durabilityFraction, boolean bindingCurse) {
    public ArmorCandidate {
        if (menuSlot < 0) {
            throw new IllegalArgumentException("menuSlot must be non-negative");
        }
        equipmentSlot = Objects.requireNonNull(equipmentSlot, "equipmentSlot");
        if (!Double.isFinite(armor) || armor < 0.0D || !Double.isFinite(toughness) || toughness < 0.0D
                || !Double.isFinite(durabilityFraction) || durabilityFraction < 0.0D || durabilityFraction > 1.0D) {
            throw new IllegalArgumentException("armor facts must be finite and non-negative");
        }
    }

    public double score(boolean preferDurability) {
        return armor + toughness * 0.25D + (preferDurability ? durabilityFraction * 0.1D : 0.0D);
    }
}
