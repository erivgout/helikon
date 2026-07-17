package dev.helikon.client.hud;

import java.util.EnumMap;
import java.util.Objects;

/**
 * Minecraft-free persisted layout for the first Helikon HUD element. More
 * elements can be added here without coupling layout rules to rendering APIs.
 */
public final class HudLayout {
    public static final int DEFAULT_ACTIVE_MODULES_X = 4;
    public static final int DEFAULT_ACTIVE_MODULES_Y = 4;
    public static final int MAX_COORDINATE = 10_000;

    private final ActiveModulesLayout activeModules = new ActiveModulesLayout();
    private final EnumMap<HudElementId, HudElementPlacement> elements = new EnumMap<>(HudElementId.class);

    public HudLayout() {
        for (HudElementId element : HudElementId.values()) {
            elements.put(element, new HudElementPlacement(element));
        }
    }

    public ActiveModulesLayout activeModules() {
        return activeModules;
    }

    /** Returns a stable mutable local placement for a registered non-Active-Modules element. */
    public HudElementPlacement element(HudElementId element) {
        return elements.get(Objects.requireNonNull(element, "element"));
    }

    public void resetElements() {
        for (HudElementId element : HudElementId.values()) {
            elements.get(element).reset(element);
        }
    }

    public boolean activeModulesEnabled() {
        return activeModules.enabled();
    }

    public void setActiveModulesEnabled(boolean enabled) {
        activeModules.setEnabled(enabled);
    }

    public int activeModulesX() {
        return activeModules.x();
    }

    public int activeModulesY() {
        return activeModules.y();
    }

    /**
     * Sets the element's top-left position when both values are safe persisted
     * coordinates. Invalid positions leave the existing layout unchanged.
     */
    public boolean setActiveModulesPosition(int x, int y) {
        if (!isValidCoordinate(x) || !isValidCoordinate(y)) {
            return false;
        }
        return activeModules.setPosition(x, y);
    }

    /** Restores the active-modules element to its safe initial layout. */
    public void resetActiveModules() {
        activeModules.reset();
    }

    public static boolean isValidCoordinate(int coordinate) {
        return coordinate >= 0 && coordinate <= MAX_COORDINATE;
    }
}
