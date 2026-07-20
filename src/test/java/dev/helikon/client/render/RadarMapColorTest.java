package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RadarMapColorTest {
    @Test
    void assignsRecognizableOpaqueTerrainColors() {
        assertEquals(0xFF000000, RadarMapColor.forBlock("minecraft:water", 0) & 0xFF000000);
        assertNotEquals(RadarMapColor.forBlock("minecraft:water", 0),
                RadarMapColor.forBlock("minecraft:grass_block", 0));
        assertNotEquals(RadarMapColor.forBlock("minecraft:stone", -10),
                RadarMapColor.forBlock("minecraft:stone", 10));
    }

    @Test
    void nativeMapColorsGainOpaqueElevationAndSlopeRelief() {
        int flat = RadarMapColor.forMapColor(0xFF5E9E43, 0, 0);
        int uphill = RadarMapColor.forMapColor(0xFF5E9E43, 4, 2);
        int downhill = RadarMapColor.forMapColor(0xFF5E9E43, -4, -2);

        assertEquals(0xFF000000, flat & 0xFF000000);
        assertNotEquals(flat, uphill);
        assertNotEquals(flat, downhill);
        assertNotEquals(uphill, downhill);
    }
}
