package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatTargetTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Loaded-world, collision-checked adapter for the Anime Aura state machine. */
public final class MinecraftAnimeAuraAccess {
    private Vec3 origin;

    public boolean tick(long tick, AnimeAura module, MinecraftCombatAccess.Snapshot snapshot,
                        CombatTargetTracker tracker, boolean canAttack) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || !snapshot.available()
                || client.gui.screen() != null || player.connection == null) {
            recover(player);
            module.reset();
            return false;
        }
        if (!module.isEnabled()) {
            recover(player);
            return false;
        }
        Optional<AnimeAura.Action> next = module.next(tick,
                canAttack && player.getAttackStrengthScale(0.0F) >= 0.9F, snapshot.targets());
        if (next.isEmpty()) {
            return false;
        }
        AnimeAura.Action action = next.orElseThrow();
        LivingEntity target = snapshot.entities().get(action.target().id());
        if (target == null || !target.isAlive() || target.isRemoved()) {
            recover(player);
            module.reset();
            return false;
        }
        if (origin == null) {
            origin = player.position();
        }
        if (action.stage() == AnimeAura.Stage.RECOVERY) {
            move(player, origin);
            origin = null;
            module.markSuccessful(tick, action.stage());
            return false;
        }
        Vec3 destination = destination(action, target);
        if (destination.y - origin.y > action.safetyHeight()
                || !client.level.hasChunk(((int) Math.floor(destination.x)) >> 4,
                ((int) Math.floor(destination.z)) >> 4)
                || !client.level.noCollision(player, movedBox(player.getBoundingBox(), player.position(), destination))) {
            recover(player);
            module.reset();
            return false;
        }
        move(player, destination);
        boolean attacked = false;
        if (action.attack()) {
            client.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
            tracker.recordAttack(action.target());
            attacked = true;
        }
        module.markSuccessful(tick, action.stage());
        return attacked;
    }

    private static Vec3 destination(AnimeAura.Action action, LivingEntity target) {
        double angle = action.comboIndex() * Math.PI / 2.0;
        double x = target.getX() + Math.cos(angle) * action.orbitRadius();
        double z = target.getZ() + Math.sin(angle) * action.orbitRadius();
        double y = switch (action.stage()) {
            case APPROACH -> target.getY();
            case LAUNCHER -> target.getY() - 0.5;
            case COMBO -> target.getY() + 0.5 + (action.comboIndex() % 2);
            case FINISHER -> target.getY() + 2.0;
            default -> target.getY();
        };
        return new Vec3(x, y, z);
    }

    private static AABB movedBox(AABB box, Vec3 from, Vec3 to) {
        return box.move(to.x - from.x, to.y - from.y, to.z - from.z);
    }

    private static void move(LocalPlayer player, Vec3 position) {
        player.setPos(position.x, position.y, position.z);
        player.connection.send(new ServerboundMovePlayerPacket.Pos(position.x, position.y, position.z,
                false, player.horizontalCollision));
    }

    private void recover(LocalPlayer player) {
        if (origin != null && player != null && player.connection != null) {
            move(player, origin);
        }
        origin = null;
    }
}
