package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoSprintTest {
    private static final SprintContext FORWARD = new SprintContext(true, true, 20, false, false);

    @Test
    void requestsSprintOnlyWhenEnabledAndConditionsPass() {
        AutoSprint autoSprint = new AutoSprint();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(autoSprint);

        assertEquals(AutoSprint.SprintAction.NONE, autoSprint.update(FORWARD));
        registry.setEnabled(autoSprint, true);
        assertEquals(AutoSprint.SprintAction.START, autoSprint.update(FORWARD));
        assertEquals(AutoSprint.SprintAction.NONE,
                autoSprint.update(new SprintContext(true, true, 20, false, true)));

        assertEquals(AutoSprint.SprintAction.STOP,
                autoSprint.update(new SprintContext(false, false, 20, false, true)));
        assertEquals(AutoSprint.SprintAction.NONE,
                autoSprint.update(new SprintContext(false, false, 20, false, false)));
    }

    @Test
    void honorsHungerCollisionAndDirectionSettings() {
        AutoSprint autoSprint = new AutoSprint();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(autoSprint);
        registry.setEnabled(autoSprint, true);

        assertEquals(AutoSprint.SprintAction.NONE,
                autoSprint.update(new SprintContext(true, true, 6, false, false)));
        assertEquals(AutoSprint.SprintAction.NONE,
                autoSprint.update(new SprintContext(true, true, 20, true, false)));

        booleanSetting(autoSprint, "forward_only").set(false);
        assertEquals(AutoSprint.SprintAction.START,
                autoSprint.update(new SprintContext(false, true, 20, false, false)));
        assertEquals(AutoSprint.SprintAction.STOP,
                autoSprint.update(new SprintContext(false, false, 20, false, true)));

        booleanSetting(autoSprint, "always").set(true);
        assertEquals(AutoSprint.SprintAction.START,
                autoSprint.update(new SprintContext(false, false, 20, false, false)));
    }

    @Test
    void disablesOnlySprintItPreviouslyRequested() {
        AutoSprint autoSprint = new AutoSprint();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(autoSprint);

        registry.setEnabled(autoSprint, true);
        assertEquals(AutoSprint.SprintAction.START, autoSprint.update(FORWARD));
        registry.setEnabled(autoSprint, false);
        assertEquals(AutoSprint.SprintAction.STOP,
                autoSprint.update(new SprintContext(true, true, 20, false, true)));

        registry.setEnabled(autoSprint, true);
        assertEquals(AutoSprint.SprintAction.NONE,
                autoSprint.update(new SprintContext(true, true, 20, false, true)));
        registry.setEnabled(autoSprint, false);
        assertEquals(AutoSprint.SprintAction.NONE,
                autoSprint.update(new SprintContext(true, true, 20, false, true)));
    }

    private static BooleanSetting booleanSetting(AutoSprint module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
