package dev.helikon.client.command;

import dev.helikon.client.waypoint.Waypoint;
import dev.helikon.client.waypoint.WaypointLocation;
import dev.helikon.client.waypoint.WaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointNavigation;
import dev.helikon.client.waypoint.WaypointRepository;

import java.util.List;
import java.util.Objects;

/** Helikon command facade over Baritone's per-world waypoint collection. */
public final class WaypointCommand implements HelikonCommand {
    private final WaypointRepository waypoints;
    private final WaypointLocationProvider locations;

    public WaypointCommand(WaypointRepository waypoints, WaypointLocationProvider locations) {
        this.waypoints = Objects.requireNonNull(waypoints, "waypoints");
        this.locations = Objects.requireNonNull(locations, "locations");
    }

    @Override
    public String name() {
        return "waypoint";
    }

    @Override
    public String usage() {
        return ".waypoint list|add <name> [x y z]|remove <name>|rename <from> <to>";
    }

    @Override
    public String description() {
        return "Manages Baritone waypoints for the current world and dimension.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.isEmpty()) {
            feedback.error("Usage: " + usage());
            return;
        }
        try {
            switch (arguments.get(0)) {
                case "list" -> list(arguments, feedback);
                case "add" -> add(arguments, feedback);
                case "remove" -> remove(arguments, feedback);
                case "rename" -> rename(arguments, feedback);
                default -> feedback.error("Usage: " + usage());
            }
        } catch (RuntimeException exception) {
            feedback.error("Waypoint action failed: " + exception.getMessage());
        }
    }

    private void list(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: .waypoint list");
            return;
        }
        WaypointLocation location = requireLocation(feedback);
        if (location == null) {
            return;
        }
        List<WaypointNavigation.LocatedWaypoint> visible = WaypointNavigation.visibleSorted(
                waypoints.visible(location.context()), location);
        if (visible.isEmpty()) {
            feedback.info("No Baritone waypoints in this world and dimension.");
            return;
        }
        String entries = visible.stream()
                .map(waypoint -> waypoint.waypoint().name() + " [" + waypoint.waypoint().icon() + ", "
                        + waypoint.distance() + "m " + waypoint.direction() + "]")
                .collect(java.util.stream.Collectors.joining(", "));
        feedback.info("Waypoints: " + entries);
    }

    private void add(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2 && arguments.size() != 5) {
            feedback.error("Usage: .waypoint add <name> [x y z]");
            return;
        }
        WaypointLocation current = requireLocation(feedback);
        if (current == null) {
            return;
        }
        int x = current.x();
        int y = current.y();
        int z = current.z();
        if (arguments.size() == 5) {
            x = parseCoordinate(arguments.get(2));
            y = parseCoordinate(arguments.get(3));
            z = parseCoordinate(arguments.get(4));
        }
        if (!waypoints.addAndSave(arguments.get(1), x, y, z, current.context())) {
            feedback.error("A Baritone waypoint named '" + arguments.get(1) + "' already exists here.");
            return;
        }
        feedback.info("Added Baritone waypoint '" + Waypoint.requireName(arguments.get(1))
                + "' at " + x + ", " + y + ", " + z + ".");
    }

    private void remove(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) {
            feedback.error("Usage: .waypoint remove <name>");
            return;
        }
        WaypointLocation location = requireLocation(feedback);
        if (location == null) {
            return;
        }
        if (!waypoints.removeAndSave(arguments.get(1), location.context())) {
            feedback.error("No Baritone waypoint named '" + arguments.get(1) + "' exists here.");
            return;
        }
        feedback.info("Removed Baritone waypoint '" + arguments.get(1) + "'.");
    }

    private void rename(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 3) {
            feedback.error("Usage: .waypoint rename <from> <to>");
            return;
        }
        WaypointLocation location = requireLocation(feedback);
        if (location == null) {
            return;
        }
        if (!waypoints.renameAndSave(arguments.get(1), arguments.get(2), location.context())) {
            feedback.error("No Baritone waypoint named '" + arguments.get(1) + "' exists here.");
            return;
        }
        feedback.info("Renamed Baritone waypoint '" + arguments.get(1) + "' to '" + arguments.get(2) + "'.");
    }

    private WaypointLocation requireLocation(CommandFeedback feedback) {
        return locations.currentLocation().orElseGet(() -> {
            feedback.error("Waypoints are available only while a local world or server is loaded.");
            return null;
        });
    }

    private static int parseCoordinate(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Coordinates must be whole numbers");
        }
    }

}
