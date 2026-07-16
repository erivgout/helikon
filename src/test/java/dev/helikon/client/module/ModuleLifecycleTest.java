package dev.helikon.client.module;

import dev.helikon.client.input.Keybind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleLifecycleTest {
    @Test
    void registryRunsLifecycleCallbacksExactlyOncePerStateChange() {
        RecordingModule module = new RecordingModule();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);

        assertTrue(registry.setEnabled(module, true));
        assertTrue(registry.setEnabled(module, true));
        assertTrue(module.isEnabled());
        assertEquals(1, module.enableCount);

        assertTrue(registry.setEnabled(module, false));
        assertTrue(registry.setEnabled(module, false));
        assertFalse(module.isEnabled());
        assertEquals(1, module.disableCount);
    }

    @Test
    void registryDisablesAModuleAfterItsEnableCallbackFails() {
        ModuleRegistry registry = new ModuleRegistry();
        FailingModule module = new FailingModule();
        registry.register(module);

        assertFalse(registry.setEnabled(module, true));
        assertFalse(module.isEnabled());
        assertEquals(1, module.disableCount);
    }

    private static final class RecordingModule extends Module {
        private int enableCount;
        private int disableCount;

        private RecordingModule() {
            super("recording", "Recording", "Records lifecycle calls.", ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        }

        @Override
        protected void onEnable() {
            enableCount++;
        }

        @Override
        protected void onDisable() {
            disableCount++;
        }
    }

    private static final class FailingModule extends Module {
        private int disableCount;

        private FailingModule() {
            super("failing", "Failing", "Fails during enable.", ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        }

        @Override
        protected void onEnable() {
            throw new IllegalStateException("Expected test failure");
        }

        @Override
        protected void onDisable() {
            disableCount++;
        }
    }
}
