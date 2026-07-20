package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadarProjectionTest {
    @Test
    void projectsAndClipsCircleAndSquareModesDeterministically() {
        RadarProjection.Point point = RadarProjection.project(32.0D, 0.0D, 0.0D,
                64.0D, 40.0D, false, RadarProjection.Shape.CIRCLE);
        assertEquals(20.0D, point.x());
        assertEquals(0.0D, point.y());
        assertTrue(point.visible());

        assertFalse(RadarProjection.project(64.0D, 64.0D, 0.0D,
                64.0D, 40.0D, false, RadarProjection.Shape.CIRCLE).visible());
        assertTrue(RadarProjection.project(64.0D, 64.0D, 0.0D,
                64.0D, 40.0D, false, RadarProjection.Shape.SQUARE).visible());
    }

    @Test
    void rotatesWithYawWhenEnabled() {
        RadarProjection.Point point = RadarProjection.project(10.0D, 0.0D, 90.0D,
                10.0D, 20.0D, true, RadarProjection.Shape.SQUARE);
        assertEquals(0.0D, point.x(), 0.000001D);
        assertEquals(-20.0D, point.y(), 0.000001D);
    }

    @Test
    void convertsMinecraftYawToEightWayHeadings() {
        assertEquals("S", RadarProjection.heading(0.0D));
        assertEquals("W", RadarProjection.heading(90.0D));
        assertEquals("N", RadarProjection.heading(180.0D));
        assertEquals("E", RadarProjection.heading(-90.0D));
        assertEquals("SE", RadarProjection.heading(-45.0D));
    }
}
