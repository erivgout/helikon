package dev.helikon.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.gui.HelikonClickGuiScreen;
import dev.helikon.client.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Registers non-module key mappings used by the Helikon shell. */
public final class HelikonKeybinds {
    private static KeyMapping openGui;

    private HelikonKeybinds() {
    }

    public static void register(ModuleRegistry modules, ConfigurationManager configuration) {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(HelikonClient.MOD_ID, "general")
        );
        openGui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.helikon.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.consumeClick()) {
                // Never replace an open screen: keybinds must not fire while
                // the user might be typing into another GUI's text field.
                if (client.gui.screen() == null) {
                    client.setScreenAndShow(new HelikonClickGuiScreen(modules, configuration));
                }
            }
        });
    }
}
