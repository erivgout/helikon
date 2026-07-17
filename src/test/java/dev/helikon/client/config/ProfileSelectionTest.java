package dev.helikon.client.config;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileSelectionTest {
    @Test
    void appliesTheDefaultOnlyAtStartup() {
        assertEquals("default", ProfileSelection.atStartup(Optional.of("default")).orElseThrow());
        assertTrue(ProfileSelection.atStartup(Optional.empty()).isEmpty());
    }

    @Test
    void appliesOnlyAMatchingScopedProfileOnJoin() {
        assertEquals("server", ProfileSelection.atConnection(Optional.of("server")).orElseThrow());
        assertTrue(ProfileSelection.atConnection(Optional.empty()).isEmpty());
    }
}
