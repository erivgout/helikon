package dev.helikon.client.integration.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseVersionTest {
    @Test
    void comparesStableVersionsAndOptionalVPrefix() {
        assertTrue(ReleaseVersion.parse("v1.6.0").compareTo(ReleaseVersion.parse("1.5.12")) > 0);
        assertEquals(0, ReleaseVersion.parse("1.5.2+build.7").compareTo(ReleaseVersion.parse("v1.5.2")));
    }

    @Test
    void followsSemanticPreReleaseOrdering() {
        assertTrue(ReleaseVersion.parse("1.5.2").compareTo(ReleaseVersion.parse("1.5.2-rc.1")) > 0);
        assertTrue(ReleaseVersion.parse("1.5.2-rc.2").compareTo(ReleaseVersion.parse("1.5.2-rc.1")) > 0);
        assertTrue(ReleaseVersion.parse("1.5.2-rc.10").compareTo(ReleaseVersion.parse("1.5.2-rc.2")) > 0);
    }

    @Test
    void rejectsMalformedOrOversizedVersions() {
        assertThrows(IllegalArgumentException.class, () -> ReleaseVersion.parse("latest"));
        assertThrows(IllegalArgumentException.class, () -> ReleaseVersion.parse("1.2"));
        assertThrows(IllegalArgumentException.class, () -> ReleaseVersion.parse("999999999999.2.3"));
    }
}
