package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetTracker;
import dev.helikon.client.friend.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Narrow 26.2 adapter for legitimate rotation, sprint, and ordinary attack requests. */
public final class MinecraftGojoInfinityAccess {
    private static final double MINIMUM_SEPARATION = 1.0E-6D;
    private static volatile GojosInfinity module;
    private static volatile FriendManager friends;

    private MinecraftGojoInfinityAccess() {
    }

    public static void install(GojosInfinity infinity, FriendManager friendManager) {
        module = Objects.requireNonNull(infinity, "infinity");
        friends = Objects.requireNonNull(friendManager, "friendManager");
    }

    public static boolean tick(long tick, GojosInfinity current,
                               MinecraftCombatAccess.Snapshot snapshot, CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || !snapshot.available()) {
            current.onContextLost();
            return false;
        }
        if (client.gui.screen() != null || player.connection == null) {
            return false;
        }

        Map<String, CombatTarget> factsById = new HashMap<>();
        List<GojosInfinity.Threat> threats = new ArrayList<>();
        for (CombatTarget facts : snapshot.targets()) {
            LivingEntity entity = snapshot.entities().get(facts.id());
            if (entity == null || entity == player || entity.isRemoved()) {
                continue;
            }
            double distance = surfaceDistance(player.getBoundingBox(), entity.getBoundingBox());
            if (distance > current.detectionRadius()) {
                continue;
            }
            Vec3 relative = entity.getBoundingBox().getCenter().subtract(player.getBoundingBox().getCenter());
            Vec3 relativeVelocity = entity.getDeltaMovement().subtract(player.getDeltaMovement());
            double separation = Math.max(relative.length(), MINIMUM_SEPARATION);
            double closingSpeed = -relative.dot(relativeVelocity) / separation;
            double impactTicks = closingSpeed > MINIMUM_SEPARATION
                    ? distance / closingSpeed : Double.MAX_VALUE;
            boolean ownerPet = entity instanceof OwnableEntity ownable && ownable.getOwner() == player;
            threats.add(new GojosInfinity.Threat(facts.id(), kind(facts.type()), facts.friend(), ownerPet,
                    entity instanceof ArmorStand, facts.suspectedBot(), entity.isAlive(),
                    player.hasLineOfSight(entity), player.isWithinEntityInteractionRange(entity, 0.0D),
                    distance, closingSpeed, impactTicks));
            factsById.put(facts.id(), facts);
        }

        Optional<GojosInfinity.AttackPlan> selected =
                current.plan(tick, player.getAttackStrengthScale(0.0F), threats);
        if (selected.isEmpty()) {
            return false;
        }

        GojosInfinity.AttackPlan plan = selected.orElseThrow();
        boolean attacked = false;
        for (String id : plan.targetIds()) {
            LivingEntity target = snapshot.entities().get(id);
            CombatTarget facts = factsById.get(id);
            if (target == null || facts == null || target.isRemoved() || !target.isAlive()
                    || !player.hasLineOfSight(target)
                    || !player.isWithinEntityInteractionRange(target, 0.0D)) {
                continue;
            }
            attackFacing(client, player, target, plan);
            tracker.recordAttack(facts);
            attacked = true;
        }
        if (attacked) {
            current.markExecuted(tick);
        }
        return attacked;
    }

    private static void attackFacing(Minecraft client, LocalPlayer player, LivingEntity target,
                                     GojosInfinity.AttackPlan plan) {
        Rotation rotation = rotationTo(player, target);
        float originalYaw = player.getYRot();
        float originalPitch = player.getXRot();
        if (plan.silentRotation()) {
            player.connection.send(new ServerboundMovePlayerPacket.Rot(rotation.yaw(), rotation.pitch(),
                    player.onGround(), player.horizontalCollision));
        } else {
            player.setYRot(rotation.yaw());
            player.setXRot(rotation.pitch());
        }
        if (plan.sprintReset()) {
            player.connection.send(new ServerboundPlayerCommandPacket(player,
                    ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            player.connection.send(new ServerboundPlayerCommandPacket(player,
                    ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        }
        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
        if (plan.silentRotation()) {
            player.connection.send(new ServerboundMovePlayerPacket.Rot(originalYaw, originalPitch,
                    player.onGround(), player.horizontalCollision));
        }
    }

    private static Rotation rotationTo(LocalPlayer player, LivingEntity target) {
        Vec3 from = player.getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        double x = to.x - from.x;
        double y = to.y - from.y;
        double z = to.z - from.z;
        double horizontal = Math.hypot(x, z);
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, horizontal));
        return new Rotation(yaw, Math.clamp(pitch, -90.0F, 90.0F));
    }

    private static double surfaceDistance(AABB first, AABB second) {
        double x = axisGap(first.minX, first.maxX, second.minX, second.maxX);
        double y = axisGap(first.minY, first.maxY, second.minY, second.maxY);
        double z = axisGap(first.minZ, first.maxZ, second.minZ, second.maxZ);
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static double axisGap(double firstMinimum, double firstMaximum,
                                  double secondMinimum, double secondMaximum) {
        if (secondMinimum > firstMaximum) {
            return secondMinimum - firstMaximum;
        }
        if (firstMinimum > secondMaximum) {
            return firstMinimum - secondMaximum;
        }
        return 0.0D;
    }

    private static GojosInfinity.TargetKind kind(CombatEntityType type) {
        return switch (type) {
            case PLAYER -> GojosInfinity.TargetKind.PLAYER;
            case HOSTILE -> GojosInfinity.TargetKind.HOSTILE;
            case PASSIVE -> GojosInfinity.TargetKind.ANIMAL;
        };
    }

    private record Rotation(float yaw, float pitch) {
    }
}
