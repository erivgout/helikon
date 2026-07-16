package dev.helikon.client.module.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrosshairGeometryTest {
    @Test
    void buildsFourCenterAlignedArmsWithTheRequestedGap() {
        List<CrosshairGeometry.Rect> arms = CrosshairGeometry.arms(100, 50, 5, 2, 3, 1);

        assertEquals(List.of(
                new CrosshairGeometry.Rect(92, 49, 5, 3),
                new CrosshairGeometry.Rect(104, 49, 5, 3),
                new CrosshairGeometry.Rect(99, 42, 3, 5),
                new CrosshairGeometry.Rect(99, 54, 3, 5)
        ), arms);
    }

    @Test
    void rejectsInvalidDimensions() {
        assertThrows(IllegalArgumentException.class, () -> CrosshairGeometry.arms(0, 0, 0, 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> CrosshairGeometry.arms(0, 0, 1, -1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> CrosshairGeometry.arms(0, 0, 1, 1, 0, 0));
    }
}
