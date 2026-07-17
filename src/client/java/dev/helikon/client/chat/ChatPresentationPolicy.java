package dev.helikon.client.chat;

import dev.helikon.client.module.chat.ChatFilter;

import java.util.Objects;

/** Keeps local filter attention strictly behind the final visible-chat decision. */
public final class ChatPresentationPolicy {
    private static final ChatFilter.Decision NONE = new ChatFilter.Decision(false, false, false, false, false);

    private ChatPresentationPolicy() {
    }

    public static ChatFilter.Decision filterEffectsForVisibleLine(ChatFilter.Decision decision, boolean visible) {
        Objects.requireNonNull(decision, "decision");
        return visible ? decision : NONE;
    }
}
