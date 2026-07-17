package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.BooleanSetting;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Decides, without any Minecraft imports, whether an incoming fall should be
 * broken by requesting a normal held-block placement or an ordinary water-bucket
 * use beneath the local player. The connected server remains authoritative and
 * may still reject, correct, or ignore the resulting interaction.
 */
public final class Clutch extends Module {
    /** Which self-preservation interaction the module is allowed to request. */
    public enum Mode {
        /** Only place a held hotbar block onto the ground below. */
        BLOCK,
        /** Only use a held water bucket onto the ground below. */
        WATER,
        /** Prefer a water bucket when available, otherwise a held block. */
        BOTH
    }

    /** The concrete interaction the adapter should perform this tick. */
    public enum Action {
        NONE,
        PLACE_BLOCK,
        USE_WATER
    }

    /** Immutable local snapshot of the facts the decision needs. */
    public record State(
            boolean descending,
            boolean onGround,
            boolean riding,
            boolean abilityFlying,
            boolean fallFlying,
            boolean inLiquid,
            double fallDistance,
            double spaceBelow,
            boolean hasBlock,
            boolean hasWater
    ) {
        public State {
            if (!Double.isFinite(fallDistance) || fallDistance < 0.0D) {
                throw new IllegalArgumentException("fallDistance must be finite and non-negative");
            }
            if (!Double.isFinite(spaceBelow) || spaceBelow < 0.0D) {
                throw new IllegalArgumentException("spaceBelow must be finite and non-negative");
            }
        }
    }

    /** A hotbar candidate the adapter can switch to before clutching. */
    public record HotbarItem(int slot, boolean block, boolean water) {
        public HotbarItem {
            if (slot < 0 || slot > 8) {
                throw new IllegalArgumentException("hotbar slot must be 0..8");
            }
        }
    }

    private final EnumSetting<Mode> mode;
    private final NumberSetting activationDistance;
    private final NumberSetting minimumFallDistance;
    private final NumberSetting placementDelayTicks;
    private final BooleanSetting selectHotbarItem;
    private final BooleanSetting rotateToTarget;
    private long nextActionTick;

    public Clutch() {
        super("clutch", "Clutch", "Places a block or uses a water bucket beneath the local player to break an incoming fall.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        mode = addSetting(new EnumSetting<>("mode", "Mode",
                "Choose a held block, a water bucket, or both when catching a fall.", Mode.class, Mode.BOTH));
        activationDistance = addSetting(new NumberSetting("activation_distance", "Activation distance",
                "Trigger only when the ground below is within this many blocks.", 3.0D, 1.0D, 5.0D));
        minimumFallDistance = addSetting(new NumberSetting("minimum_fall_distance", "Minimum fall distance",
                "Ignore falls shorter than this accumulated block distance.", 3.0D, 0.0D, 20.0D));
        placementDelayTicks = addSetting(new NumberSetting("placement_delay_ticks", "Placement delay",
                "Minimum ticks between ordinary clutch interaction requests.", 3.0D, 1.0D, 20.0D));
        selectHotbarItem = addSetting(new BooleanSetting("select_hotbar_item", "Select hotbar item",
                "Select a matching hotbar block or water bucket before clutching.", true));
        rotateToTarget = addSetting(new BooleanSetting("rotate_to_target", "Rotate to target",
                "Locally look straight down before requesting the interaction.", true));
    }

    /**
     * Returns the interaction to perform this tick, or {@link Action#NONE}. When a
     * concrete action is returned the placement delay is consumed so requests stay
     * bounded across consecutive ticks.
     */
    public Action plan(long tick, State state) {
        Objects.requireNonNull(state, "state");
        if (!isEnabled() || tick < nextActionTick) {
            return Action.NONE;
        }
        if (state.onGround() || state.riding() || state.abilityFlying() || state.fallFlying()
                || state.inLiquid() || !state.descending()) {
            return Action.NONE;
        }
        if (state.fallDistance() < minimumFallDistance.value()
                || state.spaceBelow() > activationDistance.value()) {
            return Action.NONE;
        }
        Action action = chooseAction(state);
        if (action != Action.NONE) {
            nextActionTick = tick + Math.round(placementDelayTicks.value());
        }
        return action;
    }

    private Action chooseAction(State state) {
        return switch (mode.value()) {
            case BLOCK -> state.hasBlock() ? Action.PLACE_BLOCK : Action.NONE;
            case WATER -> state.hasWater() ? Action.USE_WATER : Action.NONE;
            case BOTH -> state.hasWater() ? Action.USE_WATER
                    : (state.hasBlock() ? Action.PLACE_BLOCK : Action.NONE);
        };
    }

    /**
     * Chooses a hotbar slot holding the item the pending action needs, or empty
     * when auto-selection is off, the held item already matches, or none is found.
     */
    public Optional<Integer> selectSlot(Action action, int selectedSlot, boolean selectedMatches, List<HotbarItem> items) {
        if (!isEnabled() || !selectHotbarItem.value() || action == Action.NONE || selectedMatches || items == null) {
            return Optional.empty();
        }
        if (selectedSlot < 0 || selectedSlot > 8) {
            throw new IllegalArgumentException("selectedSlot must be 0..8");
        }
        return items.stream()
                .filter(item -> action == Action.PLACE_BLOCK ? item.block() : item.water())
                .map(HotbarItem::slot)
                .min(Integer::compareTo);
    }

    public boolean rotateToTarget() {
        return isEnabled() && rotateToTarget.value();
    }

    @Override
    protected void onDisable() {
        nextActionTick = 0L;
    }
}
