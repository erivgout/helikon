package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.OptionalInt;

/** Selects a safe local hotbar tool during ordinary user-initiated block mining. */
public final class AutoTool extends Module {
    public enum ActionType {
        NONE,
        SELECT,
        RESTORE
    }

    public record Action(ActionType type, int slot) {
        private static final Action NONE = new Action(ActionType.NONE, -1);

        public static Action none() {
            return NONE;
        }
    }

    private final NumberSetting minimumDurability;
    private final BooleanSetting restorePriorSlot;
    private int priorSlot = -1;
    private int selectedToolSlot = -1;
    private boolean restoreRequested;
    private boolean selectionSuspended;

    public AutoTool() {
        super("auto_tool", "AutoTool", "Selects the best safe local hotbar tool while mining.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        minimumDurability = addSetting(new NumberSetting("minimum_durability", "Minimum durability",
                "Keep this many durability points before a tool is avoided.", 8.0, 0.0, 2_032.0));
        restorePriorSlot = addSetting(new BooleanSetting("restore_prior_slot", "Restore prior slot",
                "Return to the prior slot after ordinary block mining ends.", true));
    }

    /** Produces one selection or restoration request from Minecraft-free mining facts. */
    public Action update(boolean mining, int currentSlot, List<ToolCandidate> candidates) {
        if (currentSlot < 0 || currentSlot >= 9) {
            throw new IllegalArgumentException("currentSlot must be a hotbar index");
        }
        if (restoreRequested) {
            restoreRequested = false;
            return release(currentSlot);
        }
        if (!isEnabled() || !mining) {
            return release(currentSlot);
        }

        if (selectedToolSlot >= 0 && currentSlot != selectedToolSlot) {
            clearOwnership();
            selectionSuspended = true;
            return Action.none();
        }
        if (selectionSuspended) {
            return Action.none();
        }

        OptionalInt selected = ToolSelection.bestSlot(candidates, (int) Math.round(minimumDurability.value()));
        if (selected.isEmpty() || selected.getAsInt() == currentSlot) {
            return Action.none();
        }
        if (priorSlot < 0) {
            priorSlot = currentSlot;
        }
        selectedToolSlot = selected.getAsInt();
        return new Action(ActionType.SELECT, selectedToolSlot);
    }

    /** Clears pending selection state when a world no longer has a local player. */
    public void onPlayerUnavailable() {
        clearOwnership();
        restoreRequested = false;
    }

    @Override
    protected void onDisable() {
        if (priorSlot >= 0) {
            restoreRequested = true;
        }
    }

    private Action release(int currentSlot) {
        if (!restorePriorSlot.value() || priorSlot < 0 || currentSlot != selectedToolSlot) {
            clearOwnership();
            return Action.none();
        }
        int restoreSlot = priorSlot;
        clearOwnership();
        return new Action(ActionType.RESTORE, restoreSlot);
    }

    private void clearOwnership() {
        priorSlot = -1;
        selectedToolSlot = -1;
        selectionSuspended = false;
    }
}
