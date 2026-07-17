package dev.helikon.client.module.player;

import net.minecraft.client.Minecraft;

/** Minecraft-only integrated-server pause adapter. */
public final class MinecraftPotionSaverAccess {
    private int stationaryTicks;

    public void tick(PotionSaver module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            stationaryTicks = 0;
            return;
        }
        boolean moving = client.player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5
                || Math.abs(client.player.getDeltaMovement().y) > 1.0E-4;
        stationaryTicks = moving ? 0 : Math.min(1200, stationaryTicks + 1);
        boolean beneficial = client.player.getActiveEffects().stream()
                .anyMatch(effect -> effect.getEffect().value().isBeneficial());
        if (module.shouldPause(client.hasSingleplayerServer(), beneficial, moving,
                client.gui.screen() != null, stationaryTicks)) {
            client.pauseGame(false);
            stationaryTicks = 0;
        }
    }
}
