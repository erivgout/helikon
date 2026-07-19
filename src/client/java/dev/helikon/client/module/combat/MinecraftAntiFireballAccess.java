package dev.helikon.client.module.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Narrow 26.2 bridge that observes local fireballs and issues one ordinary attack the module selects. */
public final class MinecraftAntiFireballAccess {
    private MinecraftAntiFireballAccess() {
    }

    /**
     * Observes currently rendered fireballs, lets the module choose at most one, then issues a single
     * ordinary local attack (and swing). Returns true when an attack request was sent this tick so the
     * shared per-tick combat guard can prevent a second Helikon attack.
     */
    public static boolean tick(long tick, AntiFireball module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gameMode == null
                || dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen())) {
            return false;
        }
        LocalPlayer player = client.player;
        Vec3 eye = player.getEyePosition();
        List<AntiFireball.FireballObservation> observations = new ArrayList<>();
        Map<String, Entity> entities = new HashMap<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            AntiFireball.FireballKind kind;
            if (entity instanceof LargeFireball) {
                kind = AntiFireball.FireballKind.GHAST;
            } else if (entity instanceof SmallFireball) {
                kind = AntiFireball.FireballKind.BLAZE;
            } else {
                continue;
            }
            double dx = entity.getX() - eye.x;
            double dy = entity.getY() + entity.getBbHeight() * 0.5D - eye.y;
            double dz = entity.getZ() - eye.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            Vec3 velocity = entity.getDeltaMovement();
            // Velocity pointing toward the player: dot(velocity, playerEye - fireballPos) > 0.
            boolean approaching = velocity.x * -dx + velocity.y * -dy + velocity.z * -dz > 0.0D;
            String id = entity.getUUID().toString();
            observations.add(new AntiFireball.FireballObservation(id, kind, distance, approaching,
                    player.hasLineOfSight(entity)));
            entities.put(id, entity);
        }
        Optional<String> selected = module.selectTarget(tick, observations);
        if (selected.isEmpty()) {
            return false;
        }
        Entity target = entities.get(selected.get());
        if (target == null || target.isRemoved() || !target.isAlive() || !player.hasLineOfSight(target)) {
            return false;
        }
        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
        return true;
    }
}
