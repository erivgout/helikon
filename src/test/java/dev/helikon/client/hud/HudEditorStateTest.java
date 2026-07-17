package dev.helikon.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HudEditorStateTest {
    @Test
    void dragPreservesPointerOffsetAndClampsElementToViewport() {
        HudLayout layout = new HudLayout();
        layout.setActiveModulesPosition(10, 12);
        HudEditorState state = new HudEditorState(layout);
        HudBounds bounds = new HudBounds(10, 12, 30, 20);

        assertTrue(state.beginDrag(15, 18, bounds));
        assertTrue(state.dragTo(200, 100, 100, 80, bounds));
        assertEquals(70, layout.activeModulesX());
        assertEquals(60, layout.activeModulesY());

        state.endDrag();
        assertFalse(state.isDragging());
        assertFalse(state.dragTo(20, 20, 100, 80, bounds));
    }

    @Test
    void disabledElementCannotBeDragged() {
        HudLayout layout = new HudLayout();
        layout.setActiveModulesEnabled(false);
        HudEditorState state = new HudEditorState(layout);

        assertFalse(state.beginDrag(5, 5, new HudBounds(4, 4, 30, 20)));
        assertFalse(state.isDragging());
    }

    @Test
    void persistedPositionIsClampedAfterViewportResize() {
        HudLayout layout = new HudLayout();
        layout.setActiveModulesPosition(400, 300);
        HudEditorState state = new HudEditorState(layout);

        state.clampToViewport(100, 80, new HudBounds(400, 300, 30, 20));

        assertEquals(70, layout.activeModulesX());
        assertEquals(60, layout.activeModulesY());
    }

    @Test
    void dragSnapsToEdgesAndCenter() {
        HudLayout layout = new HudLayout();
        HudEditorState state = new HudEditorState(layout);
        HudBounds bounds = new HudBounds(4, 4, 20, 20);

        assertTrue(state.beginDrag(5, 5, bounds));
        state.dragTo(6, 6, 100, 80, bounds);
        assertEquals(0, layout.activeModulesX());
        assertEquals(0, layout.activeModulesY());

        state.dragTo(41, 31, 100, 80, new HudBounds(0, 0, 20, 20));
        assertEquals(40, layout.activeModulesX());
        assertEquals(30, layout.activeModulesY());
    }
}
