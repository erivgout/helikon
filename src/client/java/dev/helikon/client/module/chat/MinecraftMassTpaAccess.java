package dev.helikon.client.module.chat;

import dev.helikon.client.friend.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Thin adapter that reads the current player list and sends one ordinary command when due. */
public final class MinecraftMassTpaAccess {
    private MinecraftMassTpaAccess() {
    }

    public static void tick(long tick, MassTpa module, FriendManager friends) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer localPlayer = client.player;
        if (localPlayer == null || client.level == null || localPlayer.connection == null) {
            module.onContextLost();
            return;
        }
        if (dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen())) {
            return;
        }
        List<MassTpa.Candidate> candidates = new ArrayList<>();
        for (Player player : client.level.players()) {
            if (player == localPlayer) {
                continue;
            }
            String name = player.getGameProfile().name();
            candidates.add(new MassTpa.Candidate(name, friends.contains(name)));
        }
        module.nextCommand(tick, candidates).ifPresent(localPlayer.connection::sendCommand);
    }
}
