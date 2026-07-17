package dev.helikon.client.module.chat;

import dev.helikon.client.chat.ChatMessageSafety;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;

/** Applies a bounded Unicode presentation style to ordinary outgoing chat. */
public final class FancyChat extends Module {
    public enum Style {
        FULLWIDTH,
        SMALL_CAPS,
        ALTERNATING
    }

    private static final String ASCII_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘqʀꜱᴛᴜᴠᴡxʏᴢ";
    private final EnumSetting<Style> style;
    private final BooleanSetting preserveCommands;

    public FancyChat() {
        super("fancy_chat", "FancyChat", "Stylizes ordinary outgoing chat with bounded Unicode text.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        style = addSetting(new EnumSetting<>("style", "Style", "Outgoing character presentation.",
                Style.class, Style.FULLWIDTH));
        preserveCommands = addSetting(new BooleanSetting("preserve_commands", "Preserve commands",
                "Leave slash commands, private-message commands, and Helikon commands unchanged.", true));
    }

    public String format(String message) {
        if (!isEnabled() || message == null || (preserveCommands.value()
                && ChatMessageSafety.mustPreserve(message, true, true))) {
            return message;
        }
        return switch (style.value()) {
            case FULLWIDTH -> fullwidth(message);
            case SMALL_CAPS -> smallCaps(message);
            case ALTERNATING -> alternating(message);
        };
    }

    private static String fullwidth(String text) {
        StringBuilder result = new StringBuilder(text.length());
        text.codePoints().forEach(codePoint -> {
            if (codePoint == ' ') {
                result.appendCodePoint(0x3000);
            } else if (codePoint >= 0x21 && codePoint <= 0x7E) {
                result.appendCodePoint(codePoint + 0xFEE0);
            } else {
                result.appendCodePoint(codePoint);
            }
        });
        return result.toString();
    }

    private static String smallCaps(String text) {
        StringBuilder result = new StringBuilder(text.length());
        text.codePoints().forEach(codePoint -> {
            int index = ASCII_UPPER.indexOf(Character.toUpperCase(codePoint));
            result.appendCodePoint(index < 0 ? codePoint : SMALL_CAPS.codePointAt(index));
        });
        return result.toString();
    }

    private static String alternating(String text) {
        StringBuilder result = new StringBuilder(text.length());
        boolean upper = true;
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (Character.isLetter(value)) {
                result.append(upper ? Character.toUpperCase(value) : Character.toLowerCase(value));
                upper = !upper;
            } else {
                result.append(value);
            }
        }
        return result.toString();
    }
}
