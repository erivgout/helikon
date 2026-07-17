package dev.helikon.client.chat;

import dev.helikon.client.module.chat.ChatPrefix;
import dev.helikon.client.module.chat.ChatSuffix;

import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Composes local outgoing-chat formatting while preserving protected input verbatim. */
public final class OutgoingChatFormatter {
    /** Verified against 26.2 {@code ServerboundChatPacket}'s UTF message limit. */
    static final int MAX_CHAT_LENGTH = 256;
    private final ChatPrefix prefix;
    private final ChatSuffix suffix;
    private final Supplier<String> serverAddress;
    private final IntSupplier randomIndex;

    public OutgoingChatFormatter(ChatPrefix prefix, ChatSuffix suffix, Supplier<String> serverAddress,
                                 IntSupplier randomIndex) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.suffix = Objects.requireNonNull(suffix, "suffix");
        this.serverAddress = Objects.requireNonNull(serverAddress, "serverAddress");
        this.randomIndex = Objects.requireNonNull(randomIndex, "randomIndex");
    }

    public String format(String message) {
        String prefixed = prefix.format(message);
        String formatted = suffix.format(prefixed, serverAddress.get(), randomIndex.getAsInt());
        return formatted.length() <= MAX_CHAT_LENGTH ? formatted : message;
    }
}
