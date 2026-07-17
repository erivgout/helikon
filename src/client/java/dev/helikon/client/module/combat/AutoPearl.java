package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;

/**
 * Selects a fleeing local combat target and asks the normal held-item path to throw one player-owned
 * ender pearl toward it. The decision is Minecraft-free; the server still validates the ordinary throw.
 */
public final class AutoPearl extends Module {
    public enum ActionType {
        NONE,
        THROW_SELECTED,
        SELECT_AND_THROW,
        RESTORE_SLOT
    }

    /** Minecraft-free local facts for one AutoPearl tick. */
    public record Context(int selectedSlot, int pearlSlot, boolean screenOpen, boolean pearlOnCooldown,
                          List<CombatTarget> candidates) {
        public Context {
            if (selectedSlot < 0 || selectedSlot > 8 || pearlSlot < -1 || pearlSlot > 8 || candidates == null) {
                throw new IllegalArgumentException("auto-pearl context is invalid");
            }
            candidates = List.copyOf(candidates);
        }
    }

    /** Bounded local outcome; rotation is applied only when {@code rotate} is set. */
    public record Action(ActionType type, int slot, float yaw, float pitch, boolean rotate) {
        private static final Action NONE = new Action(ActionType.NONE, -1, 0.0F, 0.0F, false);

        public Action {
            if (type == null || (type != ActionType.NONE && (slot < 0 || slot > 8)) || !Float.isFinite(yaw)
                    || !Float.isFinite(pitch) || pitch < -90.0F || pitch > 90.0F) {
                throw new IllegalArgumentException("auto-pearl action is invalid");
            }
        }

        public static Action none() {
            return NONE;
        }
    }

    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final NumberSetting range;
    private final NumberSetting activationDistance;
    private final BooleanSetting requireMovingAway;
    private final NumberSetting throwDelayTicks;
    private final BooleanSetting rotate;
    private final BooleanSetting restoreSlot;
    private final NumberSetting projectileSpeed;
    private final NumberSetting gravity;
    private int originalSlot = -1;
    private int pearlSelectedSlot = -1;
    private long lastThrowTick = -1L;

    public AutoPearl() {
        super("auto_pearl", "AutoPearl", "Throws player-owned ender pearls toward fleeing targets through the normal held-item path.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Allow non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Allow hostile mobs.", false));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Allow passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never chase locally listed friends.", true));
        range = addSetting(new NumberSetting("range", "Range", "Maximum locally considered target distance.",
                40.0D, 8.0D, 96.0D));
        activationDistance = addSetting(new NumberSetting("activation_distance", "Activation distance",
                "Only throw once the target is at least this many blocks away.", 12.0D, 4.0D, 64.0D));
        requireMovingAway = addSetting(new BooleanSetting("require_moving_away", "Require moving away",
                "Only throw when the target's velocity carries it away from you.", true));
        throwDelayTicks = addSetting(new NumberSetting("throw_delay_ticks", "Throw delay",
                "Minimum ticks between normal ender-pearl throws.", 40.0D, 20.0D, 200.0D));
        rotate = addSetting(new BooleanSetting("rotate", "Rotate",
                "Snap the local view toward the predicted landing before throwing.", true));
        restoreSlot = addSetting(new BooleanSetting("restore_slot", "Restore slot",
                "Restore the previously selected hotbar slot after a throw.", true));
        projectileSpeed = addSetting(new NumberSetting("projectile_speed", "Projectile speed",
                "Conservative local ender-pearl speed estimate in blocks per tick.", 1.5D, 0.5D, 3.0D));
        gravity = addSetting(new NumberSetting("gravity", "Gravity",
                "Conservative local ender-pearl gravity estimate per tick.", 0.03D, 0.0D, 0.20D));
    }

    public Action update(long tick, Context context) {
        if (tick < 0L || context == null) {
            throw new IllegalArgumentException("auto-pearl inputs are invalid");
        }
        if (pearlSelectedSlot >= 0) {
            return releaseOwnedSlot(context.selectedSlot());
        }
        if (!isEnabled() || context.screenOpen() || context.pearlSlot() < 0 || context.pearlOnCooldown()
                || (lastThrowTick >= 0L && tick - lastThrowTick < Math.round(throwDelayTicks.value()))) {
            return Action.none();
        }
        CombatTarget target = selectTarget(context.candidates());
        if (target == null) {
            return Action.none();
        }
        CombatAim.Rotation aim = CombatAim.predictedRotation(target, projectileSpeed.value(), gravity.value(), true);
        originalSlot = context.selectedSlot();
        pearlSelectedSlot = context.pearlSlot();
        lastThrowTick = tick;
        ActionType type = context.pearlSlot() == context.selectedSlot()
                ? ActionType.THROW_SELECTED
                : ActionType.SELECT_AND_THROW;
        return new Action(type, context.pearlSlot(), aim.yaw(), aim.pitch(), rotate.value());
    }

    public void onPlayerUnavailable() {
        originalSlot = -1;
        pearlSelectedSlot = -1;
        lastThrowTick = -1L;
    }

    private CombatTarget selectTarget(List<CombatTarget> candidates) {
        return CombatTargetFilter.ordered(candidates, targetOptions(), CombatTargetFilter.Priority.DISTANCE).stream()
                .filter(target -> target.distance() >= activationDistance.value())
                .filter(target -> !requireMovingAway.value() || isMovingAway(target))
                .findFirst()
                .orElse(null);
    }

    private static boolean isMovingAway(CombatTarget target) {
        return target.relativeX() * target.velocityX() + target.relativeZ() * target.velocityZ() > 1.0e-4D;
    }

    private Action releaseOwnedSlot(int currentSlot) {
        int restore = originalSlot;
        int throwSlot = pearlSelectedSlot;
        originalSlot = -1;
        pearlSelectedSlot = -1;
        boolean owns = restoreSlot.value() && restore >= 0 && restore != throwSlot && currentSlot == throwSlot;
        return owns ? new Action(ActionType.RESTORE_SLOT, restore, 0.0F, 0.0F, false) : Action.none();
    }

    private CombatTargetFilter.Options targetOptions() {
        return new CombatTargetFilter.Options(players.value(), hostiles.value(), passive.value(), excludeFriends.value(),
                true, range.value(), 180.0D, false);
    }

    @Override
    protected void onDisable() {
        originalSlot = -1;
        pearlSelectedSlot = -1;
        lastThrowTick = -1L;
    }
}
