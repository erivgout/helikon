package dev.helikon.client.command;

import dev.helikon.client.module.chat.PrivateMessageHelper;
import dev.helikon.client.privatechat.PrivateConversation;
import dev.helikon.client.privatechat.PrivateMessageHistory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Local .pm/.reply commands that build only validated ordinary server commands. */
public final class PrivateMessageCommand implements HelikonCommand {
    /** Conservative bound for one ordinary server-command payload. */
    static final int MAXIMUM_COMMAND_LENGTH = 256;

    private final boolean reply;
    private final PrivateMessageHelper helper;
    private final PrivateMessageHistory history;
    private final ServerCommandSender sender;

    public PrivateMessageCommand(boolean reply, PrivateMessageHelper helper, PrivateMessageHistory history,
                                 ServerCommandSender sender) {
        this.reply = reply;
        this.helper = Objects.requireNonNull(helper, "helper");
        this.history = Objects.requireNonNull(history, "history");
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    @Override
    public String name() {
        return reply ? "reply" : "pm";
    }

    @Override
    public String usage() {
        return reply ? ".reply <message>|history [player]" : ".pm <player> <message>|history [player]";
    }

    @Override
    public String description() {
        return reply ? "Sends a configured normal private-message reply." : "Sends a configured normal private message.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (!helper.isEnabled()) {
            feedback.error("PrivateMessageHelper is disabled.");
            return;
        }
        boolean escapedLiteral = !arguments.isEmpty() && arguments.getFirst().equals("--");
        List<String> effectiveArguments = escapedLiteral ? arguments.subList(1, arguments.size()) : arguments;
        if (!escapedLiteral && !effectiveArguments.isEmpty() && effectiveArguments.getFirst().equalsIgnoreCase("history")) {
            showHistory(effectiveArguments, feedback);
            return;
        }
        if (reply) {
            sendReply(effectiveArguments, feedback);
        } else {
            sendMessage(effectiveArguments, feedback);
        }
    }

    private void sendMessage(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() < 2) {
            feedback.error("Usage: " + usage());
            return;
        }
        String participant;
        String message = String.join(" ", arguments.subList(1, arguments.size()));
        try {
            participant = PrivateConversation.normalizeParticipant(arguments.getFirst());
            validateMessage(message);
        } catch (IllegalArgumentException exception) {
            feedback.error(exception.getMessage());
            return;
        }
        Optional<String> command = helper.messageCommand();
        if (command.isEmpty()) {
            feedback.error("PrivateMessageHelper's message command must be one safe command token.");
            return;
        }
        String serverCommand = command.get() + " " + participant + " " + message;
        if (!isSafeCommandLength(serverCommand, feedback)) {
            return;
        }
        sender.sendCommand(serverCommand);
        history.recordOutgoing(participant, message, helper.recentLimit());
        feedback.info("Sent local PM to '" + participant + "'.");
    }

    private void sendReply(List<String> arguments, CommandFeedback feedback) {
        if (arguments.isEmpty()) {
            feedback.error("Usage: " + usage());
            return;
        }
        String message = String.join(" ", arguments);
        try {
            validateMessage(message);
        } catch (IllegalArgumentException exception) {
            feedback.error(exception.getMessage());
            return;
        }
        Optional<String> command = helper.replyCommand();
        if (command.isEmpty()) {
            feedback.error("PrivateMessageHelper's reply command must be one safe command token.");
            return;
        }
        String serverCommand = command.get() + " " + message;
        if (!isSafeCommandLength(serverCommand, feedback)) {
            return;
        }
        sender.sendCommand(serverCommand);
        feedback.info("Sent local PM reply.");
    }

    private void showHistory(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() > 2) {
            feedback.error("Usage: " + usage());
            return;
        }
        if (arguments.size() == 1) {
            List<PrivateConversation> tabs = history.tabs();
            feedback.info(tabs.isEmpty() ? "No local PM conversations." : "PM tabs: "
                    + tabs.stream().map(PrivateConversation::participant).collect(java.util.stream.Collectors.joining(", ")));
            return;
        }
        try {
            List<PrivateConversation.Entry> entries = history.entries(arguments.get(1));
            if (entries.isEmpty()) {
                feedback.info("No local PM history for '" + arguments.get(1) + "'.");
                return;
            }
            for (PrivateConversation.Entry entry : entries) {
                feedback.info((entry.direction() == PrivateConversation.Direction.OUTGOING ? "You: " : "Them: ") + entry.text());
            }
        } catch (IllegalArgumentException exception) {
            feedback.error(exception.getMessage());
        }
    }

    private static void validateMessage(String message) {
        String checked = message.trim();
        if (checked.isEmpty() || checked.length() > 256 || checked.indexOf('\n') >= 0 || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Private message must be 1 through 256 single-line characters.");
        }
    }

    private static boolean isSafeCommandLength(String command, CommandFeedback feedback) {
        if (command.length() > MAXIMUM_COMMAND_LENGTH) {
            feedback.error("Private-message command is too long for Helikon's safe local limit.");
            return false;
        }
        return true;
    }
}
