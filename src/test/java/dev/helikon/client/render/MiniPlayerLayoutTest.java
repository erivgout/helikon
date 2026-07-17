package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MiniPlayerLayoutTest {
    @Test
    void exposesTheFixedPanelAndBoundedEntityScale() {
        assertEquals(70, MiniPlayerLayout.bounds().width());
        assertEquals(80, MiniPlayerLayout.bounds().height());
        assertEquals(15, MiniPlayerLayout.entitySize(0.5D));
        assertEquals(60, MiniPlayerLayout.entitySize(2.0D));
    }

    @Test
    void rejectsUnsupportedScale() {
        assertThrows(IllegalArgumentException.class, () -> MiniPlayerLayout.entitySize(0.49D));
    }
}
