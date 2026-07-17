package dev.helikon.client.command;

import dev.helikon.client.chat.ChatHistoryEntry;
import dev.helikon.client.chat.ChatHistoryManager;
import dev.helikon.client.chat.ChatHistorySearch;
import dev.helikon.client.module.chat.ChatHistory;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Local commands for bounded ChatHistory search, copying, listing, and draft reopening. */
public final class ChatHistoryCommand implements HelikonCommand {
    private static final int MAXIMUM_RESULT_LINES = 8;
    private static final int MAXIMUM_HISTORY_LINES = 20;

    private final ChatHistory module;
    private final ChatHistoryManager history;
    private final TextClipboard clipboard;
    private final ChatInputReopener reopener;

    public ChatHistoryCommand(ChatHistory module, ChatHistoryManager history, TextClipboard clipboard,
                              ChatInputReopener reopener) {
        this.module = Objects.requireNonNull(module, "module");
        this.history = Objects.requireNonNull(history, "history");
        this.clipboard = Objects.requireNonNull(clipboard, "clipboard");
        this.reopener = Objects.requireNonNull(reopener, "reopener");
    }

    @Override
    public String name() {
        return "history";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "history <search <text>|copy <newest-index>|player <newest-index>|"
                + "reopen <newest-index>|list [count]>";
    }

    @Override
    public String description() {
        return "Searches, copies, lists, or reopens local ChatHistory entries.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (!module.isEnabled()) {
            feedback.error("ChatHistory is disabled.");
            return;
        }
        if (arguments.isEmpty()) {
            feedback.error("Usage: " + usage());
            return;
        }
        switch (arguments.getFirst().toLowerCase(Locale.ROOT)) {
            case "search" -> search(arguments.subList(1, arguments.size()), feedback);
            case "copy" -> copy(arguments.subList(1, arguments.size()), feedback);
            case "player" -> copyPlayer(arguments.subList(1, arguments.size()), feedback);
            case "reopen" -> reopen(arguments.subList(1, arguments.size()), feedback);
            case "list" -> list(arguments.subList(1, arguments.size()), feedback);
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
            List<String> matches = ChatHistorySearch.find(history.entries().stream().map(ChatHistoryEntry::text).toList(),
                    query, MAXIMUM_RESULT_LINES);
            if (matches.isEmpty()) {
                feedback.info("No local ChatHistory entries match '" + query.trim() + "'.");
                return;
            }
            feedback.info(matches.size() + " local ChatHistory match(es):");
            matches.forEach(feedback::info);
        } catch (IllegalArgumentException exception) {
            feedback.error(exception.getMessage());
        }
    }

    private void copy(List<String> arguments, CommandFeedback feedback) {
        entry(arguments, feedback).ifPresent(entry -> {
            clipboard.copy(entry.text());
            feedback.info("Copied local chat entry to the clipboard.");
        });
    }

    private void copyPlayer(List<String> arguments, CommandFeedback feedback) {
        entry(arguments, feedback).ifPresent(entry -> {
            if (!entry.canCopyPlayerName()) {
                feedback.error("That local chat entry has no player name to copy.");
                return;
            }
            clipboard.copy(entry.sender());
            feedback.info("Copied local player name '" + entry.sender() + "' to the clipboard.");
        });
    }

    private void reopen(List<String> arguments, CommandFeedback feedback) {
        entry(arguments, feedback).ifPresent(entry -> {
            if (!entry.canReopen()) {
                feedback.error("Only a locally retained sent chat entry can be reopened.");
                return;
            }
            reopener.reopen(entry.text());
            feedback.info("Reopened local sent chat as an unsent draft.");
        });
    }

    private void list(List<String> arguments, CommandFeedback feedback) {
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
        List<ChatHistoryEntry> entries = history.entries();
        if (entries.isEmpty()) {
            feedback.info("No local ChatHistory entries for this server scope.");
            return;
        }
        int displayed = Math.min(count, entries.size());
        feedback.info("Newest " + displayed + " local ChatHistory entry(ies):");
        for (int index = 0; index < displayed; index++) {
            ChatHistoryEntry entry = entries.get(index);
            String sender = entry.sender().isEmpty() ? "" : entry.sender() + ": ";
            feedback.info("#" + (index + 1) + " " + sender + entry.text());
        }
    }

    private java.util.Optional<ChatHistoryEntry> entry(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: " + usage());
            return java.util.Optional.empty();
        }
        try {
            int index = Integer.parseInt(arguments.getFirst());
            List<ChatHistoryEntry> entries = history.entries();
            if (index < 1 || index > entries.size()) {
                throw new IllegalArgumentException("History index must identify one retained local entry.");
            }
            return java.util.Optional.of(entries.get(index - 1));
        } catch (NumberFormatException exception) {
            feedback.error("History index must be a positive number.");
            return java.util.Optional.empty();
        } catch (IllegalArgumentException exception) {
            feedback.error(exception.getMessage());
            return java.util.Optional.empty();
        }
    }
}
