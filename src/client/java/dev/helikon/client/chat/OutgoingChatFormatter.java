package dev.helikon.client.chat;

import dev.helikon.client.module.chat.ChatPrefix;
import dev.helikon.client.module.chat.ChatSuffix;
import dev.helikon.client.module.chat.FancyChat;

import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Composes local outgoing-chat formatting while preserving protected input verbatim. */
public final class OutgoingChatFormatter {
    /** Verified against 26.2 {@code ServerboundChatPacket}'s UTF message limit. */
    static final int MAX_CHAT_LENGTH = 256;
    private final ChatPrefix prefix;
    private final ChatSuffix suffix;
    private final FancyChat fancyChat;
    private final Supplier<String> serverAddress;
    private final IntSupplier randomIndex;

    public OutgoingChatFormatter(ChatPrefix prefix, ChatSuffix suffix, Supplier<String> serverAddress,
                                 IntSupplier randomIndex) {
        this(prefix, suffix, null, serverAddress, randomIndex);
    }

    public OutgoingChatFormatter(ChatPrefix prefix, ChatSuffix suffix, FancyChat fancyChat,
                                 Supplier<String> serverAddress, IntSupplier randomIndex) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.suffix = Objects.requireNonNull(suffix, "suffix");
        this.fancyChat = fancyChat;
        this.serverAddress = Objects.requireNonNull(serverAddress, "serverAddress");
        this.randomIndex = Objects.requireNonNull(randomIndex, "randomIndex");
    }

    public String format(String message) {
        String stylized = fancyChat == null ? message : fancyChat.format(message);
        String prefixed = prefix.format(stylized);
        String formatted = suffix.format(prefixed, serverAddress.get(), randomIndex.getAsInt());
        return formatted.length() <= MAX_CHAT_LENGTH ? formatted : message;
    }
}
