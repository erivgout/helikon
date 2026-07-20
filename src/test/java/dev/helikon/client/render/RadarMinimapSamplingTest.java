package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadarMinimapSamplingTest {
    @Test
    void preservesFullPixelDetailForTheCachedTexture() {
        assertEquals(5_776, RadarMinimapSampling.cells(38, RadarProjection.Shape.SQUARE).size());
        assertTrue(RadarMinimapSampling.cells(38, RadarProjection.Shape.CIRCLE).size() < 5_776);
    }

    @Test
    void refreshesOnAConservativeCadenceAndMeaningfulMovement() {
        assertEquals(0L, RadarMinimapSampling.refreshBucket(19L));
        assertEquals(1L, RadarMinimapSampling.refreshBucket(20L));
        assertFalse(RadarMinimapSampling.movedFarEnough(10, 20, 13, 17));
        assertTrue(RadarMinimapSampling.movedFarEnough(10, 20, 14, 20));
    }

    @Test
    void quantizesRotatingMapsToFifteenDegreeSteps() {
        assertEquals(0, RadarMinimapSampling.yawBucket(120.0F, false));
        assertEquals(1, RadarMinimapSampling.yawBucket(14.0F, true));
        assertEquals(2, RadarMinimapSampling.yawBucket(23.0F, true));
    }
}
