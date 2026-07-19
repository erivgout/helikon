package dev.helikon.client.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Thin 26.2 adapter for Regen's bounded, vanilla-shaped movement-status burst. */
public final class MinecraftRegenAccess {
    private MinecraftRegenAccess() {
    }

    public static void tick(long tick, Regen module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || player.connection == null) {
            module.onContextLost();
            return;
        }
        int count = module.packetCount(tick, new Regen.Context(
                player.getHealth(), player.getMaxHealth(), player.getFoodData().getFoodLevel(),
                dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen()),
                player.onGround(), player.isPassenger(),
                player.getAbilities().flying, player.isFallFlying()));
        for (int index = 0; index < count; index++) {
            player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision));
        }
    }
}
