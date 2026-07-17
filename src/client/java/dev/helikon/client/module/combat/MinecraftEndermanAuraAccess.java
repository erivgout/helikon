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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Narrow Minecraft adapter for projectile observation, collision checks, and local movement. */
public final class MinecraftEndermanAuraAccess {
    private static final int DESTINATION_DIRECTIONS = 16;

    private MinecraftEndermanAuraAccess() {
    }

    public static void tick(long tick, EndermanAura module, FriendManager friends) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null || client.gui.screen() != null || player.isPassenger()) {
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
            Vec3 velocity = projectile.getDeltaMovement().subtract(playerVelocity);
            ProjectileThreatPolicy.assess(relative.x, relative.y, relative.z,
                            velocity.x, velocity.y, velocity.z, module.hitRadius(),
                            module.warningTicks(), module.detectionRange())
                    .ifPresent(threat -> result.add(new EndermanAura.Threat(projectile.getId(),
                            selfOwned, friendOwned, threat.timeToImpactTicks(),
                            threat.closestApproach(), threat.distance())));
        }
        return List.copyOf(result);
    }

    private static List<EndermanAura.Destination> destinations(ClientLevel level, LocalPlayer player,
                                                                EndermanAura module,
                                                                EndermanAura.Threat threat) {
        Entity projectile = level.getEntity(threat.entityId());
        if (projectile == null) {
            return List.of();
        }
        List<EndermanAura.Destination> result = new ArrayList<>(DESTINATION_DIRECTIONS);
        double distance = module.teleportDistance();
        AABB original = player.getBoundingBox();
        for (int index = 0; index < DESTINATION_DIRECTIONS; index++) {
            double angle = Math.PI * 2.0D * index / DESTINATION_DIRECTIONS;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
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
        return List.copyOf(result);
    }
}
