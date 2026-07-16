package dev.helikon.client.notification;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.command.CommandFeedback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Local-only notifications shown in the chat HUD, used for command feedback
 * and module failure notices. Falls back to the log when no player exists
 * (for example during startup). Nothing is ever sent to the server.
 */
public final class ChatNotifier implements CommandFeedback {
    private static final Component PREFIX = Component.literal("[Helikon] ").withStyle(ChatFormatting.GOLD);

    @Override
    public void info(String message) {
        send(message, ChatFormatting.GRAY);
    }

    @Override
    public void error(String message) {
        send(message, ChatFormatting.RED);
    }

    private void send(String message, ChatFormatting color) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            HelikonClient.LOGGER.info(() -> "[notification] " + message);
            return;
        }
        player.sendSystemMessage(Component.empty()
                .append(PREFIX)
                .append(Component.literal(message).withStyle(color)));
    }
}
