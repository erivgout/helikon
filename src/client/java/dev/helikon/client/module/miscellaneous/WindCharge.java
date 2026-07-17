package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Minecraft-free trigger and hotbar-ownership policy for ordinary wind-charge use. */
public final class WindCharge extends Module {
    public enum Trigger {
        FALLING,
        JUMP_KEY,
        FALLING_OR_JUMP
    }

    public enum ActionType {
        NONE,
        USE,
        SELECT_AND_USE,
        RESTORE
    }

    public record Action(ActionType type, int slot) {
        private static final Action NONE = new Action(ActionType.NONE, -1);

        public Action {
            Objects.requireNonNull(type, "type");
            if (type == ActionType.NONE && slot != -1) {
                throw new IllegalArgumentException("NONE actions cannot contain a slot");
            }
            if (type != ActionType.NONE && (slot < 0 || slot > 8)) {
                throw new IllegalArgumentException("Wind-charge actions require a hotbar slot");
            }
        }

        public static Action none() {
            return NONE;
        }
    }

    public record Context(boolean playerAvailable, boolean screenOpen, boolean usingItem, boolean falling,
                          boolean jumpHeld, double fallDistance, double pitch, boolean itemOnCooldown,
                          int currentSlot, int windChargeSlot) {
        public Context {
            if (!Double.isFinite(fallDistance) || fallDistance < 0.0D || !Double.isFinite(pitch)
                    || currentSlot < 0 || currentSlot > 8 || windChargeSlot < -1 || windChargeSlot > 8) {
                throw new IllegalArgumentException("Invalid wind-charge context");
            }
        }
    }

    private final EnumSetting<Trigger> trigger;
    private final NumberSetting fallDistance;
    private final NumberSetting minimumDownPitch;
    private final BooleanSetting selectFromHotbar;
    private final BooleanSetting restoreSlot;
    private final IntegerSetting cooldownTicks;
    private long nextUseTick = Long.MIN_VALUE;
    private int priorSlot = -1;
    private int selectedChargeSlot = -1;
    private boolean restoreRequested;

    public WindCharge() {
        super("wind_charge", "WindCharge",
                "Uses a player-provided hotbar wind charge under configured local conditions.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        trigger = addSetting(new EnumSetting<>(
                "trigger",
                "Trigger",
                "Use while falling, while Jump is held, or under either condition.",
                Trigger.class,
                Trigger.FALLING
        ));
        fallDistance = addSetting(new NumberSetting(
                "fall_distance",
                "Fall distance",
                "Minimum local fall distance before the falling trigger can use a charge.",
                4.0D,
                1.0D,
                20.0D,
                () -> trigger.value() != Trigger.JUMP_KEY
        ));
        minimumDownPitch = addSetting(new NumberSetting(
                "minimum_down_pitch",
                "Minimum down pitch",
                "Minimum downward view pitch required before using a wind charge.",
                60.0D,
                0.0D,
                90.0D
        ));
        selectFromHotbar = addSetting(new BooleanSetting(
                "select_from_hotbar",
                "Select from hotbar",
                "Temporarily select an existing wind charge from the hotbar when needed.",
                true
        ));
        restoreSlot = addSetting(new BooleanSetting(
                "restore_slot",
                "Restore slot",
                "Restore the prior slot after an automatic hotbar selection.",
                true,
                selectFromHotbar::value
        ));
        cooldownTicks = addSetting(new IntegerSetting(
                "cooldown_ticks",
                "Cooldown ticks",
                "Minimum client ticks between ordinary wind-charge uses.",
                10,
                1,
                40
        ));
    }

    /** Returns at most one hotbar selection, use, or restoration request for this client tick. */
    public Action update(long clientTick, Context context) {
        if (clientTick < 0L) {
            throw new IllegalArgumentException("clientTick must not be negative");
        }
        Context current = Objects.requireNonNull(context, "context");
        if (restoreRequested) {
            restoreRequested = false;
            return releaseOwnedSlot(current.currentSlot());
        }
        if (!isEnabled() || !current.playerAvailable() || current.screenOpen() || current.usingItem()
                || current.itemOnCooldown() || clientTick < nextUseTick || !triggerSatisfied(current)
                || current.pitch() < minimumDownPitch.value() || current.windChargeSlot() < 0) {
            return Action.none();
        }

        Action action;
        if (current.currentSlot() == current.windChargeSlot()) {
            action = new Action(ActionType.USE, current.currentSlot());
        } else if (selectFromHotbar.value()) {
            priorSlot = current.currentSlot();
            selectedChargeSlot = current.windChargeSlot();
            restoreRequested = true;
            action = new Action(ActionType.SELECT_AND_USE, selectedChargeSlot);
        } else {
            return Action.none();
        }

        long cooldown = cooldownTicks.value();
        nextUseTick = clientTick > Long.MAX_VALUE - cooldown ? Long.MAX_VALUE : clientTick + cooldown;
        return action;
    }

    /** Clears slot ownership when the local player/world no longer exists. */
    public void onPlayerUnavailable() {
        clearOwnership();
        nextUseTick = Long.MIN_VALUE;
    }

    @Override
    protected void onEnable() {
        nextUseTick = Long.MIN_VALUE;
    }

    @Override
    protected void onDisable() {
        if (priorSlot >= 0) {
            restoreRequested = true;
        } else {
            nextUseTick = Long.MIN_VALUE;
        }
    }

    private boolean triggerSatisfied(Context context) {
        boolean fallingTrigger = context.falling() && context.fallDistance() >= fallDistance.value();
        return switch (trigger.value()) {
            case FALLING -> fallingTrigger;
            case JUMP_KEY -> context.jumpHeld();
            case FALLING_OR_JUMP -> fallingTrigger || context.jumpHeld();
        };
    }

    private Action releaseOwnedSlot(int currentSlot) {
        if (!restoreSlot.value() || priorSlot < 0 || currentSlot != selectedChargeSlot) {
            clearOwnership();
            return Action.none();
        }
        int restore = priorSlot;
        clearOwnership();
        return new Action(ActionType.RESTORE, restore);
    }

    private void clearOwnership() {
        priorSlot = -1;
        selectedChargeSlot = -1;
        restoreRequested = false;
    }
}
