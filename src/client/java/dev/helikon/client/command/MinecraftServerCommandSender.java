package dev.helikon.client.command;

import net.minecraft.client.Minecraft;

/** Sends an already validated command through the normal Minecraft connection. */
public final class MinecraftServerCommandSender implements ServerCommandSender {
    @Override
    public void sendCommand(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            throw new IllegalStateException("No player is available for a normal server command");
        }
        client.player.connection.sendCommand(command);
    }
}
