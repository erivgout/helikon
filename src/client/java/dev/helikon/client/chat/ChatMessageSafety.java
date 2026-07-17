package dev.helikon.client.chat;

import java.util.Locale;

/** Conservative guards that keep local commands, private messages, and likely credentials untouched. */
public final class ChatMessageSafety {
    private ChatMessageSafety() {
    }

    public static boolean mustPreserve(String message, boolean excludeCommands, boolean excludePrivateMessages) {
        if (message == null || message.isBlank()) {
            return true;
        }
        String trimmed = message.trim();
        if (trimmed.startsWith(".")) {
            return true;
        }
        if (!trimmed.startsWith("/")) {
            return false;
        }
        String command = trimmed.substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (isAuthenticationCommand(command)) {
            return true;
        }
        if (excludeCommands) {
            return true;
        }
        return excludePrivateMessages && isPrivateMessageCommand(command);
    }

    private static boolean isAuthenticationCommand(String command) {
        return command.equals("login") || command.equals("l") || command.equals("register")
                || command.equals("reg") || command.equals("password") || command.equals("passwd")
                || command.equals("changepassword") || command.equals("cpw");
    }

    private static boolean isPrivateMessageCommand(String command) {
        return command.equals("msg") || command.equals("tell") || command.equals("w")
                || command.equals("whisper") || command.equals("message") || command.equals("m")
                || command.equals("pm") || command.equals("reply") || command.equals("r");
    }
}
