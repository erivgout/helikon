package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurabilityWarningsTest {
    @Test
    void returnsOnlyEnabledItemsAtOrBelowTheInclusiveConfiguredPercentage() {
        DurabilityWarnings warnings = enabledModule();
        List<DurabilityWarnings.Item> observed = List.of(
                new DurabilityWarnings.Item("Held", 10, 100),
                new DurabilityWarnings.Item("Helmet", 11, 100),
                new DurabilityWarnings.Item("Chest", 1, 25)
        );

        assertEquals(List.of(observed.get(0), observed.get(2)), warnings.warnings(observed));
    }

    @Test
    void disabledModuleHasNoWarningsAndFactsRequireSaneBounds() {
        DurabilityWarnings warnings = new DurabilityWarnings();

        assertTrue(warnings.warnings(List.of(new DurabilityWarnings.Item("Held", 1, 100))).isEmpty());
    }

    private static DurabilityWarnings enabledModule() {
        DurabilityWarnings warnings = new DurabilityWarnings();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(warnings);
        registry.setEnabled(warnings, true);
        return warnings;
    }
}
