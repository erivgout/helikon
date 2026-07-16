package dev.helikon.client.module.movement;

import net.minecraft.world.entity.player.Input;

import java.util.Objects;

/** Narrow Minecraft-facing bridge for the verified keyboard-input mixin. */
public final class MovementModuleAccess {
    private static volatile AutoWalk autoWalk;

    private MovementModuleAccess() {
    }

    public static void install(AutoWalk autoWalkModule) {
        autoWalk = Objects.requireNonNull(autoWalkModule, "autoWalkModule");
    }

    /** Returns an input record with only the local AutoWalk policy applied. */
    public static Input applyAutoWalk(Input input, boolean screenOpen) {
        Input current = Objects.requireNonNull(input, "input");
        AutoWalk module = autoWalk;
        if (module == null) {
            return current;
        }

        MovementInput result = module.apply(new MovementInput(
                current.forward(), current.backward(), current.left(), current.right(),
                current.jump(), current.shift(), current.sprint()
        ), screenOpen);
        if (result.forward() == current.forward()
                && result.backward() == current.backward()
                && result.left() == current.left()
                && result.right() == current.right()) {
            return current;
        }
        return new Input(result.forward(), result.backward(), result.left(), result.right(),
                result.jump(), result.shift(), result.sprint());
    }

    /** Converts a Minecraft input record to the matching tested effective movement vector. */
    public static MovementInput.MovementVector movementVector(Input input) {
        Input current = Objects.requireNonNull(input, "input");
        return new MovementInput(
                current.forward(), current.backward(), current.left(), current.right(),
                current.jump(), current.shift(), current.sprint()
        ).movementVector();
    }
}
