package dev.helikon.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Distinguishes ordinary in-game overlays from screens that genuinely suspend
 * autonomous utility work. Containers and Helikon screens leave the player in
 * a live world, so background modules may continue ticking there.
 */
public final class GameplayScreenPolicy {
    private GameplayScreenPolicy() {
    }

    public static boolean allowsAutomation(Screen screen) {
        return screen == null
                || screen instanceof AbstractContainerScreen<?>
                || screen.getClass().getPackageName().equals(HelikonClickGuiScreen.class.getPackageName());
    }

    public static boolean blocksAutomation(Screen screen) {
        return !allowsAutomation(screen);
    }
}
