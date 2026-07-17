package dev.helikon.client.module.chat;

import dev.helikon.client.chat.ChatMessageSafety;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;

import java.util.ArrayList;
import java.util.List;

/** Splits long input into a bounded sequence of protocol-valid ordinary chat messages. */
public final class InfiniChat extends Module {
    public static final int CHAT_LIMIT = 256;
    private final IntegerSetting maximumParts;
    private final BooleanSetting preferWordBoundaries;
    private final BooleanSetting continuationMarker;

    public InfiniChat() {
        super("infini_chat", "InfiniChat",
                "Splits long ordinary chat into multiple vanilla-sized messages instead of sending an oversized packet.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        maximumParts = addSetting(new IntegerSetting("maximum_parts", "Maximum parts",
                "Maximum normal messages produced from one submitted input.", 4, 2, 10));
        preferWordBoundaries = addSetting(new BooleanSetting("prefer_word_boundaries", "Word boundaries",
                "Split at whitespace when possible.", true));
        continuationMarker = addSetting(new BooleanSetting("continuation_marker", "Continuation marker",
                "Prefix later parts with an ellipsis marker.", true));
    }

    /** Returns no parts when vanilla can send the input unchanged. */
    public List<String> split(String message) {
        if (!isEnabled() || message == null || message.length() <= CHAT_LIMIT
                || ChatMessageSafety.mustPreserve(message, true, true)) {
            return List.of();
        }
        String remaining = message;
        List<String> result = new ArrayList<>();
        while (!remaining.isEmpty() && result.size() < maximumParts.value()) {
            String marker = result.isEmpty() || !continuationMarker.value() ? "" : "… ";
            int capacity = CHAT_LIMIT - marker.length();
            int end = Math.min(capacity, remaining.length());
            if (preferWordBoundaries.value() && end < remaining.length()) {
                int whitespace = remaining.lastIndexOf(' ', end);
                if (whitespace >= capacity / 2) {
                    end = whitespace;
                }
            }
            String part = remaining.substring(0, end).stripTrailing();
            if (part.isEmpty()) {
                end = Math.min(capacity, remaining.length());
                part = remaining.substring(0, end);
            }
            result.add(marker + part);
            remaining = remaining.substring(end).stripLeading();
        }
        return remaining.isEmpty() ? List.copyOf(result) : List.of();
    }
}
