package dev.helikon.client.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaritoneCompatibilityTest {
    @Test
    void detectsTheEmbeddedBaritoneFabricModId() {
        assertTrue(BaritoneCompatibility.detect(BaritoneCompatibility.MOD_ID::equals).detected());
        assertFalse(BaritoneCompatibility.detect(ignored -> false).detected());
    }
}
