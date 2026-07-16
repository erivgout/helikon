package dev.helikon.client.panic;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanicControllerTest {
    @Test
    void disablesThroughRegistryHidesHudAndClearsOnlyTransientState() {
        ModuleRegistry registry = new ModuleRegistry();
        TestModule module = new TestModule();
        registry.register(module);
        registry.setEnabled(module, true);
        PanicState state = new PanicState();
        AtomicInteger closed = new AtomicInteger();
        AtomicInteger cleared = new AtomicInteger();
        PanicController controller = new PanicController(registry, state, closed::incrementAndGet, cleared::incrementAndGet);

        PanicController.Result result = controller.activate();

        assertEquals(1, result.disabledModules());
        assertFalse(module.isEnabled());
        assertEquals(1, module.disabled);
        assertTrue(state.customHudHidden());
        assertEquals(1, closed.get());
        assertEquals(1, cleared.get());

        controller.restoreCustomHud();
        assertFalse(state.customHudHidden());
    }

    private static final class TestModule extends Module {
        private int disabled;

        private TestModule() {
            super("panic_test", "Panic Test", "Test module.", ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        }

        @Override
        protected void onDisable() {
            disabled++;
        }
    }
}
