package dev.helikon.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Bounded, Minecraft-free geometry for one local cosmetic aura ring. */
public final class LocalAuraGeometry {
    private static final int MINIMUM_SEGMENTS = 3;
    private static final int MAXIMUM_SEGMENTS = 64;

    private LocalAuraGeometry() {
    }

    public static List<Segment> ring(double centerX, double centerY, double centerZ, double radius, int segments) {
        requireFinite(centerX, "centerX");
        requireFinite(centerY, "centerY");
        requireFinite(centerZ, "centerZ");
        if (!Double.isFinite(radius) || radius <= 0.0D) {
            throw new IllegalArgumentException("radius must be finite and positive");
        }
        if (segments < MINIMUM_SEGMENTS || segments > MAXIMUM_SEGMENTS) {
            throw new IllegalArgumentException("segments must be between " + MINIMUM_SEGMENTS + " and " + MAXIMUM_SEGMENTS);
        }

        List<Segment> result = new ArrayList<>(segments);
        for (int index = 0; index < segments; index++) {
            Point from = point(centerX, centerY, centerZ, radius, index, segments);
            Point to = point(centerX, centerY, centerZ, radius, (index + 1) % segments, segments);
            result.add(new Segment(from, to));
        }
        return List.copyOf(result);
    }

    private static Point point(double centerX, double centerY, double centerZ, double radius, int index, int segments) {
        double angle = Math.PI * 2.0D * index / segments;
        return new Point(centerX + Math.cos(angle) * radius, centerY, centerZ + Math.sin(angle) * radius);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    public record Point(double x, double y, double z) {
        public Point {
            requireFinite(x, "x");
            requireFinite(y, "y");
            requireFinite(z, "z");
        }
    }

    public record Segment(Point from, Point to) {
        public Segment {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
        }
    }
}
