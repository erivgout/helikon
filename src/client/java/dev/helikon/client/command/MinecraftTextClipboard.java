package dev.helikon.client.command;

import net.minecraft.client.Minecraft;

/** Minecraft-only adapter for an explicit `.chat copy` request. */
public final class MinecraftTextClipboard implements TextClipboard {
    @Override
    public void copy(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }
}
