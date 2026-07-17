package dev.helikon.client.gui;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.ModuleSearch;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Screen-independent ClickGUI view state: the selected category, the search
 * query, and the selected module. This class intentionally has no Minecraft
 * dependencies so filtering and selection behavior stay unit-testable.
 */
public final class ClickGuiState {
    private final ModuleRegistry registry;

    private ModuleCategory selectedCategory;
    private String searchQuery = "";
    private Module selectedModule;
    private boolean showingActiveModules;

    public ClickGuiState(ModuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.selectedCategory = initialCategory(registry);
    }

    public ModuleCategory selectedCategory() {
        return selectedCategory;
    }

    public void selectCategory(ModuleCategory category) {
        this.selectedCategory = Objects.requireNonNull(category, "category");
        showingActiveModules = false;
    }

    /** Shows the currently enabled modules across every category. */
    public void selectActiveModules() {
        showingActiveModules = true;
        selectedModule = null;
    }

    public boolean isShowingActiveModules() {
        return showingActiveModules && !isSearching();
    }

    /** Selects the next or previous category, wrapping at either end. */
    public void selectAdjacentCategory(int direction) {
        if (direction == 0) {
            return;
        }
        ModuleCategory[] categories = ModuleCategory.values();
        int next = Math.floorMod(selectedCategory.ordinal() + Integer.signum(direction), categories.length);
        selectedCategory = categories[next];
        selectedModule = null;
        showingActiveModules = false;
    }

    public String searchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query == null ? "" : query;
    }

    /** Whether the module list is currently driven by the search query. */
    public boolean isSearching() {
        return !searchQuery.isBlank();
    }

    /**
     * The modules the list should show: the selected category in registry
     * order, or every registered module matching the search query across all
     * categories while a query is present.
     */
    public List<Module> visibleModules() {
        if (isSearching()) {
            return ModuleSearch.filter(registry.all(), searchQuery);
        }
        if (showingActiveModules) {
            return registry.all().stream().filter(Module::isEnabled).toList();
        }
        return registry.all().stream()
                .filter(module -> module.category() == selectedCategory)
                .toList();
    }

    public Optional<Module> selectedModule() {
        return Optional.ofNullable(selectedModule);
    }

    /** Selects the module whose settings the GUI shows; {@code null} clears it. */
    public void selectModule(Module module) {
        this.selectedModule = module;
    }

    /**
     * Selects the next or previous visible module, wrapping at either end.
     * If nothing is selected yet, forward navigation starts at the first row
     * and backward navigation starts at the last row.
     */
    public Optional<Module> selectAdjacentModule(int direction) {
        List<Module> visible = visibleModules();
        if (visible.isEmpty()) {
            selectedModule = null;
            return Optional.empty();
        }
        if (direction == 0) {
            return Optional.ofNullable(selectedModule);
        }

        int current = visible.indexOf(selectedModule);
        int next = current < 0
                ? direction > 0 ? 0 : visible.size() - 1
                : Math.floorMod(current + Integer.signum(direction), visible.size());
        selectedModule = visible.get(next);
        return Optional.of(selectedModule);
    }

    private static ModuleCategory initialCategory(ModuleRegistry registry) {
        return registry.all().stream()
                .map(Module::category)
                .findFirst()
                .orElse(ModuleCategory.COMBAT);
    }
}
