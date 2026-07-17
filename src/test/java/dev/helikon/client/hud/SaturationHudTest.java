package dev.helikon.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaturationHudTest {
    @Test
    void formatsFiniteLocalSaturationAndMasksInvalidFacts() {
        assertEquals("Saturation 5.5", SaturationHud.format(5.5F));
        assertEquals("Saturation 0.0", SaturationHud.format(0.0F));
        assertEquals("Saturation --", SaturationHud.format(Float.NaN));
        assertEquals("Saturation --", SaturationHud.format(-1.0F));
    }
}
