package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;

import java.util.Comparator;
import java.util.List;

/** Shared ownership-safe hotbar selection and ordinary-use cadence for defensive item modules. */
public abstract class HotbarUseModule extends Module {
    private final BooleanSetting selectHotbar;
    private final IntegerSetting cooldownTicks;
    private int priorSlot = -1;
    private int selectedSlot = -1;
    private long lastUseTick = Long.MIN_VALUE;

    protected HotbarUseModule(String id, String name, String description, int defaultCooldown) {
        super(id, name, description, ModuleCategory.PLAYER, false, Keybind.unbound());
        selectHotbar = addSetting(new BooleanSetting("select_hotbar", "Select hotbar",
                "Temporarily select an eligible existing hotbar item.", true));
        cooldownTicks = addSetting(new IntegerSetting("cooldown_ticks", "Cooldown ticks",
                "Minimum ticks between ordinary use attempts.", defaultCooldown, 1, 200));
    }

    public Action update(long tick, Context context, List<Candidate> candidates) {
        if (tick < 0L || context == null || candidates == null) {
            throw new IllegalArgumentException("Hotbar use inputs are invalid");
        }
        if (selectedSlot >= 0) {
            if (context.currentSlot() != selectedSlot) {
                clearOwnership();
                return Action.none();
            }
            if (!context.usingItem()) {
                int restore = priorSlot;
                clearOwnership();
                return new Action(ActionType.RESTORE, restore);
            }
            return Action.none();
        }
        if (!isEnabled() || !context.triggered() || context.screenOpen() || context.usingItem()
                || (lastUseTick != Long.MIN_VALUE && tick - lastUseTick < cooldownTicks.value())) {
            return Action.none();
        }
        Candidate candidate = candidates.stream().filter(this::accepts)
                .min(Comparator.comparingInt(Candidate::slot)).orElse(null);
        if (candidate == null) {
            return Action.none();
        }
        lastUseTick = tick;
        if (candidate.slot() == context.currentSlot() || !selectHotbar.value()) {
            return candidate.slot() == context.currentSlot()
                    ? new Action(ActionType.USE_SELECTED, candidate.slot()) : Action.none();
        }
        priorSlot = context.currentSlot();
        selectedSlot = candidate.slot();
        return new Action(ActionType.SELECT_AND_USE, selectedSlot);
    }

    protected abstract boolean accepts(Candidate candidate);

    public void onContextLost() {
        clearOwnership();
        lastUseTick = Long.MIN_VALUE;
    }

    @Override
    protected void onDisable() {
        lastUseTick = Long.MIN_VALUE;
    }

    private void clearOwnership() {
        priorSlot = -1;
        selectedSlot = -1;
    }

    public record Context(int currentSlot, boolean triggered, boolean screenOpen, boolean usingItem) {
        public Context {
            if (currentSlot < 0 || currentSlot > 8) {
                throw new IllegalArgumentException("Selected slot must be 0..8");
            }
        }
    }

    public record Candidate(int slot, boolean food, boolean milk, boolean waterBucket, boolean fireResistance) {
        public Candidate {
            if (slot < 0 || slot > 8) {
                throw new IllegalArgumentException("Candidate slot must be 0..8");
            }
        }
    }

    public record Action(ActionType type, int slot) {
        private static final Action NONE = new Action(ActionType.NONE, -1);

        public static Action none() {
            return NONE;
        }
    }

    public enum ActionType {
        NONE,
        SELECT_AND_USE,
        USE_SELECTED,
        RESTORE
    }
}
