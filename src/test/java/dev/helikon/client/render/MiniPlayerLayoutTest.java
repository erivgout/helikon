package dev.helikon.client.render;

import org.junit.jupiter.api.Test;
import dev.helikon.client.hud.HudElementId;
import dev.helikon.client.hud.HudElementPlacement;

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

    @Test
    void resolvesMovedAbsoluteContentBoundsForPipRendering() {
        HudElementPlacement placement = new HudElementPlacement(HudElementId.MINI_PLAYER);
        placement.setAbsolutePosition(120, 90);
        placement.setPadding(4);
        placement.setScale(1.5F);

        assertEquals(new dev.helikon.client.hud.HudBounds(126, 96, 105, 120),
                MiniPlayerLayout.contentBounds(placement, 400, 300));
    }
}
