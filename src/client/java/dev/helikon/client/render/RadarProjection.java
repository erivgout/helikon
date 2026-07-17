package dev.helikon.client.render;

/** Minecraft-free horizontal world-to-radar projection with bounded circular or square clipping. */
public final class RadarProjection {
    private RadarProjection() {
    }

    public static Point project(double relativeX, double relativeZ, double playerYawDegrees, double zoom,
                                double radiusPixels, boolean rotate, Shape shape) {
        if (!Double.isFinite(relativeX) || !Double.isFinite(relativeZ) || !Double.isFinite(playerYawDegrees)
                || !Double.isFinite(zoom) || zoom <= 0.0D || !Double.isFinite(radiusPixels) || radiusPixels <= 0.0D
                || shape == null) {
            throw new IllegalArgumentException("Invalid radar projection input");
        }
        double x = relativeX;
        double z = relativeZ;
        if (rotate) {
            double radians = Math.toRadians(playerYawDegrees);
            double cosine = Math.cos(radians);
            double sine = Math.sin(radians);
            double rotatedX = x * cosine + z * sine;
            z = z * cosine - x * sine;
            x = rotatedX;
        }
        double projectedX = x / zoom * radiusPixels;
        double projectedY = z / zoom * radiusPixels;
        boolean visible = shape == Shape.SQUARE
                ? Math.abs(projectedX) <= radiusPixels && Math.abs(projectedY) <= radiusPixels
                : projectedX * projectedX + projectedY * projectedY <= radiusPixels * radiusPixels;
        return new Point(projectedX, projectedY, visible);
    }

    public enum Shape {
        CIRCLE,
        SQUARE
    }

    public record Point(double x, double y, boolean visible) {
    }
}
