package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;

/**
 * Decides whether an ordinary local attack should briefly use a configured hotbar slot. Minecraft
 * inventory access and the vanilla carried-item synchronization remain in the narrow adapter.
 */
public final class HitSwap extends Module {
    public enum ActionType {
        NONE,
        SELECT,
        RESTORE
    }

    public record Action(ActionType type, int slot) {
        private static final Action NONE = new Action(ActionType.NONE, -1);

        public Action {
            if (type == null || (type == ActionType.NONE && slot != -1)
                    || (type != ActionType.NONE && (slot < 0 || slot > 8))) {
                throw new IllegalArgumentException("HitSwap action is invalid");
            }
        }

        public static Action none() {
            return NONE;
        }
    }

    private final IntegerSetting weaponSlot;
    private int priorSlot = -1;
    private int ownedSlot = -1;

    public HitSwap() {
        super("hit_swap", "HitSwap",
                "Briefly selects a configured non-empty hotbar slot for ordinary attacks.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        weaponSlot = addSetting(new IntegerSetting(
                "weapon_slot",
                "Weapon slot",
                "One-based hotbar slot to select when an ordinary local attack begins.",
                1,
                1,
                9
        ));
    }

    /**
     * Produces the selection needed immediately before an ordinary attack.
     *
     * @param currentSlot hotbar index currently selected by the player
     * @param configuredSlotOccupied whether the configured slot contains an item
     */
    public Action beforeAttack(int currentSlot, boolean configuredSlotOccupied) {
        requireHotbarSlot(currentSlot);
        if (ownedSlot >= 0 && currentSlot != ownedSlot) {
            clearOwnership();
        }
        if (!isEnabled() || !configuredSlotOccupied) {
            return Action.none();
        }

        int configuredSlot = weaponSlot.value() - 1;
        if (currentSlot == configuredSlot) {
            return Action.none();
        }
        priorSlot = currentSlot;
        ownedSlot = configuredSlot;
        return new Action(ActionType.SELECT, configuredSlot);
    }

    /**
     * Restores the prior slot only while the player still has the module-owned slot selected.
     * Calling this once per client tick also completes cleanup after disable or panic.
     */
    public Action restore(int currentSlot) {
        requireHotbarSlot(currentSlot);
        if (priorSlot < 0) {
            return Action.none();
        }
        if (currentSlot != ownedSlot) {
            clearOwnership();
            return Action.none();
        }

        int restoreSlot = priorSlot;
        clearOwnership();
        return new Action(ActionType.RESTORE, restoreSlot);
    }

    public int weaponSlot() {
        return weaponSlot.value();
    }

    public void onPlayerUnavailable() {
        clearOwnership();
    }

    @Override
    protected void onDisable() {
        // The always-running restore adapter releases any selection still owned by this module.
    }

    private void clearOwnership() {
        priorSlot = -1;
        ownedSlot = -1;
    }

    private static void requireHotbarSlot(int slot) {
        if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException("slot must be a hotbar index");
        }
    }
}
