package dev.helikon.client.hud;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    @Test
    void supportsAlphabeticalAndInjectedWidthSorting() {
        ModuleRegistry registry = new ModuleRegistry();
        TestModule alpha = new TestModule("alpha", "Alpha", ModuleCategory.RENDER);
        TestModule beta = new TestModule("beta", "Beta", ModuleCategory.RENDER);
        TestModule longest = new TestModule("longest", "Longest", ModuleCategory.RENDER);
        registry.register(alpha);
        registry.register(beta);
        registry.register(longest);
        registry.setEnabled(beta, true);
        registry.setEnabled(longest, true);
        registry.setEnabled(alpha, true);

        assertEquals(List.of("Alpha", "Beta", "Longest"),
                ActiveModules.enabledNames(registry, ActiveModulesLayout.Sort.ALPHABETICAL, String::length));
        assertEquals(List.of("Longest", "Alpha", "Beta"),
                ActiveModules.enabledNames(registry, ActiveModulesLayout.Sort.WIDTH,
                        name -> name.equals("Alpha") || name.equals("Beta") ? 5 : name.length()));
    }

    @Test
    void rainbowColorChangesAcrossHueCycleAndStaysOpaque() {
        int first = ActiveModules.rainbowColor(0);
        int second = ActiveModules.rainbowColor(120);

        assertEquals(0xFF000000, first & 0xFF000000);
        assertNotEquals(first, second);
    }

    @Test
    void entryAnimationOpacityIsBoundedAndCompletesQuickly() {
        assertEquals(0.0F, ActiveModulesHud.entryOpacity(1_000L, 1_000L));
        assertEquals(0.5F, ActiveModulesHud.entryOpacity(1_000L, 1_075L));
        assertEquals(1.0F, ActiveModulesHud.entryOpacity(1_000L, 1_300L));
        assertEquals(0.0F, ActiveModulesHud.entryOpacity(1_000L, 900L));
    }

    private static final class TestModule extends Module {
        private TestModule(String id, String name, ModuleCategory category) {
            super(id, name, "HUD selection test module.", category, false, Keybind.unbound());
        }
    }
}
