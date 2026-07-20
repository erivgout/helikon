package dev.helikon.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickGuiPanelPlacementTest {
    @Test
    void storesAndReplacesPanelPlacements() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        assertTrue(state.setPanelPlacement("render", 10, 20, false));
        assertTrue(state.setPanelPlacement("render", 30, 40, true));

        ClickGuiWindowState.PanelPlacement placement = state.panelPlacement("render").orElseThrow();
        assertEquals(30, placement.x());
        assertEquals(40, placement.y());
        assertTrue(placement.collapsed());
        assertEquals(1, state.panelPlacements().size());
    }

    @Test
    void rejectsUnsafePanelKeysAndCoordinates() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        assertFalse(state.setPanelPlacement("Render", 10, 20, false));
        assertFalse(state.setPanelPlacement("bad key", 10, 20, false));
        assertFalse(state.setPanelPlacement("render", -1, 20, false));
        assertFalse(state.setPanelPlacement("render", 10, ClickGuiWindowState.MAX_COORDINATE + 1, false));
        assertTrue(state.panelPlacements().isEmpty());
    }

    @Test
    void tracksExpandedModulesWithValidation() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        state.setModuleExpanded("auto_sprint", true);
        assertTrue(state.isModuleExpanded("auto_sprint"));
        state.setModuleExpanded("auto_sprint", false);
        assertFalse(state.isModuleExpanded("auto_sprint"));
        assertThrows(IllegalArgumentException.class, () -> state.setModuleExpanded("Not Valid!", true));
    }

    @Test
    void replaceExpandedModulesDropsInvalidIdsQuietly() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        state.replaceExpandedModules(java.util.List.of("auto_sprint", "Not Valid!", "x_ray"));
        assertTrue(state.isModuleExpanded("auto_sprint"));
        assertTrue(state.isModuleExpanded("x_ray"));
        assertEquals(2, state.expandedModuleIds().size());
    }

    @Test
    void resetClearsPanelLayoutState() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        state.setPanelPlacement("render", 10, 20, false);
        state.setModuleExpanded("auto_sprint", true);
        state.reset();
        assertTrue(state.panelPlacements().isEmpty());
        assertTrue(state.expandedModuleIds().isEmpty());
    }
}
