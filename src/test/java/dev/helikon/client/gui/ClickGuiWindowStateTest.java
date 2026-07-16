package dev.helikon.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickGuiWindowStateTest {
    @Test
    void unsetWindowCentersWithinViewport() {
        ClickGuiWindowState state = new ClickGuiWindowState();

        assertEquals(new ClickGuiWindowState.Position(70, 40), state.resolve(500, 300, 360, 220));
        assertFalse(state.isPositioned());
    }

    @Test
    void savedWindowPositionClampsAfterViewportResize() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        assertTrue(state.setPosition(400, 300));

        assertEquals(new ClickGuiWindowState.Position(140, 80), state.resolve(500, 300, 360, 220));
        assertEquals(140, state.x());
        assertEquals(80, state.y());
    }

    @Test
    void dragPreservesPointerOffsetAndKeepsWindowVisible() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        state.setPosition(10, 20);
        ClickGuiWindowDragState drag = new ClickGuiWindowDragState(state);
        drag.beginDrag(16, 28, new ClickGuiWindowState.Position(10, 20));

        assertTrue(drag.dragTo(500, 500, 400, 300, 360, 220));
        assertEquals(new ClickGuiWindowState.Position(40, 80), state.resolve(400, 300, 360, 220));

        drag.endDrag();
        assertFalse(drag.isDragging());
        assertFalse(drag.dragTo(20, 20, 400, 300, 360, 220));
    }

    @Test
    void savedSizeClampsToViewportAndCanKeepCenteredPlacement() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        assertTrue(state.setSize(640, 480));

        assertEquals(new ClickGuiWindowState.Size(484, 284), state.resolveSize(500, 300));
        assertEquals(new ClickGuiWindowState.Position(8, 8), state.resolve(500, 300, 484, 284));
        assertFalse(state.isPositioned());
    }

    @Test
    void smallViewportRetainsAResizedSizeInsteadOfExpandingItAgain() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        assertTrue(state.setSize(100, 80));

        assertEquals(new ClickGuiWindowState.Size(100, 80), state.resolveSize(180, 120));
    }
}
