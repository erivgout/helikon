package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.module.movement.MovementInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

/** Narrow 26.2 bridge feeding WTap observed attack facts and applying its forward release. */
public final class WTapAccess {
    private static volatile WTap module;
    private static volatile FriendManager friends;

    private WTapAccess() {
    }

    public static void install(WTap wtap, FriendManager friendManager) {
        module = Objects.requireNonNull(wtap, "wtap");
        friends = Objects.requireNonNull(friendManager, "friendManager");
    }

    /**
     * Reports one initiated local melee attack. Reads only already-loaded local
     * player and target facts; it never changes the attack, target, or result.
     */
    public static void observeAttack(Player attacker, Entity target) {
        WTap current = module;
        if (current == null || !(attacker instanceof LocalPlayer local) || !(target instanceof LivingEntity living)) {
            return;
        }
        CombatEntityType type = categoryOf(living);
        boolean friend = isFriend(living);
        boolean forward = local.input != null && local.input.keyPresses.forward();
        current.onAttack(new WTap.AttackContext(type, friend, local.isSprinting(), forward));
    }

    /** Returns the input record with WTap's forward release applied, or the original when unchanged. */
    public static Input apply(Input input, boolean screenOpen) {
        Input current = Objects.requireNonNull(input, "input");
        WTap active = module;
        if (active == null) {
            return current;
        }
        MovementInput source = new MovementInput(current.forward(), current.backward(), current.left(),
                current.right(), current.jump(), current.shift(), current.sprint());
        MovementInput result = active.apply(source, screenOpen);
        if (result.forward() == source.forward() && result.sprint() == source.sprint()) {
            return current;
        }
        return new Input(result.forward(), result.backward(), result.left(), result.right(),
                result.jump(), result.shift(), result.sprint());
    }

    public static void onPlayerUnavailable() {
        WTap current = module;
        if (current != null) {
            current.onPlayerUnavailable();
        }
    }

    private static CombatEntityType categoryOf(LivingEntity entity) {
        if (entity instanceof Player) {
            return CombatEntityType.PLAYER;
        }
        if (entity instanceof Monster) {
            return CombatEntityType.HOSTILE;
        }
        return CombatEntityType.PASSIVE;
    }

    private static boolean isFriend(LivingEntity entity) {
        FriendManager manager = friends;
        if (manager == null || !(entity instanceof Player player)) {
            return false;
        }
        String name = player.getGameProfile().name();
        return name != null && !name.isBlank() && manager.contains(name.trim());
    }
}
