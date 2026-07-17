package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AntiAfkTest {
    private static final AntiAfk.Context IDLE_GROUNDED = new AntiAfk.Context(false, false, true);

    @Test
    void waitsForTheConfiguredIdleIntervalThenRequestsSelectedOrdinaryActions() {
        AntiAfk module = enabledModule();
        booleanSetting(module, "jump").set(true);
        booleanSetting(module, "short_movement").set(true);

        for (int tick = 0; tick < 100; tick++) {
            assertEquals(AntiAfk.Action.NONE, module.tick(IDLE_GROUNDED));
        }
        assertEquals(new AntiAfk.Action(AntiAfk.ROTATION_DEGREES, true, true), module.tick(IDLE_GROUNDED));
        assertEquals(AntiAfk.Action.NONE, module.tick(IDLE_GROUNDED));
    }

    @Test
    void resetsItsIdleTimerForScreensAndManualInputAndNeverJumpsInAir() {
        AntiAfk module = enabledModule();
        booleanSetting(module, "jump").set(true);

        for (int tick = 0; tick < 100; tick++) {
            module.tick(IDLE_GROUNDED);
        }
        assertEquals(AntiAfk.Action.NONE, module.tick(new AntiAfk.Context(true, false, true)));
        for (int tick = 0; tick < 100; tick++) {
            module.tick(IDLE_GROUNDED);
        }
        assertEquals(AntiAfk.Action.NONE, module.tick(new AntiAfk.Context(false, true, true)));
        for (int tick = 0; tick < 100; tick++) {
            module.tick(IDLE_GROUNDED);
        }
        assertEquals(new AntiAfk.Action(AntiAfk.ROTATION_DEGREES, false, false),
                module.tick(new AntiAfk.Context(false, false, false)));
    }

    private static AntiAfk enabledModule() {
        AntiAfk module = new AntiAfk();
        numberSetting(module, "interval_seconds").set(5.0D);
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(AntiAfk module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static NumberSetting numberSetting(AntiAfk module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

}
