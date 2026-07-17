package dev.helikon.client.module.chat;

import dev.helikon.client.chat.ChatMessageSafety;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.StringSetting;

/** Adds a configured client-side prefix only to ordinary safe outgoing chat. */
public final class ChatPrefix extends Module {
    private final StringSetting prefix;
    private final StringSetting separator;
    private final BooleanSetting excludeCommands;
    private final BooleanSetting excludePrivateMessages;

    public ChatPrefix() {
        super("chat_prefix", "ChatPrefix", "Adds a local prefix to ordinary outgoing chat.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        prefix = addSetting(new StringSetting("prefix", "Prefix", "Text placed before safe ordinary chat.",
                "", 64, true));
        separator = addSetting(new StringSetting("separator", "Separator", "Text between the prefix and message.",
                " ", 16, true));
        excludeCommands = addSetting(new BooleanSetting("exclude_commands", "Exclude commands",
                "Never alter slash commands or Helikon local commands.", true));
        excludePrivateMessages = addSetting(new BooleanSetting("exclude_private_messages", "Exclude private messages",
                "Never alter common private-message command forms.", true));
    }

    public String format(String message) {
        if (!isEnabled() || ChatMessageSafety.mustPreserve(message, excludeCommands.value(), excludePrivateMessages.value())
                || prefix.value().isBlank()) {
            return message;
        }
        return prefix.value() + separator.value() + message;
    }
}
