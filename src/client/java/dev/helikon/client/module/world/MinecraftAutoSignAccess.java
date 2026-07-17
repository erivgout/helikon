package dev.helikon.client.module.world;

import dev.helikon.client.mixin.AbstractSignEditScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;

/** Minecraft-only adapter for the verified 26.2 sign editor. */
public final class MinecraftAutoSignAccess {
    private AbstractSignEditScreen handled;

    public void tick(AutoSign module) {
        Minecraft client = Minecraft.getInstance();
        if (!module.isEnabled() || !(client.gui.screen() instanceof AbstractSignEditScreen screen)) {
            handled = null;
            return;
        }
        if (screen == handled) {
            return;
        }
        handled = screen;
        String[] messages = ((AbstractSignEditScreenAccessor) screen).helikon$messages();
        for (int index = 0; index < messages.length && index < 4; index++) {
            messages[index] = module.fourLines().get(index);
        }
        ((AbstractSignEditScreenAccessor) screen).helikon$done();
    }
}
