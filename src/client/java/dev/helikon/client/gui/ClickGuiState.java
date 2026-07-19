package dev.helikon.client.gui;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.ModuleSearch;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Screen-independent ClickGUI view state: the selected category, the search
 * query, and the selected module. This class intentionally has no Minecraft
 * dependencies so filtering and selection behavior stay unit-testable.
 */
public final class ClickGuiState {
    public enum ViewMode {
        CATEGORY,
        ACTIVE,
        FAVORITES,
        BARITONE
    }

    private final ModuleRegistry registry;

    private ModuleCategory selectedCategory;
    private String searchQuery = "";
    private Module selectedModule;
    private boolean showingActiveModules;
    private boolean showingFavorites;
    private boolean showingBaritone;
    private Set<String> favoriteModuleIds = Set.of();

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
        showingFavorites = false;
        showingBaritone = false;
    }

    /** Shows the currently enabled modules across every category. */
    public void selectActiveModules() {
        showingActiveModules = true;
        showingFavorites = false;
        showingBaritone = false;
        selectedModule = null;
    }

    public boolean isShowingActiveModules() {
        return showingActiveModules && !isSearching();
    }

    public void selectFavoriteModules(Set<String> favoriteModuleIds) {
        this.favoriteModuleIds = Set.copyOf(Objects.requireNonNull(favoriteModuleIds, "favoriteModuleIds"));
        showingFavorites = true;
        showingActiveModules = false;
        showingBaritone = false;
        selectedModule = null;
    }

    public boolean isShowingFavoriteModules() {
        return showingFavorites && !isSearching();
    }

    /** Opens the dedicated embedded-Baritone control section. */
    public void selectBaritone() {
        showingBaritone = true;
        showingFavorites = false;
        showingActiveModules = false;
        selectedModule = registry.find("baritone").orElse(null);
    }

    public boolean isShowingBaritone() {
        return showingBaritone && !isSearching();
    }

    public ViewMode viewMode() {
        if (showingActiveModules) {
            return ViewMode.ACTIVE;
        }
        if (showingFavorites) {
            return ViewMode.FAVORITES;
        }
        if (showingBaritone) {
            return ViewMode.BARITONE;
        }
        return ViewMode.CATEGORY;
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
        showingFavorites = false;
        showingBaritone = false;
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
        if (showingFavorites) {
            return registry.all().stream().filter(module -> favoriteModuleIds.contains(module.id())).toList();
        }
        if (showingBaritone) {
            return registry.find("baritone").stream().toList();
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

    /** Restores a previously persisted view, ignoring modules that no longer exist or are no longer visible. */
    public void restore(ViewMode viewMode, ModuleCategory category, String query, String selectedModuleId,
                        Set<String> favoriteModuleIds) {
        selectedCategory = Objects.requireNonNullElse(category, initialCategory(registry));
        searchQuery = query == null ? "" : query;
        this.favoriteModuleIds = Set.copyOf(Objects.requireNonNullElse(favoriteModuleIds, Set.of()));
        showingActiveModules = viewMode == ViewMode.ACTIVE;
        showingFavorites = viewMode == ViewMode.FAVORITES;
        showingBaritone = viewMode == ViewMode.BARITONE;
        selectedModule = registry.find(selectedModuleId).filter(visibleModules()::contains).orElse(null);
        if (showingBaritone && selectedModule == null) {
            selectedModule = registry.find("baritone").orElse(null);
        }
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
