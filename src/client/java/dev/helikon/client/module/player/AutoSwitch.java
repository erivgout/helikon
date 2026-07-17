package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.ItemSelectorSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Minecraft-free trigger, hotbar selection, and ownership-safe restoration for AutoSwitch. */
public final class AutoSwitch extends Module {
    public enum Trigger {
        ATTACK_HELD,
        USE_HELD,
        SNEAKING,
        LOW_HEALTH,
        ALWAYS
    }

    public enum ActionType {
        NONE,
        SELECT,
        RESTORE
    }

    public record HotbarItem(int slot, String itemId) {
        public HotbarItem {
            if (slot < 0 || slot > 8 || itemId == null || itemId.isBlank()) {
                throw new IllegalArgumentException("AutoSwitch hotbar item is invalid");
            }
            itemId = itemId.toLowerCase(Locale.ROOT);
        }
    }

    public record Context(int currentSlot, boolean screenOpen, boolean attackHeld, boolean useHeld,
                          boolean sneaking, double health, List<HotbarItem> hotbarItems) {
        public Context {
            if (currentSlot < 0 || currentSlot > 8 || !Double.isFinite(health) || health < 0.0D) {
                throw new IllegalArgumentException("AutoSwitch context is invalid");
            }
            hotbarItems = List.copyOf(Objects.requireNonNull(hotbarItems, "hotbarItems"));
        }
    }

    public record Action(ActionType type, int slot) {
        private static final Action NONE = new Action(ActionType.NONE, -1);

        public Action {
            if (type == null || (type == ActionType.NONE && slot != -1)
                    || (type != ActionType.NONE && (slot < 0 || slot > 8))) {
                throw new IllegalArgumentException("AutoSwitch action is invalid");
            }
        }

        public static Action none() {
            return NONE;
        }
    }

    private final ItemSelectorSetting targetItems;
    private final EnumSetting<Trigger> trigger;
    private final NumberSetting healthThreshold;
    private final BooleanSetting restorePriorSlot;
    private int priorSlot = -1;
    private int ownedSlot = -1;
    private boolean restoreRequested;
    private boolean selectionSuspended;

    public AutoSwitch() {
        super("auto_switch", "AutoSwitch", "Selects configured hotbar items when a local condition is active.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        targetItems = addSetting(new ItemSelectorSetting("target_items", "Target items",
                "Ordered item IDs to select from the existing hotbar.",
                List.of("minecraft:diamond_sword", "minecraft:iron_sword", "minecraft:stone_sword"), 16));
        trigger = addSetting(new EnumSetting<>("trigger", "Trigger", "Local condition that activates switching.",
                Trigger.class, Trigger.ATTACK_HELD));
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold",
                "Activate at or below this health when Low Health is selected.",
                10.0D, 1.0D, 20.0D, () -> trigger.value() == Trigger.LOW_HEALTH));
        restorePriorSlot = addSetting(new BooleanSetting("restore_prior_slot", "Restore prior slot",
                "Return to the prior slot after the configured condition ends.", true));
    }

    /** Produces one bounded local slot selection or ownership-safe restoration request. */
    public Action update(Context context) {
        Context current = Objects.requireNonNull(context, "context");
        if (restoreRequested) {
            restoreRequested = false;
            return release(current.currentSlot());
        }

        boolean active = isEnabled() && !current.screenOpen() && triggerActive(current);
        if (!active) {
            return release(current.currentSlot());
        }
        if (ownedSlot >= 0 && current.currentSlot() != ownedSlot) {
            clearOwnership();
            selectionSuspended = true;
            return Action.none();
        }
        if (selectionSuspended) {
            return Action.none();
        }

        int selected = preferredSlot(current.hotbarItems());
        if (selected < 0) {
            return release(current.currentSlot());
        }
        if (selected == current.currentSlot()) {
            return Action.none();
        }
        if (priorSlot < 0) {
            priorSlot = current.currentSlot();
        }
        ownedSlot = selected;
        return new Action(ActionType.SELECT, selected);
    }

    /** Drops ownership when no local player exists; no stale slot is restored in another world. */
    public void onPlayerUnavailable() {
        clearOwnership();
        restoreRequested = false;
    }

    private boolean triggerActive(Context context) {
        return switch (trigger.value()) {
            case ATTACK_HELD -> context.attackHeld();
            case USE_HELD -> context.useHeld();
            case SNEAKING -> context.sneaking();
            case LOW_HEALTH -> context.health() <= healthThreshold.value();
            case ALWAYS -> true;
        };
    }

    private int preferredSlot(List<HotbarItem> candidates) {
        for (String configured : targetItems.value()) {
            int bestSlot = 9;
            for (HotbarItem candidate : candidates) {
                if (candidate.itemId().equals(configured) && candidate.slot() < bestSlot) {
                    bestSlot = candidate.slot();
                }
            }
            if (bestSlot < 9) {
                return bestSlot;
            }
        }
        return -1;
    }

    private Action release(int currentSlot) {
        if (!restorePriorSlot.value() || priorSlot < 0 || currentSlot != ownedSlot) {
            clearOwnership();
            return Action.none();
        }
        int restoreSlot = priorSlot;
        clearOwnership();
        return new Action(ActionType.RESTORE, restoreSlot);
    }

    private void clearOwnership() {
        priorSlot = -1;
        ownedSlot = -1;
        selectionSuspended = false;
    }

    @Override
    protected void onDisable() {
        if (priorSlot >= 0) {
            restoreRequested = true;
        }
    }
}
