package dev.helikon.client.render;

import java.util.Set;

/** Minecraft-free zero-work gate for disabled or empty local BlockESP scans. */
public final class BlockEspScanPolicy {
    private BlockEspScanPolicy() {
    }

    public static boolean shouldScan(boolean enabled, Set<String> targetBlocks) {
        return enabled && targetBlocks != null && !targetBlocks.isEmpty();
    }
}
