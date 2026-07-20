package dev.helikon.client.module.world;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.Level;

import java.util.Objects;

/** Reads only already-loaded client entities and a locally owned integrated-server seed. */
public final class MinecraftSeedCrackerAccess {
    private final SeedCracker module;

    public MinecraftSeedCrackerAccess(SeedCracker module) {
        this.module = Objects.requireNonNull(module, "module");
    }

    public void tick(Minecraft client, long tick) {
        Objects.requireNonNull(client, "client");
        if (!module.isEnabled() || client.level == null || client.player == null) {
            return;
        }
        if (client.isLocalServer() && module.revealSingleplayerSeed()
                && client.getSingleplayerServer() != null) {
            module.revealLocalWorldSeed(client.getSingleplayerServer().overworld().getSeed());
            module.tick();
            return;
        }
        if (module.automaticSlimes() && client.level.dimension() == Level.OVERWORLD) {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof Slime slime && slime.isAlive() && slime.getY() < 40.0D
                        && client.level.hasChunk(slime.getBlockX() >> 4, slime.getBlockZ() >> 4)) {
                    module.observeSlime(slime.getUUID(), slime.getBlockX() >> 4, slime.getBlockZ() >> 4, tick);
                }
            }
        }
        module.tick();
    }
}
