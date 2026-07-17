package dev.helikon.client.module.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthReadoutTest {
    @Test
    void showsRoundedHealthWithMaximum() {
        assertEquals("18/20", HealthReadout.text(18.4F, 20.0F, 0.0F, true, true, false));
    }

    @Test
    void hidesMaximumWhenDisabled() {
        assertEquals("18", HealthReadout.text(18.0F, 20.0F, 0.0F, false, true, false));
    }

    @Test
    void appendsAbsorptionOnlyWhenPresentAndEnabled() {
        assertEquals("20/20 +4", HealthReadout.text(20.0F, 20.0F, 4.0F, true, true, false));
        assertEquals("20/20", HealthReadout.text(20.0F, 20.0F, 0.0F, true, true, false));
        assertEquals("20/20", HealthReadout.text(20.0F, 20.0F, 4.0F, true, false, false));
    }

    @Test
    void showsOneDecimalWhenRequested() {
        assertEquals("18.5/20.0 +2.5", HealthReadout.text(18.5F, 20.0F, 2.5F, true, true, true));
    }

    @Test
    void hidesMaximumForNonPositiveOrInvalidMax() {
        assertEquals("10", HealthReadout.text(10.0F, 0.0F, 0.0F, true, true, false));
        assertEquals("10", HealthReadout.text(10.0F, Float.NaN, 0.0F, true, true, false));
    }

    @Test
    void reportsUnavailableForInvalidHealth() {
        assertEquals("--", HealthReadout.text(Float.NaN, 20.0F, 0.0F, true, true, false));
        assertEquals("--", HealthReadout.text(-1.0F, 20.0F, 0.0F, true, true, false));
    }

    @Test
    void fractionIsClampedToUnitRange() {
        assertEquals(0.0F, HealthReadout.fraction(0.0F, 20.0F));
        assertEquals(0.5F, HealthReadout.fraction(10.0F, 20.0F));
        assertEquals(1.0F, HealthReadout.fraction(20.0F, 20.0F));
        assertEquals(1.0F, HealthReadout.fraction(30.0F, 20.0F));
        assertEquals(0.0F, HealthReadout.fraction(10.0F, 0.0F));
        assertEquals(0.0F, HealthReadout.fraction(Float.NaN, 20.0F));
    }

    @Test
    void colorMovesFromRedThroughYellowToGreenAndIsOpaque() {
        int empty = HealthReadout.color(0.0F, 20.0F);
        int half = HealthReadout.color(10.0F, 20.0F);
        int full = HealthReadout.color(20.0F, 20.0F);

        assertEquals(0xFFFF5555, empty);
        assertEquals(0xFFFFEE55, half);
        assertEquals(0xFF55FF55, full);

        for (int color : new int[] {empty, half, full}) {
            assertEquals(0xFF, (color >>> 24) & 0xFF, "color must be fully opaque");
        }

        int quarter = HealthReadout.color(5.0F, 20.0F);
        int red = (quarter >> 16) & 0xFF;
        int green = (quarter >> 8) & 0xFF;
        assertTrue(red > green, "low health should lean red");
    }
}
