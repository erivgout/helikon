package dev.helikon.client.gui;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickGuiStateTest {
    private ModuleRegistry registry;
    private TestModule fullbright;
    private TestModule sprint;
    private TestModule timestamps;

    @BeforeEach
    void setUp() {
        registry = new ModuleRegistry();
        fullbright = new TestModule("fullbright_stub", "Fullbright (Stub)",
                "Placeholder that exercises settings.", ModuleCategory.RENDER);
        sprint = new TestModule("auto_sprint", "AutoSprint",
                "Keeps the player sprinting.", ModuleCategory.MOVEMENT);
        timestamps = new TestModule("chat_timestamps", "ChatTimestamps",
                "Adds a local timestamp to chat messages.", ModuleCategory.CHAT);
        registry.register(fullbright);
        registry.register(sprint);
        registry.register(timestamps);
    }

    @Test
    void initialCategoryComesFromFirstRegisteredModule() {
        assertEquals(ModuleCategory.RENDER, new ClickGuiState(registry).selectedCategory());
    }

    @Test
    void initialCategoryDefaultsToCombatForEmptyRegistry() {
        assertEquals(ModuleCategory.COMBAT, new ClickGuiState(new ModuleRegistry()).selectedCategory());
    }

    @Test
    void visibleModulesFilterBySelectedCategory() {
        ClickGuiState state = new ClickGuiState(registry);
        state.selectCategory(ModuleCategory.MOVEMENT);
        assertEquals(List.of(sprint), state.visibleModules());

        state.selectCategory(ModuleCategory.COMBAT);
        assertTrue(state.visibleModules().isEmpty());
    }

    @Test
    void searchMatchesNameCaseInsensitively() {
        ClickGuiState state = new ClickGuiState(registry);
        state.setSearchQuery("SPRINT");
        assertEquals(List.of(sprint), state.visibleModules());
    }

    @Test
    void searchMatchesModuleId() {
        ClickGuiState state = new ClickGuiState(registry);
        state.setSearchQuery("_stub");
        assertEquals(List.of(fullbright), state.visibleModules());
    }

    @Test
    void searchMatchesDescription() {
        ClickGuiState state = new ClickGuiState(registry);
        state.setSearchQuery("timestamp to chat");
        assertEquals(List.of(timestamps), state.visibleModules());
    }

    @Test
    void searchSpansAllCategories() {
        ClickGuiState state = new ClickGuiState(registry);
        state.selectCategory(ModuleCategory.RENDER);
        state.setSearchQuery("s");
        assertEquals(List.of(fullbright, sprint, timestamps), state.visibleModules());
    }

    @Test
    void surroundingWhitespaceInQueryIsIgnored() {
        ClickGuiState state = new ClickGuiState(registry);
        state.setSearchQuery("  autosprint  ");
        assertEquals(List.of(sprint), state.visibleModules());
    }

    @Test
    void blankQueryRestoresCategoryFiltering() {
        ClickGuiState state = new ClickGuiState(registry);
        state.selectCategory(ModuleCategory.CHAT);
        state.setSearchQuery("sprint");
        assertEquals(List.of(sprint), state.visibleModules());

        state.setSearchQuery("   ");
        assertFalse(state.isSearching());
        assertEquals(List.of(timestamps), state.visibleModules());
    }

    @Test
    void nullQueryIsTreatedAsEmpty() {
        ClickGuiState state = new ClickGuiState(registry);
        state.setSearchQuery(null);
        assertEquals("", state.searchQuery());
        assertFalse(state.isSearching());
    }

    @Test
    void unmatchedQueryYieldsNoModules() {
        ClickGuiState state = new ClickGuiState(registry);
        state.setSearchQuery("does not exist");
        assertTrue(state.visibleModules().isEmpty());
    }

    @Test
    void moduleSelectionCanBeSetAndCleared() {
        ClickGuiState state = new ClickGuiState(registry);
        assertTrue(state.selectedModule().isEmpty());

        state.selectModule(sprint);
        assertEquals(sprint, state.selectedModule().orElseThrow());

        state.selectModule(null);
        assertTrue(state.selectedModule().isEmpty());
    }

    @Test
    void adjacentCategorySelectionWrapsAndClearsModuleSelection() {
        ClickGuiState state = new ClickGuiState(registry);
        state.selectCategory(ModuleCategory.COMBAT);
        state.selectModule(sprint);

        state.selectAdjacentCategory(-1);

        assertEquals(ModuleCategory.MISCELLANEOUS, state.selectedCategory());
        assertTrue(state.selectedModule().isEmpty());

        state.selectAdjacentCategory(1);
        assertEquals(ModuleCategory.COMBAT, state.selectedCategory());
    }

    @Test
    void adjacentModuleSelectionUsesVisibleRowsAndWraps() {
        ClickGuiState state = new ClickGuiState(registry);
        state.setSearchQuery("s");

        assertEquals(fullbright, state.selectAdjacentModule(1).orElseThrow());
        assertEquals(sprint, state.selectAdjacentModule(1).orElseThrow());
        assertEquals(fullbright, state.selectAdjacentModule(-1).orElseThrow());
        assertEquals(timestamps, state.selectAdjacentModule(-1).orElseThrow());
        assertEquals(fullbright, state.selectAdjacentModule(1).orElseThrow());
    }

    @Test
    void adjacentModuleSelectionClearsAnInvisibleSelectionForEmptyResults() {
        ClickGuiState state = new ClickGuiState(registry);
        state.selectModule(sprint);
        state.setSearchQuery("not found");

        assertTrue(state.selectAdjacentModule(1).isEmpty());
        assertTrue(state.selectedModule().isEmpty());
    }

    private static final class TestModule extends Module {
        private TestModule(String id, String name, String description, ModuleCategory category) {
            super(id, name, description, category, false, Keybind.unbound());
        }
    }
}
