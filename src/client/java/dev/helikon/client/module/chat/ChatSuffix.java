package dev.helikon.client.module.chat;

import dev.helikon.client.chat.ChatMessageSafety;
import dev.helikon.client.chat.SuffixOptions;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.StringSetting;

/** Adds a safe outgoing suffix with optional local server and random lists. */
public final class ChatSuffix extends Module {
    private final StringSetting suffix;
    private final StringSetting separator;
    private final StringSetting perServerSuffixes;
    private final StringSetting randomSuffixes;
    private final BooleanSetting excludeCommands;
    private final BooleanSetting excludePrivateMessages;

    public ChatSuffix() {
        super("chat_suffix", "ChatSuffix", "Adds a local suffix to ordinary outgoing chat.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        suffix = addSetting(new StringSetting("suffix", "Suffix", "Fallback text appended to safe ordinary chat.",
                "", 64, true));
        separator = addSetting(new StringSetting("separator", "Separator", "Text between the message and suffix.",
                " | ", 16, true));
        perServerSuffixes = addSetting(new StringSetting("per_server_suffixes", "Per-server suffixes",
                "Semicolon-separated server=suffix entries, matched locally by normalized address.", "", 255, true));
        randomSuffixes = addSetting(new StringSetting("random_suffixes", "Random suffixes",
                "Comma-separated suffixes; one is chosen when no server entry applies.", "", 255, true));
        excludeCommands = addSetting(new BooleanSetting("exclude_commands", "Exclude commands",
                "Never alter slash commands or Helikon local commands.", true));
        excludePrivateMessages = addSetting(new BooleanSetting("exclude_private_messages", "Exclude private messages",
                "Never alter common private-message command forms.", true));
    }

    public String format(String message, String serverAddress, int randomIndex) {
        if (!isEnabled() || ChatMessageSafety.mustPreserve(message, excludeCommands.value(), excludePrivateMessages.value())) {
            return message;
        }
        return SuffixOptions.select(suffix.value(), perServerSuffixes.value(), randomSuffixes.value(), serverAddress, randomIndex)
                .map(selected -> message + separator.value() + selected)
                .orElse(message);
    }
}
