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

/** Narrow 26.2 adapter for validated movement around a target and one ordinary attack. */
public final class MinecraftTpAuraAccess {
    private static PendingReturn pendingReturn;

    private MinecraftTpAuraAccess() {
    }

    public static void install(TpAura module) {
        module.setTeleportRestorer(() -> stop(Minecraft.getInstance()));
    }

    public static boolean tick(long tick, TpAura module, MinecraftCombatAccess.Snapshot snapshot,
                               CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (pendingReturn != null) {
            finishPendingReturn(client, tick, false);
            return false;
        }
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || !snapshot.available()) {
            pendingReturn = null;
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
        TpAura.Point destination = module.orbitDestination(origin, point(entity), plan.attackDistance());
        List<TpAura.Point> outward = module.buildPath(origin, destination, plan);
        List<TpAura.Point> failureReturn = module.buildPath(destination, origin, plan);
        boolean movementRequired = distance(origin, destination) >= 1.0E-6D;
        boolean returns = module.returnsToOrigin();
        List<TpAura.Point> returning = returns ? failureReturn : List.of();
        if ((movementRequired && (outward.isEmpty() || failureReturn.isEmpty()))
                || !pathIsLoaded(client.level, outward, returning, entity)
                || (movementRequired && !pathIsCollisionFree(client.level, player, origin, outward))
                || !hasLineOfSight(client.level, player, destination, entity)) {
            return false;
        }

        if (movementRequired) {
            sendPath(player, outward);
            player.setPos(destination.x(), destination.y(), destination.z());
            if (returns) {
                pendingReturn = new PendingReturn(player, client.level, origin, returning,
                        tick + module.returnDelayTicks());
            }
        }
        boolean attacked = false;
        try {
            client.gameMode.attack(player, entity);
            tracker.recordAttack(plan.target());
            player.swing(InteractionHand.MAIN_HAND);
            attacked = true;
        } catch (RuntimeException exception) {
            if (pendingReturn != null) {
                finishPendingReturn(client, tick, true);
            } else if (movementRequired) {
                sendPath(player, failureReturn);
                player.setPos(origin.x(), origin.y(), origin.z());
            }
            throw exception;
        }
        if (attacked) {
            module.markExecuted(tick);
        }
        return attacked;
    }

    /** Immediately restores an in-progress visible teleport when the module is disabled. */
    public static void stop(Minecraft client) {
        finishPendingReturn(client, Long.MAX_VALUE, true);
    }

    private static void finishPendingReturn(Minecraft client, long tick, boolean force) {
        PendingReturn pending = pendingReturn;
        if (pending == null) {
            return;
        }
        if (client.player != pending.player() || client.level != pending.level()
                || pending.player().connection == null) {
            pendingReturn = null;
            return;
        }
        if (!force && tick < pending.returnTick()) {
            return;
        }
        sendPath(pending.player(), pending.path());
        pending.player().setPos(pending.origin().x(), pending.origin().y(), pending.origin().z());
        pendingReturn = null;
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

    private static double distance(TpAura.Point from, TpAura.Point to) {
        return Math.sqrt(Math.pow(to.x() - from.x(), 2.0D)
                + Math.pow(to.y() - from.y(), 2.0D)
                + Math.pow(to.z() - from.z(), 2.0D));
    }

    private static TpAura.Point point(Entity entity) {
        return new TpAura.Point(entity.getX(), entity.getY(), entity.getZ());
    }

    private record PendingReturn(LocalPlayer player, ClientLevel level, TpAura.Point origin,
                                 List<TpAura.Point> path, long returnTick) {
    }
}
