package dev.helikon.client.module.movement;

import net.minecraft.world.entity.player.Input;

import java.util.Objects;

/** Narrow Minecraft-facing bridge for InventoryWalk's tested input policy. */
public final class InventoryWalkAccess {
    private static volatile InventoryWalk inventoryWalk;

    private InventoryWalkAccess() {
    }

    public static void install(InventoryWalk module) {
        inventoryWalk = Objects.requireNonNull(module, "module");
    }

    /** Applies the inventory-only physical-key policy and preserves untouched input fields. */
    public static Input apply(Input currentInput, Input physicalInput,
                              boolean ordinaryInventoryScreen, boolean textEntryFocused) {
        Input current = Objects.requireNonNull(currentInput, "currentInput");
        Input physical = Objects.requireNonNull(physicalInput, "physicalInput");
        InventoryWalk module = inventoryWalk;
        if (module == null) {
            return current;
        }
        MovementInput result = module.apply(movementInput(current), movementInput(physical),
                ordinaryInventoryScreen, textEntryFocused);
        if (matches(result, current)) {
            return current;
        }
        return new Input(result.forward(), result.backward(), result.left(), result.right(),
                result.jump(), result.shift(), result.sprint());
    }

    private static MovementInput movementInput(Input input) {
        return new MovementInput(input.forward(), input.backward(), input.left(), input.right(),
                input.jump(), input.shift(), input.sprint());
    }

    private static boolean matches(MovementInput input, Input current) {
        return input.forward() == current.forward()
                && input.backward() == current.backward()
                && input.left() == current.left()
                && input.right() == current.right()
                && input.jump() == current.jump()
                && input.shift() == current.shift()
                && input.sprint() == current.sprint();
    }
}
