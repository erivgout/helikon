package dev.helikon.client.module.movement;

import dev.helikon.client.friend.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Narrow adapter shared by the small legacy movement modules. */
public final class MinecraftLegacyMovementAccess {
    private static boolean ownsNoPhysics;

    private MinecraftLegacyMovementAccess() {
    }

    public static void tickNoClip(NoClip module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (!module.isEnabled() || player == null || client.level == null || player.isSpectator()) {
            if (player != null && ownsNoPhysics && player.noPhysics) {
                player.noPhysics = false;
            }
            ownsNoPhysics = false;
            return;
        }
        player.noPhysics = true;
        ownsNoPhysics = true;
        double forward = axis(client.options.keyUp.isDown(), client.options.keyDown.isDown());
        double sideways = axis(client.options.keyRight.isDown(), client.options.keyLeft.isDown());
        NoClip.Motion motion = module.motion(player.getYRot(), forward, sideways,
                client.options.keyJump.isDown(), client.options.keyShift.isDown());
        player.setDeltaMovement(motion.x(), motion.y(), motion.z());
    }

    public static void tickSnowShoe(SnowShoe module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return;
        }
        BlockPos feet = player.blockPosition();
        boolean powderSnow = client.level.getBlockState(feet).is(Blocks.POWDER_SNOW)
                || client.level.getBlockState(feet.below()).is(Blocks.POWDER_SNOW);
        Vec3 velocity = player.getDeltaMovement();
        double vertical = module.verticalVelocity(powderSnow, client.options.keyJump.isDown(), velocity.y);
        if (vertical != velocity.y) {
            player.setDeltaMovement(velocity.x, vertical, velocity.z);
        }
    }

    public static void tickMountBypass(MountBypass module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (!module.isEnabled() || player == null || client.level == null || client.gui.screen() != null) {
            return;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            return;
        }
        double forward = axis(client.options.keyUp.isDown(), client.options.keyDown.isDown());
        double sideways = axis(client.options.keyRight.isDown(), client.options.keyLeft.isDown());
        NoClip.Motion motion = module.motion(player.getYRot(), forward, sideways,
                client.options.keyJump.isDown(), client.options.keyShift.isDown());
        vehicle.setDeltaMovement(motion.x(), motion.y(), motion.z());
    }

    public static void tickForcePush(ForcePush module, FriendManager friends) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gui.screen() != null || player.isPassenger()) {
            return;
        }
        List<ForcePush.Candidate> candidates = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity) || entity == player || entity.isRemoved()) {
                continue;
            }
            double x = entity.getX() - player.getX();
            double z = entity.getZ() - player.getZ();
            double distance = Math.hypot(x, z);
            boolean friend = entity instanceof Player target
                    && friends.contains(target.getGameProfile().name());
            candidates.add(new ForcePush.Candidate(entity.getId(), friend, distance, x, z));
        }
        module.motion(client.options.keyAttack.isDown(), candidates).ifPresent(motion -> {
            Vec3 current = player.getDeltaMovement();
            player.setDeltaMovement(motion.x(), current.y, motion.z());
        });
    }

    public static void tickPhase(long tick, Phase module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            module.onContextLost();
            return;
        }
        double forward = axis(client.options.keyUp.isDown(), client.options.keyDown.isDown());
        double sideways = axis(client.options.keyRight.isDown(), client.options.keyLeft.isDown());
        if (!module.canAttempt(tick, player.horizontalCollision, forward != 0.0D || sideways != 0.0D)) {
            return;
        }
        double yaw = Math.toRadians(player.getYRot());
        double length = Math.max(1.0D, Math.hypot(forward, sideways));
        double directionX = (-Math.sin(yaw) * forward + Math.cos(yaw) * sideways) / length;
        double directionZ = (Math.cos(yaw) * forward + Math.sin(yaw) * sideways) / length;
        AABB original = player.getBoundingBox();
        for (double distance = module.stepDistance(); distance <= module.maximumDistance() + 1.0E-6D;
             distance += module.stepDistance()) {
            double x = player.getX() + directionX * distance;
            double z = player.getZ() + directionZ * distance;
            if (!client.level.hasChunk(Mth.floor(x) >> 4, Mth.floor(z) >> 4)) {
                break;
            }
            AABB destination = original.move(directionX * distance, 0.0D, directionZ * distance);
            if (client.level.noCollision(player, destination)) {
                player.setPos(x, player.getY(), z);
                module.markAttempt(tick);
                return;
            }
        }
        module.markAttempt(tick);
    }

    private static double axis(boolean positive, boolean negative) {
        return (positive ? 1.0D : 0.0D) - (negative ? 1.0D : 0.0D);
    }
}
