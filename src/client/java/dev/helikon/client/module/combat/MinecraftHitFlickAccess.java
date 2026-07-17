package dev.helikon.client.module.combat;

import dev.helikon.client.friend.FriendManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Narrow 26.2 bridge for HitFlick. On an ordinary local melee attack it sends one well-formed vanilla
 * rotation packet reflecting the module's flicked yaw, then restores the real view with a second
 * rotation packet on the next client tick. It never changes the local player's own yaw, edits an
 * entity, or fabricates a malformed packet; the connected server still authorizes any knockback.
 */
public final class MinecraftHitFlickAccess {
    private static volatile HitFlick module;
    private static volatile FriendManager friends;
    private static Restore pendingRestore;

    private record Restore(float yaw, float pitch, boolean onGround, boolean horizontalCollision) {
    }

    private MinecraftHitFlickAccess() {
    }

    /** Registers the ordinary Fabric attack event; the flick decision itself stays in the module. */
    public static void install(HitFlick hitFlick, FriendManager friendManager) {
        module = Objects.requireNonNull(hitFlick, "hitFlick");
        friends = Objects.requireNonNull(friendManager, "friendManager");
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            onAttack(player, entity);
            return InteractionResult.PASS;
        });
    }

    /**
     * Sends the deferred restore rotation so the server's tracked view returns to the real aim after the
     * flicked attack packet has been dispatched. Safe to call every tick and while disabled.
     */
    public static void tickRestore() {
        Restore restore = pendingRestore;
        pendingRestore = null;
        if (restore == null) {
            return;
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundMovePlayerPacket.Rot(restore.yaw(), restore.pitch(),
                    restore.onGround(), restore.horizontalCollision()));
        }
    }

    /** Clears any pending restore, e.g. on disable or when leaving the world. */
    public static void reset() {
        pendingRestore = null;
    }

    private static void onAttack(Player player, Entity entity) {
        HitFlick hitFlick = module;
        FriendManager friendManager = friends;
        if (hitFlick == null || friendManager == null || !hitFlick.isEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer local = client.player;
        if (local == null || player != local || local.isSpectator()) {
            return;
        }
        if (!(entity instanceof LivingEntity) || entity == local) {
            return;
        }
        if (hitFlick.excludeFriends() && entity instanceof Player target && isFriend(friendManager, target)) {
            return;
        }
        ClientPacketListener connection = client.getConnection();
        if (connection == null) {
            return;
        }
        float yawToTarget = yawToward(local, entity);
        hitFlick.flickYaw(yawToTarget).ifPresent(flickedYaw -> {
            // Restore the real view first so a disable or world change between attacks cannot leave the
            // server tracking the flicked yaw.
            pendingRestore = new Restore(local.getYRot(), local.getXRot(), local.onGround(),
                    local.horizontalCollision);
            connection.send(new ServerboundMovePlayerPacket.Rot(flickedYaw, local.getXRot(), local.onGround(),
                    local.horizontalCollision));
        });
    }

    private static boolean isFriend(FriendManager friendManager, Player target) {
        String name = target.getGameProfile().name();
        return name != null && !name.isBlank() && friendManager.contains(name.trim());
    }

    private static float yawToward(LocalPlayer local, Entity entity) {
        Vec3 eye = local.getEyePosition();
        double dx = entity.getX() - eye.x;
        double dz = entity.getZ() - eye.z;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
