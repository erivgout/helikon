package dev.helikon.client.chat;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Minecraft-only conversion of incoming components into a small filtering model. */
public final class IncomingMessageAdapter {
    private IncomingMessageAdapter() {
    }

    public static IncomingChatMessage chat(Component message, PlayerChatMessage signedMessage, GameProfile sender,
                                           long receivedAtMillis) {
        String displayText = message.getString();
        String rawText = signedMessage == null ? displayText : signedMessage.signedBody().content();
        return new IncomingChatMessage(IncomingChatMessage.Channel.CHAT, displayText, rawText,
                sender == null ? "" : sender.name(), translationKey(message), false, receivedAtMillis);
    }

    public static IncomingChatMessage chat(Component message, GameProfile sender, long receivedAtMillis) {
        return chat(message, null, sender, receivedAtMillis);
    }

    public static IncomingChatMessage game(Component message, boolean overlay, long receivedAtMillis) {
        return new IncomingChatMessage(IncomingChatMessage.Channel.GAME, message.getString(), message.getString(), "",
                translationKey(message), overlay, receivedAtMillis);
    }

    private static String translationKey(Component component) {
        return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
    }
}
