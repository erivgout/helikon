package dev.helikon.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.config.HudConfigurationManager;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.gui.HelikonClickGuiScreen;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Registers non-module key mappings used by the Helikon shell. */
public final class HelikonKeybinds {
    private static KeyMapping openGui;
    private static KeyMapping openMap;

    private HelikonKeybinds() {
    }

    public static void register(
            ModuleRegistry modules,
            ConfigurationManager configuration,
            ClickGuiWindowState clickGuiWindow,
            HudLayout hudLayout,
            HudConfigurationManager hudConfiguration,
            Runnable openMapAction
    ) {
        java.util.Objects.requireNonNull(openMapAction, "openMapAction");
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(HelikonClient.MOD_ID, "general")
        );
        openGui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.helikon.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));
        openMap = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.helikon.open_map",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.consumeClick()) {
                // Never replace an open screen: keybinds must not fire while
                // the user might be typing into another GUI's text field.
                if (client.gui.screen() == null) {
                    client.setScreenAndShow(HelikonClickGuiScreen.create(
                            modules, configuration, clickGuiWindow, hudLayout, hudConfiguration
                    ));
                }
            }
            while (openMap.consumeClick()) {
                if (client.gui.screen() == null) {
                    openMapAction.run();
                }
            }
        });
    }

    /**
     * Whether this keyboard or mouse input currently opens the Helikon GUI.
     * Such a bind can never activate a module (the GUI opens first and
     * suppresses module keybinds), so every bind assignment path rejects it.
     */
    public static boolean isGuiKey(Keybind keybind) {
        if (!keybind.isBound()) {
            return false;
        }
        if (keybind.isKeyboard()
                && (keybind.keyCode() == GLFW.GLFW_KEY_RIGHT_SHIFT || keybind.keyCode() == GLFW.GLFW_KEY_M)) {
            return true;
        }
        InputConstants.Type type = keybind.isKeyboard() ? InputConstants.Type.KEYSYM : InputConstants.Type.MOUSE;
        return openGui != null && openGui.matches(type.getOrCreate(keybind.keyCode()))
                || openMap != null && openMap.matches(type.getOrCreate(keybind.keyCode()));
    }
}
