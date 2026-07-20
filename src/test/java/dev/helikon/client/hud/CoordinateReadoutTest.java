package dev.helikon.client.hud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoordinateReadoutTest {
    @Test
    void floorsBlockCoordinatesIncludingNegativeValues() {
        assertEquals(List.of("XYZ 12, 64, -4"),
                CoordinateReadout.lines(12.9D, 64.1D, -3.1D, "minecraft:overworld", false, false));
    }

    @Test
    void optionallyShowsDecimalsAndDimension() {
        assertEquals(List.of("XYZ 12.3, 64.5, -3.8", "Dimension The Nether"),
                CoordinateReadout.lines(12.25D, 64.5D, -3.75D, "minecraft:the_nether", true, true));
    }

    @Test
    void rejectsNonFinitePositions() {
        assertEquals(List.of("XYZ --"),
                CoordinateReadout.lines(Double.NaN, 64.0D, 0.0D, "minecraft:overworld", true, true));
    }
}
