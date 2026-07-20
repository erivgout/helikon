package dev.helikon.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HudEditorGridTest {
    @Test
    void snapsToNearestEightPixelCellInsideBounds() {
        assertEquals(0, HudEditorGrid.snap(3, 0, 100));
        assertEquals(8, HudEditorGrid.snap(5, 0, 100));
        assertEquals(24, HudEditorGrid.snap(27, 0, 100));
        assertEquals(100, HudEditorGrid.snap(99, 0, 100));
    }

    @Test
    void supportsReservedToolbarOrigin() {
        assertEquals(22, HudEditorGrid.snap(4, 22, 100));
        assertEquals(30, HudEditorGrid.snap(31, 22, 100));
        assertThrows(IllegalArgumentException.class, () -> HudEditorGrid.snap(0, 10, 5));
    }
}
