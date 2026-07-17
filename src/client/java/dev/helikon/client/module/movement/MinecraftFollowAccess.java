package dev.helikon.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Narrow 26.2 bridge for applying Follow's local horizontal velocity decision. */
public final class MinecraftFollowAccess {
    private MinecraftFollowAccess() {
    }

    public static void tick(Follow follow) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        LocalPlayer player = client.player;
        List<Follow.Target> targets = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == player || entity.isRemoved()) {
                continue;
            }
            double x = entity.getX() - player.getX();
            double z = entity.getZ() - player.getZ();
            targets.add(new Follow.Target(entity.getName().getString(), Math.hypot(x, z), x, z));
        }
        follow.velocity(new Follow.Context(client.gui.screen() != null, player.isPassenger(),
                player.getAbilities().flying, player.isFallFlying(), targets)).ifPresent(velocity -> {
            Vec3 current = player.getDeltaMovement();
            player.setDeltaMovement(velocity.x(), current.y, velocity.z());
        });
    }
}
