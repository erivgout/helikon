package dev.helikon.client.automation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerClickSequenceTest {
    @Test
    void swapUsesTheNormalThreePickupCursorSequence() {
        var clicks = ContainerClickSequence.swap(12, 5);

        assertEquals(3, clicks.size());
        assertEquals(new ContainerClick(12, 0, ContainerClick.Type.PICKUP), clicks.get(0));
        assertEquals(new ContainerClick(5, 0, ContainerClick.Type.PICKUP), clicks.get(1));
        assertEquals(new ContainerClick(12, 0, ContainerClick.Type.PICKUP), clicks.get(2));
    }

    @Test
    void sameSlotDoesNotCreateAnUnsafeCursorAction() {
        assertTrue(ContainerClickSequence.swap(12, 12).isEmpty());
    }
}
