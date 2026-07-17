package dev.helikon.client.module.combat;

import net.minecraft.client.Minecraft;

/** Narrow 26.2 bridge for Minecraft's ordinary local-player respawn action. */
public final class MinecraftAutoRespawnAccess {
    private MinecraftAutoRespawnAccess() {
    }

    /** Calls the same LocalPlayer respawn action used by Minecraft's DeathScreen. */
    public static void tick(long tick, AutoRespawn autoRespawn) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            autoRespawn.onContextLost();
            return;
        }
        if (autoRespawn.shouldRequestRespawn(tick, client.player.isDeadOrDying())) {
            client.player.respawn();
        }
    }
}
