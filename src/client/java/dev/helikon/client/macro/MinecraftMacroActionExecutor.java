package dev.helikon.client.macro;

import dev.helikon.client.command.CommandDispatcher;
import dev.helikon.client.command.CommandFeedback;
import net.minecraft.client.Minecraft;

import java.util.Objects;

/** Thin, explicit 26.2 action bridge; it performs no scripting or packet shaping. */
public final class MinecraftMacroActionExecutor implements MacroActionExecutor {
    private final CommandDispatcher commands;
    private final CommandFeedback feedback;

    public MinecraftMacroActionExecutor(CommandDispatcher commands, CommandFeedback feedback) {
        this.commands = Objects.requireNonNull(commands, "commands");
        this.feedback = Objects.requireNonNull(feedback, "feedback");
    }

    @Override
    public void executeLocalCommand(String command) {
        if (!commands.dispatch(command, feedback)) {
            throw new IllegalArgumentException("Macro local action is not a Helikon command");
        }
    }

    @Override
    public void sendChat(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            throw new IllegalStateException("No player is available for macro chat");
        }
        client.player.connection.sendChat(message);
    }

    @Override
    public void sendMinecraftCommand(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            throw new IllegalStateException("No player is available for macro command");
        }
        client.player.connection.sendCommand(command);
    }
}
