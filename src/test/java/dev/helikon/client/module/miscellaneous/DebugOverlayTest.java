package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleTimingMetrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugOverlayTest {
    @Test
    void enablingAndDisablingTheLocalOverlayControlsTimingCollection() {
        ModuleTimingMetrics metrics = new ModuleTimingMetrics();
        DebugOverlay overlay = new DebugOverlay(metrics);

        assertFalse(metrics.isRecording());
        overlay.enable();
        assertTrue(metrics.isRecording());
        overlay.disable();
        assertFalse(metrics.isRecording());
    }
}
