package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplosionSphereGeometryTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void buildsThreeClosedGreatCirclesOnTheSphereSurface() {
        double centerX = 5.0D;
        double centerY = 64.0D;
        double centerZ = -12.0D;
        double radius = 8.0D;
        int segments = 16;

        List<ExplosionSphereGeometry.Segment> sphere = ExplosionSphereGeometry.wireframe(centerX, centerY, centerZ,
                radius, segments);

        assertEquals(segments * 3, sphere.size());
        for (ExplosionSphereGeometry.Segment segment : sphere) {
            assertOnSurface(segment.from(), centerX, centerY, centerZ, radius);
            assertOnSurface(segment.to(), centerX, centerY, centerZ, radius);
        }
    }

    @Test
    void rejectsNonFiniteOrUnboundedGeometry() {
        assertThrows(IllegalArgumentException.class,
                () -> ExplosionSphereGeometry.wireframe(Double.NaN, 0.0D, 0.0D, 4.0D, 16));
        assertThrows(IllegalArgumentException.class,
                () -> ExplosionSphereGeometry.wireframe(0.0D, 0.0D, 0.0D, 0.0D, 16));
        assertThrows(IllegalArgumentException.class,
                () -> ExplosionSphereGeometry.wireframe(0.0D, 0.0D, 0.0D, 4.0D, 7));
        assertThrows(IllegalArgumentException.class,
                () -> ExplosionSphereGeometry.wireframe(0.0D, 0.0D, 0.0D, 4.0D, 65));
    }

    private static void assertOnSurface(ExplosionSphereGeometry.Point point, double cx, double cy, double cz,
                                        double radius) {
        double distance = Math.sqrt(Math.pow(point.x() - cx, 2) + Math.pow(point.y() - cy, 2)
                + Math.pow(point.z() - cz, 2));
        assertTrue(Math.abs(distance - radius) < EPSILON, "point must lie on the sphere surface: distance=" + distance);
    }
}
