package dev.helikon.client.gui;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;

import java.util.List;
import java.util.Locale;
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

    public ClickGuiState(ModuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.selectedCategory = initialCategory(registry);
    }

    public ModuleCategory selectedCategory() {
        return selectedCategory;
    }

    public void selectCategory(ModuleCategory category) {
        this.selectedCategory = Objects.requireNonNull(category, "category");
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
        String needle = searchQuery.trim().toLowerCase(Locale.ROOT);
        return registry.all().stream()
                .filter(module -> needle.isEmpty()
                        ? module.category() == selectedCategory
                        : matches(module, needle))
                .toList();
    }

    public Optional<Module> selectedModule() {
        return Optional.ofNullable(selectedModule);
    }

    /** Selects the module whose settings the GUI shows; {@code null} clears it. */
    public void selectModule(Module module) {
        this.selectedModule = module;
    }

    /** Case-insensitive match against module name, ID, and description. */
    static boolean matches(Module module, String lowerCaseNeedle) {
        return module.name().toLowerCase(Locale.ROOT).contains(lowerCaseNeedle)
                || module.id().contains(lowerCaseNeedle)
                || module.description().toLowerCase(Locale.ROOT).contains(lowerCaseNeedle);
    }

    private static ModuleCategory initialCategory(ModuleRegistry registry) {
        return registry.all().stream()
                .map(Module::category)
                .findFirst()
                .orElse(ModuleCategory.COMBAT);
    }
}
