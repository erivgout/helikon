package dev.helikon.client.module.combat;

import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.render.ProjectileThreatPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Narrow Minecraft adapter for projectile observation, collision checks, and local movement. */
public final class MinecraftEndermanAuraAccess {
    private MinecraftEndermanAuraAccess() {
    }

    public static void tick(long tick, EndermanAura module, FriendManager friends) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null
                || dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen())
                || player.isPassenger()) {
            module.onContextLost();
            return;
        }
        List<EndermanAura.Threat> threats = threats(level, player, module, friends);
        if (threats.isEmpty()) {
            return;
        }
        List<EndermanAura.Destination> destinations = destinations(level, player, module, threats.getFirst());
        Optional<EndermanAura.EscapePlan> plan = module.choose(tick, threats, destinations);
        if (plan.isEmpty()) {
            return;
        }
        EndermanAura.Destination destination = plan.orElseThrow().destination();
        player.setPos(destination.x(), destination.y(), destination.z());
        if (module.cancelVelocity()) {
            player.setDeltaMovement(Vec3.ZERO);
        }
        module.markTeleported(tick);
    }

    private static List<EndermanAura.Threat> threats(ClientLevel level, LocalPlayer player, EndermanAura module,
                                                       FriendManager friends) {
        List<EndermanAura.Threat> result = new ArrayList<>();
        Vec3 center = player.getBoundingBox().getCenter();
        Vec3 playerVelocity = player.getDeltaMovement();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Projectile projectile) || entity.isRemoved()) {
                continue;
            }
            Entity owner = projectile.getOwner();
            boolean selfOwned = owner == player;
            boolean friendOwned = owner instanceof Player ownerPlayer
                    && friends.contains(ownerPlayer.getGameProfile().name());
            Vec3 relative = projectile.position().subtract(center);
            Vec3 projectileVelocity = projectile.getDeltaMovement();
            Optional<ProjectileThreatPolicy.ProjectileThreat> assessed =
                    projectile instanceof AbstractArrow
                            ? ProjectileThreatPolicy.assessBallistic(
                                    relative.x, relative.y, relative.z,
                                    projectileVelocity.x, projectileVelocity.y, projectileVelocity.z,
                                    playerVelocity.x, playerVelocity.y, playerVelocity.z,
                                    module.hitRadius(), module.warningTicks(), module.detectionRange(),
                                    0.05D, 0.99D)
                            : ProjectileThreatPolicy.assess(relative.x, relative.y, relative.z,
                                    projectileVelocity.x - playerVelocity.x,
                                    projectileVelocity.y - playerVelocity.y,
                                    projectileVelocity.z - playerVelocity.z,
                                    module.hitRadius(), module.warningTicks(), module.detectionRange());
            assessed
                    .ifPresent(threat -> result.add(new EndermanAura.Threat(projectile.getId(),
                            selfOwned, friendOwned, threat.timeToImpactTicks(),
                            threat.closestApproach(), threat.distance())));
        }
        result.sort(Comparator.comparingDouble(EndermanAura.Threat::timeToImpactTicks)
                .thenComparingDouble(EndermanAura.Threat::closestApproach)
                .thenComparingInt(EndermanAura.Threat::entityId));
        return List.copyOf(result);
    }

    private static List<EndermanAura.Destination> destinations(ClientLevel level, LocalPlayer player,
                                                                EndermanAura module,
                                                                EndermanAura.Threat threat) {
        Entity projectile = level.getEntity(threat.entityId());
        if (projectile == null) {
            return List.of();
        }
        Vec3 projectileVelocity = projectile.getDeltaMovement();
        List<EndermanAura.Destination> result = new ArrayList<>(module.escapeDistances().size() * 2);
        AABB original = player.getBoundingBox();
        for (double distance : module.escapeDistances()) {
            List<EndermanAura.SidewaysOffset> offsets = EndermanAura.sidewaysOffsets(
                    projectileVelocity.x, projectileVelocity.z,
                    projectile.getX() - player.getX(), projectile.getZ() - player.getZ(), distance);
            for (EndermanAura.SidewaysOffset offset : offsets) {
                double x = player.getX() + offset.x();
                double z = player.getZ() + offset.z();
                double y = player.getY();
                boolean loaded = level.hasChunk(Mth.floor(x) >> 4, Mth.floor(z) >> 4);
                AABB moved = original.move(x - player.getX(), 0.0D, z - player.getZ());
                boolean collisionFree = loaded && level.noCollision(player, moved);
                AABB floorProbe = moved.move(0.0D, -0.08D, 0.0D);
                boolean safeFloor = collisionFree && !level.noCollision(player, floorProbe);
                double projectileSeparation = projectile.position().distanceTo(new Vec3(x, y, z));
                result.add(new EndermanAura.Destination(x, y, z, distance, projectileSeparation,
                        loaded, collisionFree, safeFloor));
            }
        }
        return List.copyOf(result);
    }
}
