package dev.helikon.client.module.movement;

import net.minecraft.world.entity.player.Input;

import dev.helikon.client.module.miscellaneous.Twerk;

import java.util.Objects;
import java.util.function.IntPredicate;

/** Narrow Minecraft-facing bridge for the verified keyboard-input mixin. */
public final class MovementModuleAccess {
    private static volatile AutoWalk autoWalk;
    private static volatile AutoSneak autoSneak;
    private static volatile Twerk twerk;

    private MovementModuleAccess() {
    }

    public static void install(AutoWalk autoWalkModule, AutoSneak autoSneakModule, Twerk twerkModule) {
        autoWalk = Objects.requireNonNull(autoWalkModule, "autoWalkModule");
        autoSneak = Objects.requireNonNull(autoSneakModule, "autoSneakModule");
        twerk = Objects.requireNonNull(twerkModule, "twerkModule");
    }

    /** Returns an input record with only the local movement-module policies applied. */
    public static Input applyMovement(Input input, boolean screenOpen, boolean autoSneakKeyDown) {
        Input current = Objects.requireNonNull(input, "input");
        MovementInput result = new MovementInput(
                current.forward(), current.backward(), current.left(), current.right(),
                current.jump(), current.shift(), current.sprint()
        );
        AutoWalk walkModule = autoWalk;
        if (walkModule != null) {
            result = walkModule.apply(result, screenOpen);
        }
        AutoSneak sneakModule = autoSneak;
        if (sneakModule != null) {
            result = sneakModule.apply(result, screenOpen, autoSneakKeyDown);
        }
        Twerk twerkModule = twerk;
        if (twerkModule != null) {
            result = twerkModule.apply(result, screenOpen);
        }
        if (result.forward() == current.forward()
                && result.backward() == current.backward()
                && result.left() == current.left()
                && result.right() == current.right()
                && result.shift() == current.shift()) {
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

    /** Reads the configured AutoSneak key only through a caller-supplied platform adapter. */
    public static boolean isAutoSneakKeyDown(IntPredicate keyStateReader) {
        IntPredicate reader = Objects.requireNonNull(keyStateReader, "keyStateReader");
        AutoSneak module = autoSneak;
        return module != null && module.keybind().isBound() && reader.test(module.keybind().keyCode());
    }
}
