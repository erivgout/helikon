package dev.helikon.client.command;

import dev.helikon.client.waypoint.WaypointContext;
import dev.helikon.client.waypoint.WaypointLocation;
import dev.helikon.client.waypoint.WaypointManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointCommandTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void managesCurrentContextWaypointsWithoutServerCommands() {
        WaypointManager waypoints = new WaypointManager(temporaryDirectory.resolve("helikon"));
        WaypointLocation location = new WaypointLocation(4, 65, -2,
                new WaypointContext("world:command-test", "minecraft:overworld"));
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.register(new WaypointCommand(waypoints, () -> Optional.of(location)));
        RecordingFeedback feedback = new RecordingFeedback();

        assertTrue(dispatcher.dispatch(".waypoint add Home", feedback));
        assertTrue(dispatcher.dispatch(".waypoint add Mine 10 70 -5", feedback));
        assertTrue(dispatcher.dispatch(".waypoint color mine #123456", feedback));
        assertTrue(dispatcher.dispatch(".waypoint icon mine pickaxe", feedback));
        assertTrue(dispatcher.dispatch(".waypoint toggle mine", feedback));
        assertTrue(dispatcher.dispatch(".waypoint list", feedback));
        assertTrue(dispatcher.dispatch(".waypoint rename home Base", feedback));
        assertTrue(dispatcher.dispatch(".waypoint remove base", feedback));

        assertEquals(0xFF123456, waypoints.find("mine", location.context()).orElseThrow().color());
        assertEquals("pickaxe", waypoints.find("mine", location.context()).orElseThrow().icon());
        assertTrue(feedback.infos.stream().anyMatch(line -> line.contains("Disabled: Mine")));
        assertTrue(feedback.infos.stream().anyMatch(line -> line.contains("Renamed local waypoint")));
    }

    @Test
    void requiresALoadedWorldContext() {
        WaypointManager waypoints = new WaypointManager(temporaryDirectory.resolve("helikon"));
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.register(new WaypointCommand(waypoints, Optional::<WaypointLocation>empty));
        RecordingFeedback feedback = new RecordingFeedback();

        dispatcher.dispatch(".waypoint add Home", feedback);

        assertTrue(feedback.errors.get(0).contains("only while a local world or server is loaded"));
    }
}
