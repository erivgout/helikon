package dev.helikon.client.module.movement;

import dev.helikon.client.input.KeybindManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Narrow 26.2 client bridge for TpClick. It reads the module's bound key, casts
 * one ordinary block ray from the local eyes, and applies only a local position
 * change. Minecraft's normal movement packet then reports the new position, so
 * the server retains full authority to reject or correct it. It never builds a
 * packet or treats client motion as server truth.
 */
public final class MinecraftTpClickAccess {
    private MinecraftTpClickAccess() {
    }

    public static void tick(TpClick module, KeybindManager.KeyStateReader keys) {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(keys, "keys");
        Minecraft client = Minecraft.getInstance();
        boolean screenOpen = client.gui.screen() != null;
        boolean keyDown = keys.isDown(module.keybind());
        if (!module.pollTrigger(keyDown, screenOpen)) {
            return;
        }
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0F).scale(module.maxDistance()));
        BlockHitResult hit = client.level.clip(new ClipContext(eye, reach,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos block = hit.getBlockPos();
        Direction face = hit.getDirection();
        module.destination(block.getX(), block.getY(), block.getZ(),
                        face.getStepX(), face.getStepY(), face.getStepZ(), eye.distanceTo(hit.getLocation()))
                .ifPresent(destination -> {
                    player.setPos(destination.x(), destination.y(), destination.z());
                    if (module.cancelVelocity()) {
                        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                        player.fallDistance = 0.0F;
                    }
                });
    }
}
