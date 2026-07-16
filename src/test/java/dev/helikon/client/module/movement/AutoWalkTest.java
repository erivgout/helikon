package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoWalkTest {
    private static final MovementInput INPUT = new MovementInput(false, true, true, false, true, true, false);

    @Test
    void onlyForcesForwardWhenEnabledAndPermitted() {
        AutoWalk autoWalk = new AutoWalk();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(autoWalk);

        assertEquals(INPUT, autoWalk.apply(INPUT, false));
        registry.setEnabled(autoWalk, true);
        assertEquals(new MovementInput(true, true, true, false, true, true, false), autoWalk.apply(INPUT, false));
        assertEquals(new MovementInput.MovementVector(0.0F, 1.0F),
                autoWalk.apply(new MovementInput(false, false, false, false, false, false, false), false)
                        .movementVector());
        assertEquals(INPUT, autoWalk.apply(INPUT, true));

        booleanSetting(autoWalk, "continue_forward").set(false);
        assertEquals(INPUT, autoWalk.apply(INPUT, false));
    }

    @Test
    void canKeepOrClearUserSteeringWithoutChangingOtherInputs() {
        AutoWalk autoWalk = new AutoWalk();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(autoWalk);
        registry.setEnabled(autoWalk, true);

        booleanSetting(autoWalk, "allow_steering").set(false);
        assertEquals(new MovementInput(true, false, false, false, true, true, false), autoWalk.apply(INPUT, false));

        booleanSetting(autoWalk, "stop_on_gui").set(false);
        assertEquals(new MovementInput(true, false, false, false, true, true, false), autoWalk.apply(INPUT, true));
    }

    @Test
    void movementVectorMatchesMinecraftsNormalizedKeyImpulses() {
        assertEquals(new MovementInput.MovementVector(0.0F, 0.0F),
                new MovementInput(true, true, false, false, false, false, false).movementVector());
        float diagonal = (float) (1.0 / Math.sqrt(2.0));
        assertEquals(new MovementInput.MovementVector(diagonal, diagonal),
                new MovementInput(true, false, true, false, false, false, false).movementVector());
    }

    private static BooleanSetting booleanSetting(AutoWalk module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
