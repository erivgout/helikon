package dev.helikon.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bounded, Minecraft-free geometry for a wireframe explosion-radius sphere.
 *
 * <p>The sphere is approximated by three orthogonal great circles (horizontal,
 * and two vertical planes), which reads clearly as a sphere while keeping the
 * segment count small and bounded.
 */
public final class ExplosionSphereGeometry {
    private static final int MINIMUM_SEGMENTS = 8;
    private static final int MAXIMUM_SEGMENTS = 64;

    private ExplosionSphereGeometry() {
    }

    /**
     * Builds the line segments of a wireframe sphere.
     *
     * @param segments segments per great circle; total segment count is three times this value
     */
    public static List<Segment> wireframe(double centerX, double centerY, double centerZ, double radius, int segments) {
        requireFinite(centerX, "centerX");
        requireFinite(centerY, "centerY");
        requireFinite(centerZ, "centerZ");
        if (!Double.isFinite(radius) || radius <= 0.0D) {
            throw new IllegalArgumentException("radius must be finite and positive");
        }
        if (segments < MINIMUM_SEGMENTS || segments > MAXIMUM_SEGMENTS) {
            throw new IllegalArgumentException("segments must be between " + MINIMUM_SEGMENTS + " and " + MAXIMUM_SEGMENTS);
        }

        List<Segment> result = new ArrayList<>(segments * 3);
        addRing(result, Plane.HORIZONTAL, centerX, centerY, centerZ, radius, segments);
        addRing(result, Plane.VERTICAL_X, centerX, centerY, centerZ, radius, segments);
        addRing(result, Plane.VERTICAL_Z, centerX, centerY, centerZ, radius, segments);
        return List.copyOf(result);
    }

    private static void addRing(List<Segment> result, Plane plane, double centerX, double centerY, double centerZ,
                                double radius, int segments) {
        for (int index = 0; index < segments; index++) {
            Point from = point(plane, centerX, centerY, centerZ, radius, index, segments);
            Point to = point(plane, centerX, centerY, centerZ, radius, (index + 1) % segments, segments);
            result.add(new Segment(from, to));
        }
    }

    private static Point point(Plane plane, double centerX, double centerY, double centerZ, double radius,
                               int index, int segments) {
        double angle = Math.PI * 2.0D * index / segments;
        double cos = Math.cos(angle) * radius;
        double sin = Math.sin(angle) * radius;
        return switch (plane) {
            case HORIZONTAL -> new Point(centerX + cos, centerY, centerZ + sin);
            case VERTICAL_X -> new Point(centerX + cos, centerY + sin, centerZ);
            case VERTICAL_Z -> new Point(centerX, centerY + cos, centerZ + sin);
        };
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private enum Plane {
        HORIZONTAL,
        VERTICAL_X,
        VERTICAL_Z
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
