package dev.helikon.client.chat;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Minecraft-only conversion of incoming components into a small filtering model. */
public final class IncomingMessageAdapter {
    private IncomingMessageAdapter() {
    }

    public static IncomingChatMessage chat(Component message, GameProfile sender, long receivedAtMillis) {
        return new IncomingChatMessage(IncomingChatMessage.Channel.CHAT, message.getString(),
                sender == null ? "" : sender.name(), translationKey(message), false, receivedAtMillis);
    }

    public static IncomingChatMessage game(Component message, boolean overlay, long receivedAtMillis) {
        return new IncomingChatMessage(IncomingChatMessage.Channel.GAME, message.getString(), "",
                translationKey(message), overlay, receivedAtMillis);
    }

    private static String translationKey(Component component) {
        return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
    }
}
