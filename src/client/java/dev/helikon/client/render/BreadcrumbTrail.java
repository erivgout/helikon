package dev.helikon.client.render;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** Bounded, timestamped local position history with distance sampling. */
public final class BreadcrumbTrail {
    private final Deque<Point> points = new ArrayDeque<>();
    private final Iterable<Point> readOnlyPoints = Collections.unmodifiableCollection(points);

    /** Adds a sample only when it is far enough from the newest retained point. */
    public void sample(double x, double y, double z, long timestampMillis, double minimumDistance,
                       int maximumPoints, long maximumAgeMillis) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Double.isFinite(minimumDistance) || minimumDistance < 0.0D
                || maximumPoints < 1 || maximumAgeMillis < 0L) {
            throw new IllegalArgumentException("Invalid breadcrumb sample limits");
        }
        if (!points.isEmpty() && timestampMillis < points.getLast().timestampMillis()) {
            points.clear();
        }
        prune(timestampMillis, maximumAgeMillis);

        Point newest = points.peekLast();
        if (newest == null || squaredDistance(newest, x, y, z) >= minimumDistance * minimumDistance) {
            points.addLast(new Point(x, y, z, timestampMillis));
        }
        while (points.size() > maximumPoints) {
            points.removeFirst();
        }
    }

    public void clear() {
        points.clear();
    }

    public List<Point> snapshot() {
        return List.copyOf(points);
    }

    /** Read-only live iteration for the single-threaded renderer, without allocating a render-frame snapshot. */
    public Iterable<Point> points() {
        return readOnlyPoints;
    }

    public int size() {
        return points.size();
    }

    private void prune(long timestampMillis, long maximumAgeMillis) {
        long cutoff = timestampMillis < Long.MIN_VALUE + maximumAgeMillis
                ? Long.MIN_VALUE : timestampMillis - maximumAgeMillis;
        while (!points.isEmpty() && points.getFirst().timestampMillis() < cutoff) {
            points.removeFirst();
        }
    }

    private static double squaredDistance(Point point, double x, double y, double z) {
        double deltaX = point.x() - x;
        double deltaY = point.y() - y;
        double deltaZ = point.z() - z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    public record Point(double x, double y, double z, long timestampMillis) {
    }
}
