package dev.helikon.client.module.chat;

import dev.helikon.client.chat.ChatTimestampFormat;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;

import java.time.Instant;
import java.time.ZoneId;

/** Local display settings for timestamp labels added to chat lines. */
public final class ChatTimestamps extends Module {
    private final BooleanSetting twentyFourHour;
    private final BooleanSetting includeSeconds;
    private final BooleanSetting brackets;
    private final ColorSetting color;
    private final BooleanSetting relativeMode;
    private final Instant sessionStart = Instant.now();

    public ChatTimestamps() {
        super("chat_timestamps", "ChatTimestamps", "Adds local time labels to incoming chat lines.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        twentyFourHour = addSetting(new BooleanSetting("twenty_four_hour", "24-hour clock",
                "Use a local 24-hour timestamp instead of AM/PM.", true));
        includeSeconds = addSetting(new BooleanSetting("include_seconds", "Include seconds",
                "Include seconds in local clock timestamps.", false));
        brackets = addSetting(new BooleanSetting("brackets", "Brackets",
                "Wrap the local timestamp label in square brackets.", true));
        color = addSetting(new ColorSetting("color", "Timestamp color",
                "Local ARGB color used for the timestamp label.", 0xFF808080));
        relativeMode = addSetting(new BooleanSetting("relative_mode", "Relative mode",
                "Show elapsed client-session time instead of local clock time.", false));
    }

    public String label(Instant timestamp, ZoneId zone) {
        return ChatTimestampFormat.format(timestamp, sessionStart, zone, twentyFourHour.value(), includeSeconds.value(),
                brackets.value(), relativeMode.value());
    }

    /** Minecraft components use RGB; configuration preserves the alpha for a future chat-style adapter. */
    public int rgbColor() {
        return color.value() & 0x00FFFFFF;
    }
}
