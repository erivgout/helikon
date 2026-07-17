package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockEspScanAnchorTest {
    @Test
    void keepsScanProgressStableThroughOrdinaryMovementThenRecentersAtABoundedThreshold() {
        BlockEspScanAnchor anchor = new BlockEspScanAnchor();

        assertTrue(anchor.update(0, 64, 0, 24, 24, true).changed());
        assertFalse(anchor.update(40, 64, 0, 24, 24, false).changed());
        assertTrue(anchor.update(40, 64, 0, 24, 24, true).changed());
    }

    @Test
    void changingScanSettingsCreatesANewRegionImmediately() {
        BlockEspScanAnchor anchor = new BlockEspScanAnchor();
        anchor.update(0, 64, 0, 24, 24, true);

        BlockEspScanAnchor.Update update = anchor.update(0, 64, 0, 32, 24, false);
        assertFalse(update.changed());
        // Configuration changes are handled by the renderer's explicit cache/cursor reset;
        // the anchor itself continues to supply the new bounded region.
        org.junit.jupiter.api.Assertions.assertEquals(32, update.region().horizontalRadius());
    }
}
