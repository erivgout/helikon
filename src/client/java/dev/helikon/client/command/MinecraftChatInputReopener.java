package dev.helikon.client.command;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

/** Minecraft-only adapter that opens a normal chat screen with a local draft; it never sends the draft. */
public final class MinecraftChatInputReopener implements ChatInputReopener {
    @Override
    public void reopen(String text) {
        Minecraft.getInstance().setScreenAndShow(new ChatScreen(text, false));
    }
}
