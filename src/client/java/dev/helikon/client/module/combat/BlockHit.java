package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Objects;

/**
 * Holds a normal local Use to raise a held shield while an eligible combat
 * target is in range, then briefly lowers it so ordinary attacks land. The
 * decision is Minecraft-free; the connected server remains authoritative and
 * may still take the shield-blocked or unblocked hit that it validates.
 */
public final class BlockHit extends Module {
    /** Local facts sampled by the combat adapter; contains no Minecraft objects. */
    public record Context(boolean shieldReady, boolean attackHeld, boolean attackReady, boolean screenOpen,
                          List<CombatTarget> targets) {
        public Context {
            targets = targets == null ? List.of() : List.copyOf(targets);
        }
    }

    /** Requested normal local Use-key ownership change for this tick. */
    public record Action(boolean holdBlock, boolean releaseBlock) {
        private static final Action NONE = new Action(false, false);

        public static Action none() {
            return NONE;
        }
    }

    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final NumberSetting range;
    private final BooleanSetting requireAttackKey;
    private final BooleanSetting unblockOnAttack;
    private final NumberSetting unblockTicks;
    private final UseKeyAccess useKey;
    private boolean blockHeldByModule;
    private long unblockUntilTick = -1L;

    public BlockHit(UseKeyAccess useKey) {
        super("block_hit", "BlockHit",
                "Automatically raises a held shield during combat and briefly lowers it so normal attacks land.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        this.useKey = Objects.requireNonNull(useKey, "useKey");
        players = addSetting(new BooleanSetting("players", "Players", "Treat non-friend players as combat threats.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Treat hostile mobs as combat threats.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Treat passive mobs as combat threats.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never treat locally listed friends as combat threats.", true));
        range = addSetting(new NumberSetting("range", "Range",
                "Maximum distance of a combat threat that makes blocking advantageous.", 4.0D, 1.0D, 6.0D));
        requireAttackKey = addSetting(new BooleanSetting("require_attack_key", "Require attack held",
                "Only raise the shield while you are actively holding the attack key.", true));
        unblockOnAttack = addSetting(new BooleanSetting("unblock_on_attack", "Unblock to attack",
                "Briefly lower the shield when a full-strength attack is ready so the swing lands.", true));
        unblockTicks = addSetting(new NumberSetting("unblock_ticks", "Unblock ticks",
                "Ticks the shield stays lowered around a ready attack before it is raised again.", 2.0D, 1.0D, 10.0D));
    }

    /** Applies the validated decision through the narrow local Use-key port. */
    public Action tick(long tick, Context context) {
        Action action = update(tick, context);
        if (action.releaseBlock() && !useKey.isPhysicallyDown()) {
            useKey.setDown(false);
        }
        if (action.holdBlock()) {
            useKey.setDown(true);
        }
        return action;
    }

    /** Pure decision: raise, keep, or lower a module-held shield block from local facts. */
    public Action update(long tick, Context context) {
        if (tick < 0L || context == null) {
            throw new IllegalArgumentException("BlockHit inputs are invalid");
        }
        boolean advantageous = isEnabled() && context.shieldReady() && !context.screenOpen()
                && (!requireAttackKey.value() || context.attackHeld()) && threatInRange(context.targets());
        if (!advantageous) {
            unblockUntilTick = -1L;
            return dropBlock();
        }
        if (unblockOnAttack.value()) {
            if (context.attackHeld() && context.attackReady()) {
                unblockUntilTick = tick + Math.round(unblockTicks.value());
            }
            if (tick < unblockUntilTick) {
                return dropBlock();
            }
        }
        return raiseBlock();
    }

    /** Releases only module-owned Use input when the local player disappears. */
    public Action onPlayerUnavailable() {
        unblockUntilTick = -1L;
        boolean releaseBlock = blockHeldByModule;
        if (releaseBlock && !useKey.isPhysicallyDown()) {
            useKey.setDown(false);
        }
        blockHeldByModule = false;
        return releaseBlock ? new Action(false, true) : Action.none();
    }

    @Override
    protected void onDisable() {
        if (blockHeldByModule && !useKey.isPhysicallyDown()) {
            useKey.setDown(false);
        }
        blockHeldByModule = false;
        unblockUntilTick = -1L;
    }

    private boolean threatInRange(List<CombatTarget> targets) {
        CombatTargetFilter.Options options = new CombatTargetFilter.Options(players.value(), hostiles.value(),
                passive.value(), excludeFriends.value(), true, range.value(), 180.0D, false);
        return targets.stream().anyMatch(target -> CombatTargetFilter.allows(target, options));
    }

    private Action raiseBlock() {
        if (blockHeldByModule) {
            return Action.none();
        }
        blockHeldByModule = true;
        return new Action(true, false);
    }

    private Action dropBlock() {
        boolean releaseBlock = blockHeldByModule;
        blockHeldByModule = false;
        return releaseBlock ? new Action(false, true) : Action.none();
    }

    /** Narrow Minecraft-free port for normal local Use-key ownership. */
    public interface UseKeyAccess {
        boolean isPhysicallyDown();

        void setDown(boolean value);
    }
}
