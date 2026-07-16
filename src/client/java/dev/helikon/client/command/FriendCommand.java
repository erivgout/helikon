package dev.helikon.client.command;

import dev.helikon.client.friend.Friend;
import dev.helikon.client.friend.FriendManager;

import java.util.List;
import java.util.Objects;

/** Local friend-list commands; no name, UUID, or color is transmitted. */
public final class FriendCommand implements HelikonCommand {
    private final FriendManager friends;

    public FriendCommand(FriendManager friends) { this.friends = Objects.requireNonNull(friends, "friends"); }
    @Override public String name() { return "friend"; }
    @Override public String usage() { return ".friend list|add <player>|remove <player>|color <player> <#RRGGBB|#AARRGGBB>"; }
    @Override public String description() { return "Manages local player-name friends and their render colors."; }

    @Override public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.isEmpty()) { feedback.error("Usage: " + usage()); return; }
        try {
            switch (arguments.get(0)) {
                case "list" -> list(arguments, feedback); case "add" -> add(arguments, feedback);
                case "remove" -> remove(arguments, feedback); case "color" -> color(arguments, feedback);
                default -> feedback.error("Usage: " + usage());
            }
        } catch (RuntimeException exception) { feedback.error("Friend action failed: " + exception.getMessage()); }
    }
    private void list(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) { feedback.error("Usage: .friend list"); return; }
        List<Friend> listed = friends.list();
        feedback.info(listed.isEmpty() ? "No local friends." : "Friends: " + listed.stream().map(Friend::name).reduce((a, b) -> a + ", " + b).orElseThrow());
    }
    private void add(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) { feedback.error("Usage: .friend add <player>"); return; }
        if (!friends.add(arguments.get(1))) { feedback.error("'" + arguments.get(1) + "' is already a friend."); return; }
        friends.save(); feedback.info("Added local friend '" + arguments.get(1) + "'.");
    }
    private void remove(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) { feedback.error("Usage: .friend remove <player>"); return; }
        if (!friends.remove(arguments.get(1))) { feedback.error("'" + arguments.get(1) + "' is not a friend."); return; }
        friends.save(); feedback.info("Removed local friend '" + arguments.get(1) + "'.");
    }
    private void color(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 3) { feedback.error("Usage: .friend color <player> <#RRGGBB|#AARRGGBB>"); return; }
        if (!friends.setColor(arguments.get(1), parseColor(arguments.get(2)))) { feedback.error("'" + arguments.get(1) + "' is not a friend."); return; }
        friends.save(); feedback.info("Updated local friend color for '" + arguments.get(1) + "'.");
    }
    private static int parseColor(String text) {
        String hex = text == null ? "" : text.trim(); if (hex.startsWith("#")) hex = hex.substring(1);
        if (!hex.matches("[0-9a-fA-F]{6}|[0-9a-fA-F]{8}")) throw new IllegalArgumentException("Color must be #RRGGBB or #AARRGGBB");
        long value = Long.parseLong(hex, 16); return hex.length() == 6 ? (int) (0xFF000000L | value) : (int) value;
    }
}
