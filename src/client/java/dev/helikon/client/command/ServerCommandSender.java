package dev.helikon.client.command;

/** Narrow port for Minecraft's ordinary server-command path. */
@FunctionalInterface
public interface ServerCommandSender {
    void sendCommand(String command);
}
