package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.movement.MovementInput;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/**
 * Briefly releases the local forward key just after an eligible melee attack so
 * vanilla sprint restarts, which the server rewards with a fresh sprint-hit
 * knockback. The decision is Minecraft-free; a narrow adapter feeds it observed
 * attack facts and applies the resulting forward release through ordinary input.
 */
public final class WTap extends Module {
    /** Minecraft-free facts about one observed local melee attack. */
    public record AttackContext(CombatEntityType type, boolean friend, boolean sprinting, boolean forward) {
        public AttackContext {
            Objects.requireNonNull(type, "type");
        }
    }

    private final NumberSetting releaseTicks;
    private final BooleanSetting requireSprint;
    private final BooleanSetting requireForward;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private int remainingReleaseTicks;

    public WTap() {
        super("wtap", "WTap",
                "Releases forward briefly after an attack to reset sprint for a stronger knockback hit.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        releaseTicks = addSetting(new NumberSetting("release_ticks", "Release ticks",
                "Client ticks to hold the forward key released after an eligible attack.", 1.0D, 1.0D, 5.0D));
        requireSprint = addSetting(new BooleanSetting("require_sprint", "Require sprint",
                "Only reset when the local player was sprinting during the attack.", true));
        requireForward = addSetting(new BooleanSetting("require_forward", "Require forward",
                "Only reset when the local forward key was held during the attack.", true));
        players = addSetting(new BooleanSetting("players", "Players", "React to attacks on non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "React to attacks on hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "React to attacks on passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never react to attacks on locally listed friends.", true));
    }

    /** Starts a bounded forward-release pulse when an observed attack passes the local filters. */
    public void onAttack(AttackContext context) {
        AttackContext current = Objects.requireNonNull(context, "context");
        if (!isEnabled() || !shouldReset(current)) {
            return;
        }
        remainingReleaseTicks = (int) Math.round(releaseTicks.value());
    }

    /**
     * Returns the movement input with forward (and sprint) released while an
     * active pulse remains. It never presses a key the player did not hold; it
     * only releases the existing forward input for the configured tick count.
     */
    public MovementInput apply(MovementInput input, boolean screenOpen) {
        MovementInput current = Objects.requireNonNull(input, "input");
        if (!isEnabled() || screenOpen || remainingReleaseTicks <= 0) {
            return current;
        }
        remainingReleaseTicks--;
        if (!current.forward()) {
            return current;
        }
        return new MovementInput(false, current.backward(), current.left(), current.right(),
                current.jump(), current.shift(), false);
    }

    /** Clears any pulse when no local player exists, avoiding cross-world carryover. */
    public void onPlayerUnavailable() {
        remainingReleaseTicks = 0;
    }

    private boolean shouldReset(AttackContext context) {
        if (context.friend() && excludeFriends.value()) {
            return false;
        }
        if (requireSprint.value() && !context.sprinting()) {
            return false;
        }
        if (requireForward.value() && !context.forward()) {
            return false;
        }
        return switch (context.type()) {
            case PLAYER -> players.value();
            case HOSTILE -> hostiles.value();
            case PASSIVE -> passive.value();
        };
    }

    @Override
    protected void onDisable() {
        remainingReleaseTicks = 0;
    }
}
