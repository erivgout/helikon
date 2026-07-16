package dev.helikon.client.hud;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveModulesTest {
    @Test
    void includesOnlyEnabledModulesInRegistryOrder() {
        ModuleRegistry registry = new ModuleRegistry();
        TestModule first = new TestModule("first", "First", ModuleCategory.RENDER);
        TestModule second = new TestModule("second", "Second", ModuleCategory.MOVEMENT);
        TestModule third = new TestModule("third", "Third", ModuleCategory.CHAT);
        registry.register(first);
        registry.register(second);
        registry.register(third);

        registry.setEnabled(first, true);
        registry.setEnabled(third, true);

        assertEquals(List.of("First", "Third"), ActiveModules.enabledNames(registry));
    }

    private static final class TestModule extends Module {
        private TestModule(String id, String name, ModuleCategory category) {
            super(id, name, "HUD selection test module.", category, false, Keybind.unbound());
        }
    }
}
