package dev.helikon.client.render;

/** Minecraft-free current-player range culling for entries retained by a prior BlockESP scan pass. */
public final class BlockEspRange {
    private BlockEspRange() {
    }

    public static boolean contains(BlockEspScanCursor.Position position, double playerX, double playerY, double playerZ,
                                   int horizontalRadius, int verticalRadius) {
        if (position == null || !Double.isFinite(playerX) || !Double.isFinite(playerY) || !Double.isFinite(playerZ)
                || horizontalRadius < 0 || verticalRadius < 0) {
            return false;
        }
        return Math.abs(position.x() + 0.5D - playerX) <= horizontalRadius
                && Math.abs(position.y() + 0.5D - playerY) <= verticalRadius
                && Math.abs(position.z() + 0.5D - playerZ) <= horizontalRadius;
    }
}
