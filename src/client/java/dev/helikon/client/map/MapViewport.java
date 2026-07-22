package dev.helikon.client.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Minecraft-free north-up pan/zoom projection for the full-screen map. */
public final class MapViewport {
    public static final double MINIMUM_PIXELS_PER_BLOCK = 0.25D;
    public static final double MAXIMUM_PIXELS_PER_BLOCK = 8.0D;
    public static final int MAXIMUM_VISIBLE_REGIONS = 128;

    private double centerX;
    private double centerZ;
    private double pixelsPerBlock;

    public MapViewport(double centerX, double centerZ, double pixelsPerBlock) {
        requireFinite(centerX, "centerX");
        requireFinite(centerZ, "centerZ");
        requireFinite(pixelsPerBlock, "pixelsPerBlock");
        if (pixelsPerBlock <= 0.0D) {
            throw new IllegalArgumentException("Map zoom must be positive");
        }
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.pixelsPerBlock = Math.clamp(pixelsPerBlock,
                MINIMUM_PIXELS_PER_BLOCK, MAXIMUM_PIXELS_PER_BLOCK);
    }

    public double centerX() {
        return centerX;
    }

    public double centerZ() {
        return centerZ;
    }

    public double pixelsPerBlock() {
        return pixelsPerBlock;
    }

    public void recenter(double worldX, double worldZ) {
        requireFinite(worldX, "worldX");
        requireFinite(worldZ, "worldZ");
        centerX = worldX;
        centerZ = worldZ;
    }

    /** Moves the map with a drag gesture; dragging right reveals terrain west of the old center. */
    public void panPixels(double deltaX, double deltaY) {
        requireFinite(deltaX, "deltaX");
        requireFinite(deltaY, "deltaY");
        centerX -= deltaX / pixelsPerBlock;
        centerZ -= deltaY / pixelsPerBlock;
    }

    public void panBlocks(double deltaX, double deltaZ) {
        requireFinite(deltaX, "deltaX");
        requireFinite(deltaZ, "deltaZ");
        centerX += deltaX;
        centerZ += deltaZ;
    }

    public void zoomAt(double factor, double screenX, double screenY, int width, int height) {
        requireFinite(factor, "factor");
        if (factor <= 0.0D || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Map zoom input must be positive");
        }
        WorldPoint anchored = screenToWorld(screenX, screenY, width, height);
        pixelsPerBlock = Math.clamp(pixelsPerBlock * factor,
                MINIMUM_PIXELS_PER_BLOCK, MAXIMUM_PIXELS_PER_BLOCK);
        WorldPoint after = screenToWorld(screenX, screenY, width, height);
        centerX += anchored.x() - after.x();
        centerZ += anchored.z() - after.z();
    }

    public ScreenPoint worldToScreen(double worldX, double worldZ, int width, int height) {
        requireViewport(width, height);
        requireFinite(worldX, "worldX");
        requireFinite(worldZ, "worldZ");
        return new ScreenPoint(width * 0.5D + (worldX - centerX) * pixelsPerBlock,
                height * 0.5D + (worldZ - centerZ) * pixelsPerBlock);
    }

    public WorldPoint screenToWorld(double screenX, double screenY, int width, int height) {
        requireViewport(width, height);
        requireFinite(screenX, "screenX");
        requireFinite(screenY, "screenY");
        return new WorldPoint(centerX + (screenX - width * 0.5D) / pixelsPerBlock,
                centerZ + (screenY - height * 0.5D) / pixelsPerBlock);
    }

    public List<RegionCoordinate> visibleRegions(int width, int height) {
        requireViewport(width, height);
        WorldPoint topLeft = screenToWorld(0.0D, 0.0D, width, height);
        WorldPoint bottomRight = screenToWorld(width, height, width, height);
        int minimumX = MapRegion.regionCoordinateForBlock((int) Math.floor(topLeft.x()));
        int maximumX = MapRegion.regionCoordinateForBlock((int) Math.floor(bottomRight.x()));
        int minimumZ = MapRegion.regionCoordinateForBlock((int) Math.floor(topLeft.z()));
        int maximumZ = MapRegion.regionCoordinateForBlock((int) Math.floor(bottomRight.z()));
        List<RegionCoordinate> regions = new ArrayList<>();
        for (int regionZ = minimumZ; regionZ <= maximumZ; regionZ++) {
            for (int regionX = minimumX; regionX <= maximumX; regionX++) {
                regions.add(new RegionCoordinate(regionX, regionZ));
            }
        }
        double centerRegionX = centerX / MapRegion.SIZE;
        double centerRegionZ = centerZ / MapRegion.SIZE;
        regions.sort(Comparator.comparingDouble(region ->
                distanceSquared(region.x() + 0.5D, region.z() + 0.5D, centerRegionX, centerRegionZ)));
        if (regions.size() > MAXIMUM_VISIBLE_REGIONS) {
            return List.copyOf(regions.subList(0, MAXIMUM_VISIBLE_REGIONS));
        }
        return List.copyOf(regions);
    }

    private static double distanceSquared(double x, double z, double centerX, double centerZ) {
        double deltaX = x - centerX;
        double deltaZ = z - centerZ;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private static void requireViewport(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Map viewport dimensions must be positive");
        }
    }

    private static void requireFinite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
    }

    public record ScreenPoint(double x, double y) {
    }

    public record WorldPoint(double x, double z) {
    }

    public record RegionCoordinate(int x, int z) {
    }
}

