package dev.helikon.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickGuiWindowStateTest {
    @Test
    void favoritesAreBoundedValidatedAndResettable() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        state.setFavorite("enderman_aura", true);
        state.setFavorite("zoom", true);
        assertTrue(state.isFavorite("enderman_aura"));
        assertEquals(java.util.Set.of("enderman_aura", "zoom"), state.favoriteModuleIds());
        state.setFavorite("zoom", false);
        assertFalse(state.isFavorite("zoom"));
        assertThrows(IllegalArgumentException.class, () -> state.setFavorite("Bad ID", true));
        state.reset();
        assertTrue(state.favoriteModuleIds().isEmpty());
    }
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

    @Test
    void appliesValidatedInterfaceScaleAndReducedAnimationPreference() {
        ClickGuiWindowState state = new ClickGuiWindowState();

        assertTrue(state.setInterfaceScale(1.25F));
        assertFalse(state.setInterfaceScale(2.0F));
        state.setReducedAnimations(true);

        assertEquals(new ClickGuiWindowState.Size(450, 275), state.resolveSize(800, 600));
        assertTrue(state.reducedAnimations());
        state.reset();
        assertEquals(1.0F, state.interfaceScale());
        assertFalse(state.reducedAnimations());
    }
}
