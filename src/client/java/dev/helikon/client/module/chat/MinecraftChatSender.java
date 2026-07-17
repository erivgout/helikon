package dev.helikon.client.module.chat;

import net.minecraft.client.Minecraft;

/** Sends through Minecraft's existing client chat connection, without packet construction. */
public final class MinecraftChatSender implements ChatSpammer.ChatSender {
    @Override
    public void send(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            throw new IllegalStateException("No local player is available for normal chat");
        }
        client.player.connection.sendChat(message);
    }
}
