package dev.helikon.client.gui;

import dev.helikon.client.module.ModuleCategory;

/**
 * Minecraft-free persisted ClickGUI placement. An unset position centers the
 * window; saved coordinates are always clamped to the current GUI viewport.
 */
public final class ClickGuiWindowState {
    public static final int MAX_COORDINATE = 10_000;
    public static final int DEFAULT_WIDTH = 360;
    public static final int DEFAULT_HEIGHT = 220;
    public static final int MIN_WIDTH = 280;
    public static final int MIN_HEIGHT = 180;
    public static final int MAX_DIMENSION = 10_000;
    public static final float MIN_INTERFACE_SCALE = 0.75F;
    public static final float MAX_INTERFACE_SCALE = 1.50F;

    private boolean positioned;
    private int x;
    private int y;
    private boolean sized;
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private ClickGuiTheme theme = ClickGuiTheme.MIDNIGHT;
    private float interfaceScale = 1.0F;
    private boolean reducedAnimations;
    private final java.util.LinkedHashSet<String> favoriteModuleIds = new java.util.LinkedHashSet<>();
    private boolean viewStateSaved;
    private ClickGuiState.ViewMode viewMode = ClickGuiState.ViewMode.CATEGORY;
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private String searchQuery = "";
    private String selectedModuleId = "";
    private double listScroll;
    private double settingsScroll;

