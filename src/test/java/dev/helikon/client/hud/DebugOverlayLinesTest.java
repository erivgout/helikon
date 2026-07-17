package dev.helikon.client.hud;

import dev.helikon.client.module.ModuleTimingMetrics;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DebugOverlayLinesTest {
    @Test
    void pagesModuleTimingRowsAndIncludesEveryRequiredLocalDiagnostic() {
        List<ModuleTimingMetrics.Snapshot> snapshots = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            snapshots.add(new ModuleTimingMetrics.Snapshot("module" + index, index * 1_000_000L,
                    index * 2_000_000L, 1L, 1L));
        }

        List<String> lines = DebugOverlayLines.format(snapshots, 2, 3, 4, 5, "saved");

        assertEquals("Helikon Debug 2/2", lines.getFirst());
        assertEquals("module10 T 10.000ms R 20.000ms", lines.get(1));
        assertEquals("module11 T 11.000ms R 22.000ms", lines.get(2));
        assertEquals("Caches block-esp=3 storage-esp=4", lines.get(3));
        assertEquals("Event subscribers: 5", lines.get(4));
        assertEquals("Global config save: saved", lines.get(5));
    }

    @Test
    void clampsLargePagesAndRejectsInvalidDiagnosticFacts() {
        assertEquals("Helikon Debug 1/1", DebugOverlayLines.format(List.of(), 10, 0, 0, 0, "not saved").getFirst());
        assertThrows(IllegalArgumentException.class,
                () -> DebugOverlayLines.format(List.of(), 0, 0, 0, 0, "saved"));
        assertThrows(IllegalArgumentException.class,
                () -> DebugOverlayLines.format(List.of(), 1, -1, 0, 0, "saved"));
    }
}
