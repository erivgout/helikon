package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Narrow 26.2 adapter for the bounded vanilla position/attack/return sequence. */
public final class MinecraftGojoInfinityAccess {
    private static final int MAX_OBSERVED_THREATS = 16;
    private static final double MINIMUM_HORIZONTAL_SEPARATION = 1.0e-4D;

    private MinecraftGojoInfinityAccess() {
    }

    /**
     * Performs one bounded loop: report a temporary position between player and threat, request one
     * ordinary attack, then report and restore the original position.
     */
    public static boolean tick(long tick, GojosInfinity module, MinecraftCombatAccess.Snapshot snapshot,
                               CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (!snapshot.available() || client.player == null || client.level == null) {
            module.resetTransientState();
            return false;
        }
        if (client.gameMode == null || client.gui.screen() != null || client.player.connection == null) {
            return false;
        }

        LocalPlayer player = client.player;
        List<GojosInfinity.Threat> threats = observations(player, snapshot);
        Optional<String> selected = module.selectThreat(tick, attackReady(player), threats);
        if (selected.isEmpty()) {
            return false;
        }
        CombatTarget targetFacts = snapshot.targets().stream()
                .filter(target -> target.id().equals(selected.get()))
                .findFirst()
                .orElse(null);
        LivingEntity target = snapshot.entities().get(selected.get());
        if (targetFacts == null || target == null || target.isRemoved() || !target.isAlive()
                || !player.hasLineOfSight(target)) {
            return false;
        }

        Vec3 original = player.position();
        Optional<Vec3> attackPosition = attackPosition(client, player, target, module.attackDistance());
        if (attackPosition.isEmpty() || original.distanceTo(attackPosition.get()) > module.barrierRadius() + 1.0D) {
            return false;
        }
        return teleportAttackAndReturn(player, client, target, targetFacts, tracker, original, attackPosition.get());
    }

    private static List<GojosInfinity.Threat> observations(LocalPlayer player,
                                                            MinecraftCombatAccess.Snapshot snapshot) {
        Vec3 playerVelocity = player.getDeltaMovement();
        List<CombatTarget> nearest = snapshot.targets().stream()
                .sorted(Comparator.comparingDouble(CombatTarget::distance))
                .limit(MAX_OBSERVED_THREATS)
                .toList();
        List<GojosInfinity.Threat> threats = new ArrayList<>(nearest.size());
        for (CombatTarget target : nearest) {
            double horizontalDistance = Math.hypot(target.relativeX(), target.relativeZ());
            double closingSpeed = 0.0D;
            if (horizontalDistance > MINIMUM_HORIZONTAL_SEPARATION) {
                double relativeVelocityX = target.velocityX() - playerVelocity.x();
                double relativeVelocityZ = target.velocityZ() - playerVelocity.z();
                closingSpeed = -(target.relativeX() * relativeVelocityX
                        + target.relativeZ() * relativeVelocityZ) / horizontalDistance;
            }
            threats.add(new GojosInfinity.Threat(target.id(), target.type(), target.friend(),
                    target.suspectedBot(), target.alive(), target.lineOfSight(), target.distance(), closingSpeed));
        }
        return List.copyOf(threats);
    }

    private static Optional<Vec3> attackPosition(Minecraft client, LocalPlayer player, LivingEntity target,
                                                  double attackDistance) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double horizontalDistance = Math.hypot(dx, dz);
        if (horizontalDistance <= MINIMUM_HORIZONTAL_SEPARATION) {
            return Optional.empty();
        }

        double x = target.getX() - dx / horizontalDistance * attackDistance;
        double y = target.getY();
        double z = target.getZ() - dz / horizontalDistance * attackDistance;
        BlockPos destinationBlock = BlockPos.containing(x, y, z);
        if (!client.level.isInsideBuildHeight(destinationBlock.getY())
                || !client.level.hasChunk(destinationBlock.getX() >> 4, destinationBlock.getZ() >> 4)) {
            return Optional.empty();
        }

        Vec3 destination = new Vec3(x, y, z);
        Vec3 displacement = destination.subtract(player.position());
        AABB destinationBox = player.getBoundingBox().move(displacement);
        return client.level.noCollision(player, destinationBox) ? Optional.of(destination) : Optional.empty();
    }

    private static boolean teleportAttackAndReturn(LocalPlayer player, Minecraft client, LivingEntity target,
                                                    CombatTarget facts, CombatTargetTracker tracker,
                                                    Vec3 original, Vec3 attackPosition) {
        boolean moved = false;
        try {
            player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    attackPosition, player.onGround(), player.horizontalCollision
            ));
            player.setPos(attackPosition);
            moved = true;
            client.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
            tracker.recordAttack(facts);
            return true;
        } finally {
            if (moved) {
                try {
                    player.connection.send(new ServerboundMovePlayerPacket.Pos(
                            original, player.onGround(), player.horizontalCollision
                    ));
                } finally {
                    player.setPos(original);
                }
            }
        }
    }

    private static boolean attackReady(LocalPlayer player) {
        return player.getAttackStrengthScale(0.0F) >= 0.9F;
    }
}
