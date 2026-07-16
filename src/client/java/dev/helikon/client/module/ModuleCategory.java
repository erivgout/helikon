package dev.helikon.client.module;

/** The stable top-level categories used by the module registry and GUI. */
public enum ModuleCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    WORLD("World"),
    CHAT("Chat"),
    MISCELLANEOUS("Miscellaneous");

    private final String displayName;

    ModuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
