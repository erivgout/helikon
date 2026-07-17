package dev.helikon.client.render;

/** Keeps a bounded scan cube stable through ordinary movement, then recenters it deliberately. */
public final class BlockEspScanAnchor {
    private Integer centerX;
    private Integer centerY;
    private Integer centerZ;

    /** Returns the current scan region and whether it was newly centered this update. */
    public Update update(int playerX, int playerY, int playerZ, int horizontalRadius, int verticalRadius,
                         boolean allowRecenter) {
        if (horizontalRadius < 0 || verticalRadius < 0) {
            throw new IllegalArgumentException("Scan radii must not be negative");
        }
        boolean movedBeyondThreshold = centerX != null && (Math.abs(playerX - centerX) > recenterDistance(horizontalRadius)
                || Math.abs(playerY - centerY) > recenterDistance(verticalRadius)
                || Math.abs(playerZ - centerZ) > recenterDistance(horizontalRadius));
        boolean changed = centerX == null || (allowRecenter && movedBeyondThreshold);
        if (changed) {
            centerX = playerX;
            centerY = playerY;
            centerZ = playerZ;
        }
        return new Update(new BlockEspScanCursor.Region(centerX, centerY, centerZ, horizontalRadius, verticalRadius), changed);
    }

    public void clear() {
        centerX = null;
        centerY = null;
        centerZ = null;
    }

    private static int recenterDistance(int radius) {
        return Math.max(8, radius / 2);
    }

    public record Update(BlockEspScanCursor.Region region, boolean changed) {
    }
}
