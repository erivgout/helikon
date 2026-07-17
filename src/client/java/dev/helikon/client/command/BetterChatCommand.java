package dev.helikon.client.command;

import dev.helikon.client.chat.ChatHistorySearch;
import dev.helikon.client.module.chat.BetterChat;

import java.util.List;
import java.util.Objects;

/** Local search, copy, and bounded history views for the enabled BetterChat module. */
public final class BetterChatCommand implements HelikonCommand {
    private static final int MAXIMUM_RESULT_LINES = 8;
    private static final int MAXIMUM_HISTORY_LINES = 20;

    private final BetterChat betterChat;
    private final ChatHistoryProvider history;
    private final TextClipboard clipboard;

    public BetterChatCommand(BetterChat betterChat, ChatHistoryProvider history, TextClipboard clipboard) {
        this.betterChat = Objects.requireNonNull(betterChat, "betterChat");
        this.history = Objects.requireNonNull(history, "history");
        this.clipboard = Objects.requireNonNull(clipboard, "clipboard");
    }

    @Override
    public String name() {
        return "chat";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "chat <search <text>|copy <newest-index>|history [count]>";
    }

    @Override
    public String description() {
        return "Searches, copies, or lists local BetterChat history.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (!betterChat.isEnabled()) {
            feedback.error("BetterChat is disabled.");
            return;
        }
        if (arguments.isEmpty()) {
            feedback.error("Usage: " + usage());
            return;
        }
        switch (arguments.getFirst().toLowerCase(java.util.Locale.ROOT)) {
            case "search" -> search(arguments.subList(1, arguments.size()), feedback);
            case "copy" -> copy(arguments.subList(1, arguments.size()), feedback);
            case "history" -> showHistory(arguments.subList(1, arguments.size()), feedback);
            default -> feedback.error("Usage: " + usage());
        }
    }

    private void search(List<String> arguments, CommandFeedback feedback) {
        if (arguments.isEmpty()) {
            feedback.error("Usage: " + usage());
            return;
        }
        try {
            String query = String.join(" ", arguments);
            List<String> matches = ChatHistorySearch.find(history.messages(), query, MAXIMUM_RESULT_LINES);
            if (matches.isEmpty()) {
                feedback.info("No local chat lines match '" + query.trim() + "'.");
                return;
            }
            feedback.info(matches.size() + " local chat match(es):");
            for (String match : matches) {
                feedback.info(match);
            }
        } catch (IllegalArgumentException exception) {
            feedback.error(exception.getMessage());
        }
    }

    private void copy(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: " + usage());
            return;
        }
        List<String> messages = history.messages();
        try {
            int index = Integer.parseInt(arguments.getFirst());
            if (index < 1 || index > messages.size()) {
                throw new IllegalArgumentException("Chat copy index must identify one retained local line.");
            }
            clipboard.copy(messages.get(index - 1));
            feedback.info("Copied local chat line #" + index + " to the clipboard.");
        } catch (NumberFormatException exception) {
            feedback.error("Chat copy index must be a positive number.");
        } catch (IllegalArgumentException exception) {
            feedback.error(exception.getMessage());
        }
    }

    private void showHistory(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() > 1) {
            feedback.error("Usage: " + usage());
            return;
        }
        int count = MAXIMUM_HISTORY_LINES;
        if (!arguments.isEmpty()) {
            try {
                count = Math.clamp(Integer.parseInt(arguments.getFirst()), 1, MAXIMUM_HISTORY_LINES);
            } catch (NumberFormatException exception) {
                feedback.error("History count must be a positive number.");
                return;
            }
        }
        List<String> messages = history.messages();
        if (messages.isEmpty()) {
            feedback.info("No local retained chat lines.");
            return;
        }
        int displayed = Math.min(count, messages.size());
        feedback.info("Newest " + displayed + " local chat line(s):");
        for (int index = 0; index < displayed; index++) {
            feedback.info("#" + (index + 1) + " " + messages.get(index));
        }
    }
}
