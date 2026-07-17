package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Lowers the ordinary local use cooldown without creating interactions of its own. */
public final class FastPlace extends Module {
    public enum ItemFilter {
        ALL,
        BLOCKS,
        NON_BLOCKS
    }

    public record Action(boolean setDelay, int delay) {
        private static final Action NONE = new Action(false, -1);

        public static Action none() {
            return NONE;
        }
    }

    private final NumberSetting useDelay;
    private final EnumSetting<ItemFilter> itemFilter;
    private final NumberSetting safeMinimumDelay;
    private final CooldownAccess cooldown;
    private int originalDelay = -1;
    private int lastAppliedDelay = -1;

    public FastPlace(CooldownAccess cooldown) {
        super("fast_place", "FastPlace", "Lowers the ordinary local item-use cooldown while use is held.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
        useDelay = addSetting(new NumberSetting("use_delay", "Client use delay",
                "The requested local cooldown in ticks after an ordinary use.", 0.0, 0.0, 4.0));
        itemFilter = addSetting(new EnumSetting<>("item_filter", "Item filter",
                "Choose which held item types may receive the local cooldown reduction.",
                ItemFilter.class, ItemFilter.BLOCKS));
        safeMinimumDelay = addSetting(new NumberSetting("safe_minimum_delay", "Safe minimum delay",
                "Never lower the local cooldown below this many ticks.", 0.0, 0.0, 4.0));
    }

    /** Produces a cooldown reduction only for an already-held ordinary use action. */
    public Action update(boolean useHeld, boolean heldItemIsBlock, int currentDelay) {
        if (currentDelay < 0) {
            throw new IllegalArgumentException("currentDelay must not be negative");
        }
        if (!isEnabled() || !useHeld || !matchesFilter(heldItemIsBlock)) {
            return Action.none();
        }

        int requestedDelay = Math.max((int) Math.round(useDelay.value()),
                (int) Math.round(safeMinimumDelay.value()));
        if (currentDelay <= requestedDelay) {
            return Action.none();
        }
        return new Action(true, requestedDelay);
    }

    /** Applies one cooldown reduction through the narrow platform port. */
    public Action tick(boolean useHeld, boolean heldItemIsBlock) {
        int currentDelay = cooldown.delay();
        Action action = update(useHeld, heldItemIsBlock, currentDelay);
        if (!action.setDelay()) {
            return action;
        }
        if (originalDelay < 0) {
            originalDelay = currentDelay;
        }
        lastAppliedDelay = action.delay();
        cooldown.setDelay(action.delay());
        return action;
    }

    @Override
    protected void onDisable() {
        if (originalDelay >= 0 && cooldown.delay() == lastAppliedDelay) {
            cooldown.setDelay(originalDelay);
        }
        originalDelay = -1;
        lastAppliedDelay = -1;
    }

    private boolean matchesFilter(boolean heldItemIsBlock) {
        return switch (itemFilter.value()) {
            case ALL -> true;
            case BLOCKS -> heldItemIsBlock;
            case NON_BLOCKS -> !heldItemIsBlock;
        };
    }

    /** Narrow Minecraft-free port for the transient client item-use cooldown. */
    public interface CooldownAccess {
        int delay();

        void setDelay(int value);
    }
}
