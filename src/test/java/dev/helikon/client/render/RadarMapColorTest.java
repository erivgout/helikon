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
}
