package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlintColorTest {
    @Test
    void cyclesThroughFullSaturationPrimaryColors() {
        assertEquals(0xFFFF0000, GlintColor.rainbow(0L, 1.0D));
        assertEquals(0xFF01FF00, GlintColor.rainbow(333L, 1.0D));
        assertEquals(0xFF0100FF, GlintColor.rainbow(667L, 1.0D));
        assertEquals(0xFFFF0000, GlintColor.rainbow(1_000L, 1.0D));
    }

    @Test
    void rejectsInvalidTimeAndCycleRates() {
        assertThrows(IllegalArgumentException.class, () -> GlintColor.rainbow(-1L, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> GlintColor.rainbow(0L, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> GlintColor.rainbow(0L, Double.NaN));
    }
}
