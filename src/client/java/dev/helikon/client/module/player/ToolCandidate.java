package dev.helikon.client.module.player;

/** Minecraft-free description of one hotbar item while selecting a mining tool. */
public record ToolCandidate(int slot, double destroySpeed, boolean correctForDrops, int remainingDurability) {
    public ToolCandidate {
        if (slot < 0 || slot >= 9) {
            throw new IllegalArgumentException("slot must be a hotbar index");
        }
        if (!Double.isFinite(destroySpeed) || destroySpeed < 0.0) {
            throw new IllegalArgumentException("destroySpeed must be finite and non-negative");
        }
        if (remainingDurability < 0) {
            throw new IllegalArgumentException("remainingDurability must be non-negative");
        }
    }
}
