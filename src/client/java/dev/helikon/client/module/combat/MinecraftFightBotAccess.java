package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Narrow 26.2 adapter for FightBot's local movement, aim, and ordinary attack decisions. */
public final class MinecraftFightBotAccess {
    private MinecraftFightBotAccess() {
    }

    /**
     * Applies local approach movement and returns true only when this callback starts an ordinary
     * Minecraft attack.
     */
    public static boolean tick(long tick, FightBot module, MinecraftCombatAccess.Snapshot snapshot,
                               CombatTargetTracker tracker, boolean ordinaryAttackAvailable) {
        Minecraft client = Minecraft.getInstance();
        if (!snapshot.available() || client.player == null || client.level == null) {
            module.resetTransientState();
            return false;
        }

        LocalPlayer player = client.player;
        boolean attackReady = ordinaryAttackAvailable && client.gameMode != null
                && player.getAttackStrengthScale(0.0F) >= 0.9F;
        FightBot.Context context = new FightBot.Context(
                dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen()), player.isPassenger(),
                player.getAbilities().flying, player.isFallFlying(), attackReady, snapshot.targets());
        Optional<FightBot.Action> selected = module.update(tick, context);
        if (selected.isEmpty()) {
            return false;
        }

        FightBot.Action action = selected.get();
        CombatTarget targetFacts = action.target();
        LivingEntity target = snapshot.entities().get(targetFacts.id());
        if (target == null || target.isRemoved() || !target.isAlive() || !player.hasLineOfSight(target)) {
            return false;
        }

        CombatAim.Rotation rotation = module.rotateToward(targetFacts,
                new CombatAim.Rotation(player.getYRot(), player.getXRot()));
        player.setYRot(rotation.yaw());
        player.setXRot(rotation.pitch());
        if (action.move()) {
            Vec3 current = player.getDeltaMovement();
            player.setDeltaMovement(action.velocityX(), current.y, action.velocityZ());
        }
        if (!action.attack() || client.gameMode == null) {
            return false;
        }

        client.gameMode.attack(player, target);
        tracker.recordAttack(targetFacts);
        player.swing(InteractionHand.MAIN_HAND);
        return true;
    }
}
