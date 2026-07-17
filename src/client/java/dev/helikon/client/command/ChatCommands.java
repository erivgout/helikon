package dev.helikon.client.command;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.gui.components.EditBox;

import java.util.Objects;

/**
 * Intercepts outgoing chat messages that start with the command prefix and
 * routes them to the dispatcher. Intercepted messages are cancelled so no
 * command attempt ever reaches the server.
 */
public final class ChatCommands {
    private static volatile CommandDispatcher dispatcher;

    private ChatCommands() {
    }

    public static void register(CommandDispatcher dispatcher, CommandFeedback feedback) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        Objects.requireNonNull(feedback, "feedback");
        ChatCommands.dispatcher = dispatcher;
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !dispatcher.dispatch(message, feedback));
    }

    /** Applies a local first-token completion and returns whether Tab was consumed. */
    public static boolean complete(EditBox input) {
        CommandDispatcher current = dispatcher;
        if (current == null) {
            return false;
        }
        CommandCompletion.Result completion = CommandCompletion.complete(
                input.getValue(), input.getCursorPosition(), current.commands());
        if (!completion.changed()) {
            return false;
        }
        input.setValue(completion.value());
        input.setCursorPosition(completion.cursor());
        return true;
    }
}
