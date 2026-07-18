package dev.helikon.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickGuiScrollbarStateTest {
    @Test
    void draggingThumbMapsTrackEndsToScrollEnds() {
        ClickGuiScrollbarState state = new ClickGuiScrollbarState();

        assertEquals(0.0D, state.beginDrag(10, 10, 110, 400, 0.0D).orElseThrow(), 0.001D);
        assertEquals(300.0D, state.dragTo(110, 10, 110, 400).orElseThrow(), 0.001D);
        assertTrue(state.isDragging());

        state.endDrag();
        assertFalse(state.isDragging());
        assertTrue(state.dragTo(50, 10, 110, 400).isEmpty());
    }

    @Test
    void clickingTrackJumpsThumbTowardPointer() {
        ClickGuiScrollbarState state = new ClickGuiScrollbarState();

        double scroll = state.beginDrag(60, 10, 110, 400, 0.0D).orElseThrow();

        assertEquals(150.0D, scroll, 0.001D);
    }

    @Test
    void contentThatFitsHasNoDraggableThumb() {
        ClickGuiScrollbarState state = new ClickGuiScrollbarState();

        assertTrue(state.beginDrag(20, 10, 110, 100, 0.0D).isEmpty());
        assertTrue(ClickGuiScrollbarState.thumb(10, 110, 100, 0.0D).isEmpty());
    }
}
