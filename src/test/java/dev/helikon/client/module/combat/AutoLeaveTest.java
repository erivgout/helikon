package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AutoLeaveTest {
    @Test
    void leavesOnlyForEnabledConfiguredDangerThresholds() {
        AutoLeave module = new AutoLeave();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);

        assertEquals("auto_leave", module.id());
        assertEquals(ModuleCategory.COMBAT, module.category());
        assertFalse(module.defaultEnabled());
        assertEquals(Optional.empty(), module.danger(new AutoLeave.Context(1.0D, 30.0D)));

        registry.setEnabled(module, true);
        assertEquals(Optional.of(AutoLeave.Danger.LOW_HEALTH), module.danger(new AutoLeave.Context(6.0D, 0.0D)));

        booleanSetting(module, "low_health").set(false);
        assertEquals(Optional.of(AutoLeave.Danger.FALL_DISTANCE), module.danger(new AutoLeave.Context(1.0D, 16.0D)));

        booleanSetting(module, "falling").set(false);
        assertEquals(Optional.empty(), module.danger(new AutoLeave.Context(1.0D, 80.0D)));

        numberSetting(module, "health_threshold").set(10.0D);
        booleanSetting(module, "low_health").set(true);
        assertEquals(Optional.of(AutoLeave.Danger.LOW_HEALTH), module.danger(new AutoLeave.Context(10.0D, 0.0D)));
    }

    @Test
    void rejectsInvalidObservedFacts() {
        assertThrows(IllegalArgumentException.class, () -> new AutoLeave.Context(Double.NaN, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> new AutoLeave.Context(1.0D, -1.0D));
    }

    private static BooleanSetting booleanSetting(AutoLeave module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(AutoLeave module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
