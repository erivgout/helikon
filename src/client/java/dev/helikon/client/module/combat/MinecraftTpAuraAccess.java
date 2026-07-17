package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Narrow 26.2 adapter for a validated movement-out, ordinary-attack, movement-back sequence. */
public final class MinecraftTpAuraAccess {
    private MinecraftTpAuraAccess() {
    }

    public static boolean tick(long tick, TpAura module, MinecraftCombatAccess.Snapshot snapshot,
                               CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || !snapshot.available()) {
            module.onContextLost();
            return false;
        }
        if (client.gui.screen() != null || player.connection == null) {
            return false;
        }
        Optional<TpAura.AttackPlan> selected = module.nextPlan(tick,
                player.getAttackStrengthScale(0.0F) >= 0.9F, snapshot.targets());
        if (selected.isEmpty()) {
            return false;
        }
        TpAura.AttackPlan plan = selected.orElseThrow();
        LivingEntity entity = snapshot.entities().get(plan.target().id());
        if (entity == null || entity.isRemoved() || !entity.isAlive() || !player.hasLineOfSight(entity)) {
            return false;
        }

        TpAura.Point origin = point(player);
        TpAura.Point destination = destination(origin, point(entity), plan.attackDistance());
        List<TpAura.Point> outward = module.buildPath(origin, destination, plan);
        List<TpAura.Point> returning = module.buildPath(destination, origin, plan);
        if (outward.isEmpty() || returning.isEmpty()
                || !pathIsLoaded(client.level, outward, returning, entity)
                || !pathIsCollisionFree(client.level, player, origin, outward)
                || !hasLineOfSight(client.level, player, destination, entity)) {
            return false;
        }

        sendPath(player, outward);
        boolean attacked = false;
        try {
            client.gameMode.attack(player, entity);
            tracker.recordAttack(plan.target());
            player.swing(InteractionHand.MAIN_HAND);
            attacked = true;
        } finally {
            sendPath(player, returning);
        }
        if (attacked) {
            module.markExecuted(tick);
        }
        return attacked;
    }

    private static TpAura.Point destination(TpAura.Point origin, TpAura.Point target, double attackDistance) {
        double x = target.x() - origin.x();
        double y = target.y() - origin.y();
        double z = target.z() - origin.z();
        double distance = Math.sqrt(x * x + y * y + z * z);
        if (distance < 1.0E-6D) {
            return origin;
        }
        double scale = Math.max(0.0D, distance - attackDistance) / distance;
        return new TpAura.Point(origin.x() + x * scale, origin.y() + y * scale, origin.z() + z * scale);
    }

    private static boolean pathIsLoaded(ClientLevel level, List<TpAura.Point> outward,
                                        List<TpAura.Point> returning, LivingEntity target) {
        if (!isLoaded(level, target.getX(), target.getZ())) {
            return false;
        }
        for (TpAura.Point point : outward) {
            if (!isLoaded(level, point.x(), point.z())) {
                return false;
            }
        }
        for (TpAura.Point point : returning) {
            if (!isLoaded(level, point.x(), point.z())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLoaded(ClientLevel level, double x, double z) {
        return level.hasChunk(Mth.floor(x) >> 4, Mth.floor(z) >> 4);
    }

    private static boolean pathIsCollisionFree(ClientLevel level, LocalPlayer player,
                                               TpAura.Point origin, List<TpAura.Point> path) {
        AABB originalBox = player.getBoundingBox();
        TpAura.Point previous = origin;
        for (TpAura.Point next : path) {
            Vec3 previousOffset = offset(origin, previous);
            Vec3 segment = offset(previous, next);
            AABB sweptBox = originalBox.move(previousOffset).expandTowards(segment);
            if (!level.noCollision(player, sweptBox)) {
                return false;
            }
            previous = next;
        }
        return true;
    }

    private static boolean hasLineOfSight(ClientLevel level, LocalPlayer player,
                                          TpAura.Point destination, LivingEntity target) {
        Vec3 from = new Vec3(destination.x(), destination.y() + player.getEyeHeight(), destination.z());
        return level.clip(new ClipContext(from, target.getEyePosition(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    private static void sendPath(LocalPlayer player, List<TpAura.Point> path) {
        for (TpAura.Point point : path) {
            player.connection.send(new ServerboundMovePlayerPacket.Pos(point.x(), point.y(), point.z(),
                    false, player.horizontalCollision));
        }
    }

    private static Vec3 offset(TpAura.Point from, TpAura.Point to) {
        return new Vec3(to.x() - from.x(), to.y() - from.y(), to.z() - from.z());
    }

    private static TpAura.Point point(Entity entity) {
        return new TpAura.Point(entity.getX(), entity.getY(), entity.getZ());
    }
}
