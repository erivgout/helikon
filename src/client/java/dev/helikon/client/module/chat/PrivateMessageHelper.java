package dev.helikon.client.module.chat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Optional;

/** Holds local-only PrivateMessageHelper preferences and validates server command tokens. */
public final class PrivateMessageHelper extends Module {
    private final StringSetting messageCommand;
    private final StringSetting replyCommand;
    private final NumberSetting recentLimit;
    private final BooleanSetting notifications;
    private final BooleanSetting sound;
    private final BooleanSetting highlight;

    public PrivateMessageHelper() {
        super("private_message_helper", "PrivateMessageHelper", "Sends configured normal private-message commands.",
                ModuleCategory.CHAT, true, Keybind.unbound());
        messageCommand = addSetting(new StringSetting("message_command", "Message command",
                "Normal server command token used by .pm, without the slash.", "msg", 32, false));
        replyCommand = addSetting(new StringSetting("reply_command", "Reply command",
                "Normal server command token used by .reply, without the slash.", "r", 32, false));
        recentLimit = addSetting(new NumberSetting("recent_limit", "Recent messages",
                "Maximum in-memory messages retained for each local conversation tab.", 20.0, 1.0, 100.0));
        notifications = addSetting(new BooleanSetting("notifications", "PM notifications",
                "Allow local notifications for recognized incoming private messages.", true));
        sound = addSetting(new BooleanSetting("sound", "PM sound",
                "Play a local UI sound for a recognized incoming private message.", true));
        highlight = addSetting(new BooleanSetting("highlight", "PM highlight",
                "Highlight a recognized incoming private message in the local chat HUD.", true));
    }

    public Optional<String> messageCommand() {
        return validToken(messageCommand.value());
    }

    public Optional<String> replyCommand() {
        return validToken(replyCommand.value());
    }

    public int recentLimit() {
        return (int) Math.round(recentLimit.value());
    }

    public boolean notifications() {
        return notifications.value();
    }

    public boolean sound() {
        return sound.value();
    }

    public boolean highlight() {
        return highlight.value();
    }

    private static Optional<String> validToken(String token) {
        String trimmed = token.trim();
        return trimmed.matches("[A-Za-z][A-Za-z0-9_-]{0,31}") ? Optional.of(trimmed) : Optional.empty();
    }
}