    public boolean isPositioned() {
        return positioned;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public boolean isSized() {
        return sized;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public ClickGuiTheme theme() { return theme; }

    public float interfaceScale() { return interfaceScale; }

    public boolean reducedAnimations() { return reducedAnimations; }

    public boolean hasSavedViewState() { return viewStateSaved; }

    public ClickGuiState.ViewMode viewMode() { return viewMode; }

    public ModuleCategory selectedCategory() { return selectedCategory; }

    public String searchQuery() { return searchQuery; }

    public String selectedModuleId() { return selectedModuleId; }

    public double listScroll() { return listScroll; }

    public double settingsScroll() { return settingsScroll; }

    public java.util.Set<String> favoriteModuleIds() {
        return java.util.Set.copyOf(favoriteModuleIds);
    }

    public boolean isFavorite(String moduleId) {
        return favoriteModuleIds.contains(java.util.Objects.requireNonNull(moduleId, "moduleId"));
    }

    public void setFavorite(String moduleId, boolean favorite) {
        java.util.Objects.requireNonNull(moduleId, "moduleId");
        if (!moduleId.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Favorite module ID is invalid");
        }
        if (favorite) {
            if (favoriteModuleIds.size() < 512) {
                favoriteModuleIds.add(moduleId);
            }
        } else {
            favoriteModuleIds.remove(moduleId);
        }
    }

    public void replaceFavorites(java.util.Collection<String> moduleIds) {
        favoriteModuleIds.clear();
        if (moduleIds == null) {
            return;
        }
        for (String moduleId : moduleIds) {
            if (moduleId != null && moduleId.matches("[a-z][a-z0-9_]*")) {
                setFavorite(moduleId, true);
            }
        }
    }

    public void setTheme(ClickGuiTheme theme) { this.theme = java.util.Objects.requireNonNull(theme, "theme"); }

    public boolean setInterfaceScale(float value) {
        if (!Float.isFinite(value) || value < MIN_INTERFACE_SCALE || value > MAX_INTERFACE_SCALE) {
            return false;
        }
        interfaceScale = value;
        return true;
    }

    public void setReducedAnimations(boolean value) { reducedAnimations = value; }

    /** Stores the last navigated ClickGUI view so reopening resumes exactly where the user left it. */
    public boolean setViewState(ClickGuiState.ViewMode viewMode, ModuleCategory selectedCategory,
                                String searchQuery, String selectedModuleId,
                                double listScroll, double settingsScroll) {
        if (viewMode == null || selectedCategory == null || searchQuery == null || searchQuery.length() > 64
                || selectedModuleId == null || (!selectedModuleId.isEmpty()
                && !selectedModuleId.matches("[a-z][a-z0-9_]*"))
                || !validScroll(listScroll) || !validScroll(settingsScroll)) {
            return false;
        }
        this.viewMode = viewMode;
        this.selectedCategory = selectedCategory;
        this.searchQuery = searchQuery;
        this.selectedModuleId = selectedModuleId;
        this.listScroll = listScroll;
        this.settingsScroll = settingsScroll;
        viewStateSaved = true;
        return true;
    }

    /** Sets a user-selected top-left location if both persisted values are safe. */
    public boolean setPosition(int x, int y) {
        if (!isValidCoordinate(x) || !isValidCoordinate(y)) {
            return false;
        }
        this.x = x;
        this.y = y;
        positioned = true;
        return true;
    }

    /** Sets a user-selected size if it stays within the supported persisted range. */
    public boolean setSize(int width, int height) {
        if (!isValidWidth(width) || !isValidHeight(height)) {
            return false;
        }
        this.width = width;
        this.height = height;
        sized = true;
        return true;
    }

    /** Clears a saved position so the next GUI open centers the window. */
    public void reset() {
        resetPosition();
        resetSize();
        theme = ClickGuiTheme.MIDNIGHT;
        interfaceScale = 1.0F;
        reducedAnimations = false;
        favoriteModuleIds.clear();
        resetViewState();
    }

    /** Clears only the saved position while retaining the selected dimensions. */
    public void resetPosition() {
        positioned = false;
        x = 0;
        y = 0;
    }

    /** Clears only the saved size while retaining the selected position. */
    public void resetSize() {
        sized = false;
        width = DEFAULT_WIDTH;
        height = DEFAULT_HEIGHT;
    }

    public void resetViewState() {
        viewStateSaved = false;
        viewMode = ClickGuiState.ViewMode.CATEGORY;
        selectedCategory = ModuleCategory.COMBAT;
        searchQuery = "";
        selectedModuleId = "";
        listScroll = 0.0D;
        settingsScroll = 0.0D;
    }

    /** Resolves a centered or saved placement while keeping the whole window visible. */
    public Position resolve(int viewportWidth, int viewportHeight, int windowWidth, int windowHeight) {
        validateDimensions(viewportWidth, viewportHeight, windowWidth, windowHeight);
        int maximumX = Math.max(0, viewportWidth - windowWidth);
        int maximumY = Math.max(0, viewportHeight - windowHeight);
        if (!positioned) {
            return new Position(maximumX / 2, maximumY / 2);
        }

        int resolvedX = Math.clamp(x, 0, maximumX);
        int resolvedY = Math.clamp(y, 0, maximumY);
        x = resolvedX;
        y = resolvedY;
        return new Position(resolvedX, resolvedY);
    }

    /** Resolves a default or saved size within the usable viewport. */
    public Size resolveSize(int viewportWidth, int viewportHeight) {
        validateDimensions(viewportWidth, viewportHeight, 0, 0);
        int maximumWidth = Math.max(1, viewportWidth - 16);
        int maximumHeight = Math.max(1, viewportHeight - 16);
        int minimumWidth = maximumWidth < MIN_WIDTH ? 1 : MIN_WIDTH;
        int minimumHeight = maximumHeight < MIN_HEIGHT ? 1 : MIN_HEIGHT;
        int desiredWidth = Math.round((sized ? width : DEFAULT_WIDTH) * interfaceScale);
        int desiredHeight = Math.round((sized ? height : DEFAULT_HEIGHT) * interfaceScale);
        int resolvedWidth = Math.clamp(desiredWidth, minimumWidth, maximumWidth);
        int resolvedHeight = Math.clamp(desiredHeight, minimumHeight, maximumHeight);
        if (sized) {
            width = resolvedWidth;
            height = resolvedHeight;
        }
        return new Size(resolvedWidth, resolvedHeight);
    }

    public static boolean isValidCoordinate(int coordinate) {
        return coordinate >= 0 && coordinate <= MAX_COORDINATE;
    }

    public static boolean isValidWidth(int width) {
        return width >= 1 && width <= MAX_DIMENSION;
    }

    public static boolean isValidHeight(int height) {
        return height >= 1 && height <= MAX_DIMENSION;
    }

    private static boolean validScroll(double value) {
        return Double.isFinite(value) && value >= 0.0D && value <= MAX_DIMENSION;
    }

    private static void validateDimensions(int viewportWidth, int viewportHeight, int windowWidth, int windowHeight) {
        if (viewportWidth < 0 || viewportHeight < 0 || windowWidth < 0 || windowHeight < 0) {
            throw new IllegalArgumentException("GUI dimensions cannot be negative");
        }
    }

    /** Resolved window coordinates in scaled GUI units. */
    public record Position(int x, int y) {
    }

    /** Resolved window dimensions in scaled GUI units. */
    public record Size(int width, int height) {
    }
}
