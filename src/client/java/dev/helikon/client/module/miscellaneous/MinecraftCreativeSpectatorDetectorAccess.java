package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.friend.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Reads nearby player game modes from the ordinary client player list for the safety detector. */
public final class MinecraftCreativeSpectatorDetectorAccess {
    private MinecraftCreativeSpectatorDetectorAccess() {
    }

    public static Optional<CreativeSpectatorDetector.Candidate> observe(
            CreativeSpectatorDetector module,
            FriendManager friends
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.getConnection() == null) {
            return module.observe(List.of());
        }

        List<CreativeSpectatorDetector.Candidate> candidates = new ArrayList<>();
        for (Player player : client.level.players()) {
            if (player == client.player) {
                continue;
            }
            PlayerInfo info = client.getConnection().getPlayerInfo(player.getUUID());
            GameType gameMode = info == null ? null : info.getGameMode();
            CreativeSpectatorDetector.Mode mode;
            if (gameMode == GameType.CREATIVE) {
                mode = CreativeSpectatorDetector.Mode.CREATIVE;
            } else if (gameMode == GameType.SPECTATOR) {
                mode = CreativeSpectatorDetector.Mode.SPECTATOR;
            } else {
                continue;
            }
            String name = player.getGameProfile().name();
            if (name == null || name.isBlank()) {
                continue;
            }
            candidates.add(new CreativeSpectatorDetector.Candidate(
                    player.getUUID().toString(), name.trim(), mode, friends.contains(name),
                    client.player.distanceTo(player)));
        }
        return module.observe(candidates);
    }
}
