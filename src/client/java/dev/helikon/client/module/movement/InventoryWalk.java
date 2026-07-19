package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

import java.util.Objects;

/** Allows only physical movement keys to remain active in live-world container and Helikon screens. */
public final class InventoryWalk extends Module {
    public InventoryWalk() {
        super("inventory_walk", "InventoryWalk", "Allows local movement in container and Helikon screens.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
    }

    /** Combines safe physical movement keys without treating Shift as movement while an inventory is open. */
    public MovementInput apply(MovementInput currentInput, MovementInput physicalInput,
                               boolean liveGameplayScreen, boolean textEntryFocused) {
        MovementInput current = Objects.requireNonNull(currentInput, "currentInput");
        MovementInput physical = Objects.requireNonNull(physicalInput, "physicalInput");
        if (!isEnabled() || !liveGameplayScreen || textEntryFocused) {
            return current;
        }
        return new MovementInput(
                current.forward() || physical.forward(),
                current.backward() || physical.backward(),
                current.left() || physical.left(),
                current.right() || physical.right(),
                current.jump() || physical.jump(),
                current.shift(),
                current.sprint() || physical.sprint()
        );
    }
}
