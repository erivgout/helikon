package dev.helikon.client.hud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryPreviewLayoutTest {
    @Test
    void selectsStorageBeforeAnOptionalHotbarFromTheVerifiedThirtySixItemList() {
        assertEquals(List.of(9, 10, 11, 12, 13, 14, 15, 16, 17),
                InventoryPreviewLayout.slots(36, 1, false));
        assertEquals(List.of(9, 10, 11, 12, 13, 14, 15, 16, 17, 0, 1, 2, 3, 4, 5, 6, 7, 8),
                InventoryPreviewLayout.slots(36, 1, true));
        assertEquals(27, InventoryPreviewLayout.slots(36, 3, false).size());
    }

    @Test
    void safelyCapsIncompleteListsAndCalculatesGridGeometry() {
        assertEquals(List.of(9, 10, 11, 0, 1, 2, 3, 4, 5, 6, 7, 8),
                InventoryPreviewLayout.slots(12, 3, true));
        assertEquals(0, InventoryPreviewLayout.rowsFor(0));
        assertEquals(2, InventoryPreviewLayout.rowsFor(10));
        assertEquals(168, InventoryPreviewLayout.width());
        assertEquals(42, InventoryPreviewLayout.height(2));
    }

    @Test
    void rejectsInvalidPreviewDimensions() {
        assertThrows(IllegalArgumentException.class, () -> InventoryPreviewLayout.slots(36, 0, false));
        assertThrows(IllegalArgumentException.class, () -> InventoryPreviewLayout.slots(-1, 1, false));
        assertThrows(IllegalArgumentException.class, () -> InventoryPreviewLayout.rowsFor(-1));
    }
}
