package dev.helikon.client.chat;

import dev.helikon.client.module.chat.ChatTimestamps;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/** Thin Minecraft display adapter for optional local chat decorations. */
public final class ChatDisplayAccess {
    private static ChatTimestamps timestamps;

    private ChatDisplayAccess() {
    }

    public static void install(ChatTimestamps chatTimestamps) {
        timestamps = Objects.requireNonNull(chatTimestamps, "chatTimestamps");
    }

    public static Component decorateTimestamp(Component message) {
        if (timestamps == null || !timestamps.isEnabled()) {
            return message;
        }
        return Component.empty()
                .append(Component.literal(timestamps.label(Instant.now(), ZoneId.systemDefault()))
                        .withColor(timestamps.rgbColor()))
                .append(message);
    }

    /** Rebuilds a display message after Minecraft has already logged its original content. */
    public static GuiMessage decorateTimestamp(GuiMessage message) {
        Component decorated = decorateTimestamp(message.content());
        if (decorated == message.content()) {
            return message;
        }
        return new GuiMessage(message.addedTime(), decorated, message.signature(), message.source(), message.tag());
    }
}
