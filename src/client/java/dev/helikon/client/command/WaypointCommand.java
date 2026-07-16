package dev.helikon.client.command;

import dev.helikon.client.waypoint.Waypoint;
import dev.helikon.client.waypoint.WaypointLocation;
import dev.helikon.client.waypoint.WaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointManager;
import dev.helikon.client.waypoint.WaypointNavigation;

import java.util.List;
import java.util.Objects;

/** Local waypoint management with current-world filtering and no server traffic. */
public final class WaypointCommand implements HelikonCommand {
    private final WaypointManager waypoints;
    private final WaypointLocationProvider locations;

    public WaypointCommand(WaypointManager waypoints, WaypointLocationProvider locations) {
        this.waypoints = Objects.requireNonNull(waypoints, "waypoints");
        this.locations = Objects.requireNonNull(locations, "locations");
    }

    @Override
    public String name() {
        return "waypoint";
    }

    @Override
    public String usage() {
        return ".waypoint list|add <name> [x y z]|remove <name>|rename <from> <to>|toggle <name>|color <name> <#RRGGBB|#AARRGGBB>|icon <name> <icon|clear>";
    }

    @Override
    public String description() {
        return "Manages local waypoints for the current server/world and dimension.";
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
                case "toggle" -> toggle(arguments, feedback);
                case "color" -> color(arguments, feedback);
                case "icon" -> icon(arguments, feedback);
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
        List<Waypoint> hidden = waypoints.forContext(location.context()).stream()
                .filter(waypoint -> !waypoint.enabled())
                .toList();
        if (visible.isEmpty() && hidden.isEmpty()) {
            feedback.info("No local waypoints in this world and dimension.");
            return;
        }
        String entries = visible.stream()
                .map(waypoint -> waypoint.waypoint().name() + " [on, " + waypoint.distance() + "m " + waypoint.direction() + "]")
                .collect(java.util.stream.Collectors.joining(", "));
        if (!hidden.isEmpty()) {
            String disabled = hidden.stream().map(Waypoint::name)
                    .collect(java.util.stream.Collectors.joining(", "));
            entries = entries.isEmpty() ? "Disabled: " + disabled : entries + "; Disabled: " + disabled;
        }
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
            feedback.error("A local waypoint named '" + arguments.get(1) + "' already exists here.");
            return;
        }
        feedback.info("Added local waypoint '" + Waypoint.requireName(arguments.get(1)) + "' at " + x + ", " + y + ", " + z + ".");
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
            feedback.error("No local waypoint named '" + arguments.get(1) + "' exists here.");
            return;
        }
        feedback.info("Removed local waypoint '" + arguments.get(1) + "'.");
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
            feedback.error("No local waypoint named '" + arguments.get(1) + "' exists here.");
            return;
        }
        feedback.info("Renamed local waypoint '" + arguments.get(1) + "' to '" + arguments.get(2) + "'.");
    }

    private void toggle(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) {
            feedback.error("Usage: .waypoint toggle <name>");
            return;
        }
        WaypointLocation location = requireLocation(feedback);
        if (location == null) {
            return;
        }
        var enabled = waypoints.toggleAndSave(arguments.get(1), location.context());
        if (enabled.isEmpty()) {
            feedback.error("No local waypoint named '" + arguments.get(1) + "' exists here.");
            return;
        }
        feedback.info((enabled.orElseThrow() ? "Enabled" : "Disabled") + " local waypoint '" + arguments.get(1) + "'.");
    }

    private void color(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 3) {
            feedback.error("Usage: .waypoint color <name> <#RRGGBB|#AARRGGBB>");
            return;
        }
        WaypointLocation location = requireLocation(feedback);
        if (location == null) {
            return;
        }
        if (!waypoints.setColorAndSave(arguments.get(1), location.context(), parseColor(arguments.get(2)))) {
            feedback.error("No local waypoint named '" + arguments.get(1) + "' exists here.");
            return;
        }
        feedback.info("Updated local waypoint color for '" + arguments.get(1) + "'.");
    }

    private void icon(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 3) {
            feedback.error("Usage: .waypoint icon <name> <icon|clear>");
            return;
        }
        WaypointLocation location = requireLocation(feedback);
        if (location == null) {
            return;
        }
        String icon = arguments.get(2).equalsIgnoreCase("clear") ? Waypoint.NO_ICON : arguments.get(2);
        if (!waypoints.setIconAndSave(arguments.get(1), location.context(), icon)) {
            feedback.error("No local waypoint named '" + arguments.get(1) + "' exists here.");
            return;
        }
        feedback.info("Updated local waypoint icon for '" + arguments.get(1) + "'.");
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

    private static int parseColor(String text) {
        String hex = text == null ? "" : text.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (!hex.matches("[0-9a-fA-F]{6}|[0-9a-fA-F]{8}")) {
            throw new IllegalArgumentException("Color must be #RRGGBB or #AARRGGBB");
        }
        long value = Long.parseLong(hex, 16);
        return hex.length() == 6 ? (int) (0xFF000000L | value) : (int) value;
    }
}
