package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.waypoint.WaypointContext;
import dev.helikon.client.waypoint.WaypointLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinateTrackerTest {
    @Test
    void recordsOnlyEnabledLifecycleModulesAgainstTheLastObservedLocation() {
        CoordinateTracker tracker = new CoordinateTracker();
        DeathCoordinates death = new DeathCoordinates();
        LogoutCoordinates logout = new LogoutCoordinates();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(death);
        registry.register(logout);

        tracker.observe(new WaypointLocation(12, 64, -7,
                new WaypointContext("server:example.net", "minecraft:overworld")));
        assertTrue(death.record(tracker).isEmpty());

        registry.setEnabled(death, true);
        CoordinateEntry deathEntry = death.record(tracker).orElseThrow();
        assertEquals("Death: 12, 64, -7 (minecraft:overworld)", deathEntry.displayText());

        registry.setEnabled(logout, true);
        tracker.observe(new WaypointLocation(-3, 70, 19,
                new WaypointContext("world:local", "minecraft:the_nether")));
        CoordinateEntry logoutEntry = logout.record(tracker).orElseThrow();
        assertEquals(CoordinateKind.LOGOUT, logoutEntry.kind());
        assertEquals(-3, tracker.latest(CoordinateKind.LOGOUT).orElseThrow().location().x());
        assertTrue(tracker.latestForScope(CoordinateKind.LOGOUT, "world:local").isPresent());
        assertTrue(tracker.latestForScope(CoordinateKind.LOGOUT, "server:example.net").isEmpty());
    }

    @Test
    void refusesToRecordBeforeAnySafeLocationWasObserved() {
        CoordinateTracker tracker = new CoordinateTracker();
        assertTrue(tracker.record(CoordinateKind.DEATH).isEmpty());
        tracker.clearObservedLocation();
        assertTrue(tracker.record(CoordinateKind.LOGOUT).isEmpty());
    }
}
