package dev.helikon.client.hud;

/**
 * Minecraft-free persisted layout for the first Helikon HUD element. More
 * elements can be added here without coupling layout rules to rendering APIs.
 */
public final class HudLayout {
    public static final int DEFAULT_ACTIVE_MODULES_X = 4;
    public static final int DEFAULT_ACTIVE_MODULES_Y = 4;
    public static final int MAX_COORDINATE = 10_000;

    private boolean activeModulesEnabled = true;
    private int activeModulesX = DEFAULT_ACTIVE_MODULES_X;
    private int activeModulesY = DEFAULT_ACTIVE_MODULES_Y;

    public boolean activeModulesEnabled() {
        return activeModulesEnabled;
    }

    public void setActiveModulesEnabled(boolean enabled) {
        activeModulesEnabled = enabled;
    }

    public int activeModulesX() {
        return activeModulesX;
    }

    public int activeModulesY() {
        return activeModulesY;
    }

    /**
     * Sets the element's top-left position when both values are safe persisted
     * coordinates. Invalid positions leave the existing layout unchanged.
     */
    public boolean setActiveModulesPosition(int x, int y) {
        if (!isValidCoordinate(x) || !isValidCoordinate(y)) {
            return false;
        }
        activeModulesX = x;
        activeModulesY = y;
        return true;
    }

    /** Restores the active-modules element to its safe initial layout. */
    public void resetActiveModules() {
        activeModulesEnabled = true;
        activeModulesX = DEFAULT_ACTIVE_MODULES_X;
        activeModulesY = DEFAULT_ACTIVE_MODULES_Y;
    }

    public static boolean isValidCoordinate(int coordinate) {
        return coordinate >= 0 && coordinate <= MAX_COORDINATE;
    }
}
