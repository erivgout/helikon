package dev.helikon.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudElementPlacementTest {
    @Test
    void defaultsPreserveBottomLeftAndTopLeftAnchors() {
        HudElementPlacement saturation = new HudElementPlacement(HudElementId.SATURATION);
        HudElementPlacement waypoints = new HudElementPlacement(HudElementId.WAYPOINTS);

        assertEquals(new HudBounds(5, 215, 40, 20), saturation.bounds(100, 240, 40, 20));
        assertEquals(new HudBounds(5, 50, 40, 20), waypoints.bounds(100, 240, 40, 20));
    }

    @Test
    void centeredCrosshairPlacementResolvesToTheViewportCenter() {
        HudElementPlacement crosshair = new HudElementPlacement(HudElementId.BETTER_CROSSHAIR);

        assertEquals(new HudBounds(41, 31, 17, 17), crosshair.bounds(100, 80, 17, 17));
    }

    @Test
    void resolvedBoundsKeepOversizedOffsetsOnScreen() {
        HudElementPlacement placement = new HudElementPlacement(HudElementId.RADAR);

        assertTrue(placement.setAbsolutePosition(95, 75));
        assertEquals(new HudBounds(24, 4, 76, 76), placement.bounds(100, 80, 76, 76));
    }

    @Test
    void draggingConvertsPlacementToClampedTopLeftCoordinates() {
        HudElementPlacement placement = new HudElementPlacement(HudElementId.SATURATION);

        assertTrue(placement.setAbsolutePosition(30, 40));
        assertEquals(HudElementId.Anchor.TOP_LEFT, placement.anchor());
        assertEquals(new HudBounds(30, 40, 20, 10), placement.bounds(100, 80, 20, 10));
        assertFalse(placement.setAbsolutePosition(-1, 40));
    }

    @Test
    void validatesAndScalesEveryPersistedPresentationControl() {
        HudElementPlacement placement = new HudElementPlacement(HudElementId.SATURATION);

        assertTrue(placement.setScale(1.5F));
        assertFalse(placement.setScale(2.1F));
        placement.setAlignment(HudElementPlacement.Alignment.RIGHT);
        placement.setBackground(false);
        assertTrue(placement.setPadding(8));
        assertFalse(placement.setPadding(13));
        placement.setTextShadow(false);
        placement.setColor(0xFF80D8FF);
        placement.setRainbow(true);

        assertEquals(new HudBounds(5, 205, 60, 30), placement.scaledBounds(100, 240, 40, 20));
        assertEquals(HudElementPlacement.Alignment.RIGHT, placement.alignment());
        assertFalse(placement.background());
        assertEquals(8, placement.padding());
        assertFalse(placement.textShadow());
        assertEquals(0xFF80D8FF, placement.color());
        assertTrue(placement.rainbow());
    }
}
