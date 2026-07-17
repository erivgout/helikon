package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.Comparator;
import java.util.List;

/** Uses a player-owned hotbar mushroom stew through Minecraft's ordinary held-item path. */
public final class AutoSoup extends Module {
    public enum ActionType {
        NONE,
        SELECT_AND_USE,
        USE_SELECTED,
        RESTORE_SLOT
    }

    public record Context(int selectedSlot, double health, boolean screenOpen, boolean usingItem, List<Integer> soupSlots) {
        public Context {
            if (selectedSlot < 0 || selectedSlot > 8 || !Double.isFinite(health) || health < 0.0D || soupSlots == null
                    || soupSlots.stream().anyMatch(slot -> slot == null || slot < 0 || slot > 8)) {
                throw new IllegalArgumentException("auto-soup context is invalid");
            }
            soupSlots = List.copyOf(soupSlots);
        }
    }

    public record Action(ActionType type, int slot) {
        private static final Action NONE = new Action(ActionType.NONE, -1);

        public Action {
            if (type == null || (type != ActionType.NONE && (slot < 0 || slot > 8))) {
                throw new IllegalArgumentException("auto-soup action is invalid");
            }
        }

        public static Action none() {
            return NONE;
        }
    }

    private final NumberSetting healthThreshold;
    private final NumberSetting delayTicks;
    private int originalSlot = -1;
    private int selectedSoupSlot = -1;
    private long lastUseTick = -1L;

    public AutoSoup() {
        super("auto_soup", "AutoSoup", "Uses player-owned mushroom stew through normal held-item interactions.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold",
                "Use mushroom stew at or below this locally observed health value.", 10.0D, 1.0D, 20.0D));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay",
                "Minimum client ticks between normal mushroom-stew use requests.", 5.0D, 1.0D, 200.0D));
    }

    /** Computes the next owned hotbar/use action entirely from local observations. */
    public Action update(long tick, Context context) {
        if (tick < 0L || context == null) {
            throw new IllegalArgumentException("auto-soup inputs are invalid");
        }
        if (selectedSoupSlot >= 0) {
            if (context.usingItem()) {
                return Action.none();
            }
            return releaseOwnedSlot(context.selectedSlot());
        }
        if (!isEnabled() || context.screenOpen() || context.health() > healthThreshold.value()
                || (lastUseTick >= 0L && tick - lastUseTick < Math.round(delayTicks.value()))) {
            return Action.none();
        }
        Integer candidate = context.soupSlots().stream().min(Comparator.naturalOrder()).orElse(null);
        if (candidate == null) {
            return Action.none();
        }
        originalSlot = context.selectedSlot();
        selectedSoupSlot = candidate;
        lastUseTick = tick;
        return candidate == context.selectedSlot()
                ? new Action(ActionType.USE_SELECTED, candidate)
                : new Action(ActionType.SELECT_AND_USE, candidate);
    }

    public void onPlayerUnavailable() {
        originalSlot = -1;
        selectedSoupSlot = -1;
    }

    private Action releaseOwnedSlot(int currentSlot) {
        int restore = originalSlot;
        boolean ownsSelection = currentSlot == selectedSoupSlot && restore >= 0 && restore != currentSlot;
        originalSlot = -1;
        selectedSoupSlot = -1;
        return ownsSelection ? new Action(ActionType.RESTORE_SLOT, restore) : Action.none();
    }
}
