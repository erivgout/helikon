package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetTracker;
import dev.helikon.client.friend.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Narrow 26.2 adapter for following one local friend and normally attacking threats. */
public final class MinecraftProtectAccess {
    private MinecraftProtectAccess() {
    }

    /**
     * Applies local following and returns true only if this callback starts one ordinary attack.
     */
    public static boolean tick(long tick, Protect module, MinecraftCombatAccess.Snapshot snapshot,
                               CombatTargetTracker tracker, FriendManager friends, boolean ordinaryAttackAvailable) {
        Minecraft client = Minecraft.getInstance();
        if (!snapshot.available() || client.player == null || client.level == null) {
            module.resetTransientState();
            return false;
        }

        LocalPlayer player = client.player;
        Map<String, LivingEntity> entities = new LinkedHashMap<>(snapshot.entities());
        List<CombatTarget> targets = new ArrayList<>(snapshot.targets());
        addFilteredFriends(client, player, friends, targets, entities);
        boolean attackReady = ordinaryAttackAvailable && client.gameMode != null
                && player.getAttackStrengthScale(0.0F) >= 0.9F;
        Protect.Context context = new Protect.Context(
                dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen()), player.isPassenger(),
                player.getAbilities().flying, player.isFallFlying(), attackReady, targets);
        Optional<Protect.Action> selected = module.update(tick, context);
        if (selected.isEmpty()) {
            return false;
        }

        Protect.Action action = selected.get();
        LivingEntity friend = entities.get(action.protectedFriend().id());
        if (friend == null || friend.isRemoved() || !friend.isAlive()) {
            return false;
        }
        if (action.move()) {
            Vec3 current = player.getDeltaMovement();
            player.setDeltaMovement(action.velocityX(), current.y, action.velocityZ());
        }
        if (action.threat().isEmpty()) {
            return false;
        }

        CombatTarget threatFacts = action.threat().orElseThrow();
        LivingEntity threat = entities.get(threatFacts.id());
        if (threat == null || threat.isRemoved() || !threat.isAlive() || !player.hasLineOfSight(threat)) {
            return false;
        }
        CombatAim.Rotation rotation = module.rotateToward(threatFacts,
                new CombatAim.Rotation(player.getYRot(), player.getXRot()));
        player.setYRot(rotation.yaw());
        player.setXRot(rotation.pitch());
        if (!action.attack() || client.gameMode == null) {
            return false;
        }

        client.gameMode.attack(player, threat);
        tracker.recordAttack(threatFacts);
        player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private static void addFilteredFriends(Minecraft client, LocalPlayer player, FriendManager friends,
                                           List<CombatTarget> targets, Map<String, LivingEntity> entities) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player friend) || entity == player || entity.isRemoved()) {
                continue;
            }
            String name = friend.getGameProfile().name();
            if (name == null || !friends.contains(name)) {
                continue;
            }
            String id = friend.getUUID().toString();
            entities.putIfAbsent(id, friend);
            if (targets.stream().noneMatch(target -> target.id().equals(id))) {
                targets.add(friendTarget(player, friend, id, name));
            }
        }
    }

    private static CombatTarget friendTarget(LocalPlayer player, Player friend, String id, String name) {
        Vec3 eye = player.getEyePosition();
        double x = friend.getX() - eye.x;
        double y = friend.getY() + friend.getBbHeight() * 0.5D - eye.y;
        double z = friend.getZ() - eye.z;
        double distance = Math.sqrt(x * x + y * y + z * z);
        Vec3 velocity = friend.getDeltaMovement();
        return new CombatTarget(id, name, CombatEntityType.PLAYER, true, false, friend.isAlive(),
                !friend.isInvisible(), player.hasLineOfSight(friend), distance, 0.0D, x, y, z,
                velocity.x, velocity.y, velocity.z, friend.getHealth(), friend.getArmorValue(),
                "unknown", List.of());
    }
}
