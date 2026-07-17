package dev.helikon.client.module;

import dev.helikon.client.input.Keybind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleTimingMetricsTest {
    @Test
    void recordsOnlyOptInTickScanAndRenderWorkForEveryRegisteredModuleRow() {
        ModuleTimingMetrics metrics = new ModuleTimingMetrics();
        TestModule alpha = new TestModule("alpha");
        TestModule beta = new TestModule("beta");

        metrics.record("alpha", "tick", 99L);
        assertEquals(0L, metrics.snapshots(List.of(alpha)).getFirst().tickNanos());

        metrics.setRecording(true);
        assertTrue(metrics.isRecording());
        metrics.record("alpha", "tick", 20L);
        metrics.record("alpha", "scan", 30L);
        metrics.record("alpha", "render", 40L);
        metrics.record("alpha", "input", 50L);

        List<ModuleTimingMetrics.Snapshot> snapshots = metrics.snapshots(List.of(alpha, beta));
        assertEquals(new ModuleTimingMetrics.Snapshot("alpha", 30L, 40L, 2L, 1L), snapshots.get(0));
        assertEquals(new ModuleTimingMetrics.Snapshot("beta", 0L, 0L, 0L, 0L), snapshots.get(1));
    }

    @Test
    void disablesCollectionImmediatelyAndRejectsInvalidDurationsWhileRecording() {
        ModuleTimingMetrics metrics = new ModuleTimingMetrics();
        metrics.setRecording(true);
        assertThrows(IllegalArgumentException.class, () -> metrics.record("alpha", "tick", -1L));

        metrics.setRecording(false);
        assertFalse(metrics.isRecording());
        metrics.record("alpha", "tick", -1L);
        assertEquals(0L, metrics.snapshots(List.of(new TestModule("alpha"))).getFirst().tickNanos());
    }

    @Test
    void registryMeasuresGuardedWorkOnlyWhileDiagnosticsAreEnabled() {
        ModuleTimingMetrics metrics = new ModuleTimingMetrics();
        ModuleRegistry registry = new ModuleRegistry();
        TestModule module = new TestModule("alpha");
        registry.register(module);
        registry.setTimingMetrics(metrics);

        registry.runGuarded(module, "tick", () -> {
        });
        assertEquals(0L, metrics.snapshots(List.of(module)).getFirst().tickSamples());

        metrics.setRecording(true);
        assertTrue(registry.runGuarded(module, "render", () -> {
        }));
        assertEquals(1L, metrics.snapshots(List.of(module)).getFirst().renderSamples());
    }

    private static final class TestModule extends Module {
        private TestModule(String id) {
            super(id, id, "test module", ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        }
    }
}
