package dev.helikon.client.combat;

/** A player-owned, locally observed restorative potion in a hotbar slot. */
public record PotionCandidate(int slot, String potionId, Kind kind, boolean restorative) {
    public enum Kind {
        SPLASH,
        DRINK
    }

    public PotionCandidate {
        if (slot < 0 || slot > 8 || potionId == null || potionId.isBlank() || kind == null) {
            throw new IllegalArgumentException("potion candidate facts are invalid");
        }
    }
}
