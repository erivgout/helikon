package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalAuraGeometryTest {
    @Test
    void buildsABoundedClosedRingAtTheRequestedRadius() {
        List<LocalAuraGeometry.Segment> ring = LocalAuraGeometry.ring(10.0D, 2.0D, -3.0D, 2.0D, 4);

        assertEquals(4, ring.size());
        assertEquals(new LocalAuraGeometry.Point(12.0D, 2.0D, -3.0D), ring.getFirst().from());
        assertEquals(new LocalAuraGeometry.Point(10.0D, 2.0D, -1.0D), ring.getFirst().to());
        assertEquals(ring.getFirst().from(), ring.getLast().to());
    }

    @Test
    void rejectsNonFiniteOrUnboundedGeometry() {
        assertThrows(IllegalArgumentException.class, () -> LocalAuraGeometry.ring(Double.NaN, 0.0D, 0.0D, 1.0D, 12));
        assertThrows(IllegalArgumentException.class, () -> LocalAuraGeometry.ring(0.0D, 0.0D, 0.0D, 0.0D, 12));
        assertThrows(IllegalArgumentException.class, () -> LocalAuraGeometry.ring(0.0D, 0.0D, 0.0D, 1.0D, 2));
        assertThrows(IllegalArgumentException.class, () -> LocalAuraGeometry.ring(0.0D, 0.0D, 0.0D, 1.0D, 65));
    }
}
