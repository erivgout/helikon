package dev.helikon.client.command;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

import java.util.Objects;

/**
 * Intercepts outgoing chat messages that start with the command prefix and
 * routes them to the dispatcher. Intercepted messages are cancelled so no
 * command attempt ever reaches the server.
 */
public final class ChatCommands {
    private ChatCommands() {
    }

    public static void register(CommandDispatcher dispatcher, CommandFeedback feedback) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        Objects.requireNonNull(feedback, "feedback");
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !dispatcher.dispatch(message, feedback));
    }
}
