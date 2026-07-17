package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AutoSwimTest {
    private static final AutoSwim.Context SWIMMING_FORWARD = new AutoSwim.Context(false, true, true,
            false, false, 20, false);

    @Test
    void requestsAndReleasesOnlyItsOwnNormalSwimSprint() {
        AutoSwim autoSwim = enabled(new AutoSwim());

        assertEquals("auto_swim", autoSwim.id());
        assertEquals(ModuleCategory.MOVEMENT, autoSwim.category());
        assertFalse(autoSwim.defaultEnabled());
        assertEquals(AutoSwim.SprintAction.START, autoSwim.update(SWIMMING_FORWARD));
        assertEquals(AutoSwim.SprintAction.NONE, autoSwim.update(new AutoSwim.Context(false, true, true,
                false, false, 20, true)));
        assertEquals(AutoSwim.SprintAction.STOP, autoSwim.update(new AutoSwim.Context(false, false, false,
                false, false, 20, true)));
        assertEquals(AutoSwim.SprintAction.NONE, autoSwim.update(new AutoSwim.Context(false, false, false,
                false, false, 20, false)));
    }

    @Test
    void rejectsUnsafeContextsAndHonorsFoodConfiguration() {
        AutoSwim autoSwim = enabled(new AutoSwim());
        IntegerSetting minimumFood = integerSetting(autoSwim, "minimum_food");
        assertEquals(7, minimumFood.value());
        assertEquals(0, minimumFood.minimum());
        assertEquals(20, minimumFood.maximum());

        assertEquals(AutoSwim.SprintAction.NONE, autoSwim.update(new AutoSwim.Context(true, true, true,
                false, false, 20, false)));
        assertEquals(AutoSwim.SprintAction.NONE, autoSwim.update(new AutoSwim.Context(false, false, true,
                false, false, 20, false)));
        assertEquals(AutoSwim.SprintAction.NONE, autoSwim.update(new AutoSwim.Context(false, true, false,
                false, false, 20, false)));
        assertEquals(AutoSwim.SprintAction.NONE, autoSwim.update(new AutoSwim.Context(false, true, true,
                true, false, 20, false)));
        assertEquals(AutoSwim.SprintAction.NONE, autoSwim.update(new AutoSwim.Context(false, true, true,
                false, true, 20, false)));
        assertEquals(AutoSwim.SprintAction.NONE, autoSwim.update(new AutoSwim.Context(false, true, true,
                false, false, 6, false)));
        minimumFood.set(0);
        assertEquals(AutoSwim.SprintAction.START, autoSwim.update(new AutoSwim.Context(false, true, true,
                false, false, 0, false)));
    }

    @Test
    void disableReleasesOnlySprintTheModuleRequested() {
        AutoSwim autoSwim = enabled(new AutoSwim());
        assertEquals(AutoSwim.SprintAction.START, autoSwim.update(SWIMMING_FORWARD));
        autoSwim.disable();
        assertEquals(AutoSwim.SprintAction.STOP, autoSwim.update(new AutoSwim.Context(false, true, true,
                false, false, 20, true)));

        autoSwim.enable();
        assertEquals(AutoSwim.SprintAction.NONE, autoSwim.update(new AutoSwim.Context(false, true, true,
                false, false, 20, true)));
        autoSwim.disable();
        assertEquals(AutoSwim.SprintAction.NONE, autoSwim.update(new AutoSwim.Context(false, true, true,
                false, false, 20, true)));
    }

    private static AutoSwim enabled(AutoSwim module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static IntegerSetting integerSetting(AutoSwim module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
