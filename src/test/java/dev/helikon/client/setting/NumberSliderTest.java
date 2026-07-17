package dev.helikon.client.setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberSliderTest {
    @Test
    void fractionMapsValueToClosedUnitInterval() {
        assertEquals(0.0D, NumberSlider.fraction(0.0D, 0.0D, 10.0D));
        assertEquals(0.5D, NumberSlider.fraction(5.0D, 0.0D, 10.0D));
        assertEquals(1.0D, NumberSlider.fraction(10.0D, 0.0D, 10.0D));
    }

    @Test
    void fractionClampsOutOfRangeValues() {
        assertEquals(0.0D, NumberSlider.fraction(-3.0D, 0.0D, 10.0D));
        assertEquals(1.0D, NumberSlider.fraction(42.0D, 0.0D, 10.0D));
    }

    @Test
    void fractionIsZeroForDegenerateRange() {
        assertEquals(0.0D, NumberSlider.fraction(5.0D, 5.0D, 5.0D));
    }

    @Test
    void handleXSpansTheTrackWidth() {
        assertEquals(100, NumberSlider.handleX(0.0D, 0.0D, 10.0D, 100, 50));
        assertEquals(149, NumberSlider.handleX(10.0D, 0.0D, 10.0D, 100, 50));
        assertEquals(125, NumberSlider.handleX(5.0D, 0.0D, 10.0D, 100, 51));
    }

    @Test
    void handleXRejectsTooNarrowTrack() {
        assertThrows(IllegalArgumentException.class, () -> NumberSlider.handleX(1.0D, 0.0D, 2.0D, 0, 0));
    }

    @Test
    void valueAtRoundsIntegralSliders() {
        assertEquals(0.0D, NumberSlider.valueAt(100, 100, 101, 0.0D, 10.0D, true));
        assertEquals(5.0D, NumberSlider.valueAt(150, 100, 101, 0.0D, 10.0D, true));
        assertEquals(10.0D, NumberSlider.valueAt(200, 100, 101, 0.0D, 10.0D, true));
        // A coordinate between two whole numbers rounds to the nearest one.
        assertEquals(3.0D, NumberSlider.valueAt(126, 100, 101, 0.0D, 10.0D, true));
    }

    @Test
    void valueAtClampsCoordinatesOutsideTheTrack() {
        assertEquals(0.0D, NumberSlider.valueAt(0, 100, 101, 0.0D, 10.0D, true));
        assertEquals(10.0D, NumberSlider.valueAt(500, 100, 101, 0.0D, 10.0D, true));
    }

    @Test
    void valueAtSnapsDecimalSlidersToACleanGrid() {
        double value = NumberSlider.valueAt(150, 100, 101, 0.0D, 1.0D, false);
        assertEquals(0.5D, value);
        // The snapped decimal renders without floating-point noise.
        assertEquals("0.5", NumberSettingText.format(value));

        double thirty = NumberSlider.valueAt(130, 100, 101, 0.0D, 1.0D, false);
        assertEquals("0.3", NumberSettingText.format(thirty));
    }

    @Test
    void valueAtRejectsTooNarrowTrack() {
        assertThrows(IllegalArgumentException.class,
                () -> NumberSlider.valueAt(10, 0, 1, 0.0D, 10.0D, false));
    }

    @Test
    void nudgeStepsIntegralSlidersByOne() {
        assertEquals(6.0D, NumberSlider.nudge(5.0D, 0.0D, 10.0D, true, 1));
        assertEquals(4.0D, NumberSlider.nudge(5.0D, 0.0D, 10.0D, true, -1));
    }

    @Test
    void nudgeClampsAtTheBounds() {
        assertEquals(10.0D, NumberSlider.nudge(10.0D, 0.0D, 10.0D, true, 1));
        assertEquals(0.0D, NumberSlider.nudge(0.0D, 0.0D, 10.0D, true, -1));
    }

    @Test
    void nudgeStepsDecimalSlidersByARangeRelativeAmount() {
        double raised = NumberSlider.nudge(0.5D, 0.0D, 1.0D, false, 1);
        assertEquals("0.51", NumberSettingText.format(raised));
        double lowered = NumberSlider.nudge(0.5D, 0.0D, 1.0D, false, -1);
        assertEquals("0.49", NumberSettingText.format(lowered));
    }

    @Test
    void nudgeWithoutDirectionOnlyNormalizesTheValue() {
        assertEquals(5.0D, NumberSlider.nudge(5.0D, 0.0D, 10.0D, true, 0));
        double snapped = NumberSlider.nudge(0.5D, 0.0D, 1.0D, false, 0);
        assertTrue(Double.isFinite(snapped));
        assertEquals(0.5D, snapped);
    }
}
