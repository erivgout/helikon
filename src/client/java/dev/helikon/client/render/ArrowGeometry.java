package dev.helikon.client.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft-free rasterization of a small edge-pointing arrow triangle into horizontal fill spans.
 *
 * <p>The triangle tip points outward along the supplied screen-space direction, starting at a ring
 * of {@code ringRadius} pixels around the center. Each returned {@link Span} is a single scanline
 * an adapter fills with {@code graphics.fill(xStart, y, xEnd, y + 1, color)}; {@code xEnd} is
 * exclusive to match Minecraft's fill convention.
 */
public final class ArrowGeometry {
    private ArrowGeometry() {
    }

    public static List<Span> build(double centerX, double centerY, double ringRadius,
                                   double length, double halfWidth, double directionX, double directionY) {
        if (!allFinite(centerX, centerY, ringRadius, length, halfWidth, directionX, directionY)
                || ringRadius < 0.0D || length <= 0.0D || halfWidth <= 0.0D) {
            throw new IllegalArgumentException("Invalid arrow geometry input");
        }
        double magnitude = Math.sqrt(directionX * directionX + directionY * directionY);
        if (magnitude < 1.0e-9D) {
            throw new IllegalArgumentException("Arrow direction must be non-zero");
        }
        double dirX = directionX / magnitude;
        double dirY = directionY / magnitude;
        double perpX = -dirY;
        double perpY = dirX;

        double baseCenterX = centerX + dirX * ringRadius;
        double baseCenterY = centerY + dirY * ringRadius;
        double tipX = baseCenterX + dirX * length;
        double tipY = baseCenterY + dirY * length;
        double leftX = baseCenterX + perpX * halfWidth;
        double leftY = baseCenterY + perpY * halfWidth;
        double rightX = baseCenterX - perpX * halfWidth;
        double rightY = baseCenterY - perpY * halfWidth;

        return rasterize(tipX, tipY, leftX, leftY, rightX, rightY);
    }

    private static List<Span> rasterize(double ax, double ay, double bx, double by, double cx, double cy) {
        double minY = Math.min(ay, Math.min(by, cy));
        double maxY = Math.max(ay, Math.max(by, cy));
        int firstRow = (int) Math.floor(minY);
        int lastRow = (int) Math.ceil(maxY);
        List<Span> spans = new ArrayList<>();
        for (int row = firstRow; row < lastRow; row++) {
            double sampleY = row + 0.5D;
            double[] crossings = {
                    intersect(sampleY, ax, ay, bx, by),
                    intersect(sampleY, bx, by, cx, cy),
                    intersect(sampleY, cx, cy, ax, ay)
            };
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            for (double crossing : crossings) {
                if (Double.isNaN(crossing)) {
                    continue;
                }
                minX = Math.min(minX, crossing);
                maxX = Math.max(maxX, crossing);
            }
            if (minX > maxX) {
                continue;
            }
            int startX = (int) Math.round(minX);
            int endX = (int) Math.round(maxX);
            if (endX <= startX) {
                endX = startX + 1;
            }
            spans.add(new Span(row, startX, endX));
        }
        return List.copyOf(spans);
    }

    private static double intersect(double y, double x0, double y0, double x1, double y1) {
        double lowY = Math.min(y0, y1);
        double highY = Math.max(y0, y1);
        if (y < lowY || y > highY || y0 == y1) {
            return Double.NaN;
        }
        double t = (y - y0) / (y1 - y0);
        return x0 + t * (x1 - x0);
    }

    private static boolean allFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    /** A single horizontal fill span; {@code xEnd} is exclusive. */
    public record Span(int y, int xStart, int xEnd) {
    }
}
