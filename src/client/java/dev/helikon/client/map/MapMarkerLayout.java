package dev.helikon.client.map;

import dev.helikon.client.waypoint.Waypoint;
import dev.helikon.client.waypoint.WaypointContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Bounded Minecraft-free waypoint projection for the full-screen map. */
public final class MapMarkerLayout {
    public static final int MAXIMUM_MARKERS = 512;
    private static final double HOVER_RADIUS = 5.0D;

    private MapMarkerLayout() {
    }

    public static List<Marker> layout(List<Waypoint> waypoints, WaypointContext context, MapViewport viewport,
                                      int width, int height, double mouseX, double mouseY) {
        Objects.requireNonNull(waypoints, "waypoints");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(viewport, "viewport");
        List<Marker> markers = new ArrayList<>();
        for (Waypoint waypoint : waypoints) {
            if (markers.size() >= MAXIMUM_MARKERS || !waypoint.context().equals(context)) {
                continue;
            }
            MapViewport.ScreenPoint point = viewport.worldToScreen(
                    waypoint.x() + 0.5D, waypoint.z() + 0.5D, width, height);
            if (point.x() < -HOVER_RADIUS || point.x() > width + HOVER_RADIUS
                    || point.y() < -HOVER_RADIUS || point.y() > height + HOVER_RADIUS) {
                continue;
            }
            boolean hovered = distanceSquared(point.x(), point.y(), mouseX, mouseY)
                    <= HOVER_RADIUS * HOVER_RADIUS;
            String icon = waypoint.icon().equals("death") ? "X"
                    : waypoint.name().substring(0, 1).toUpperCase(Locale.ROOT);
            markers.add(new Marker(waypoint.name(), icon, point.x(), point.y(), waypoint.color() | 0xFF000000,
                    hovered || viewport.pixelsPerBlock() >= 1.0D, hovered));
        }
        return List.copyOf(markers);
    }

    private static double distanceSquared(double x, double y, double otherX, double otherY) {
        double deltaX = x - otherX;
        double deltaY = y - otherY;
        return deltaX * deltaX + deltaY * deltaY;
    }

    public record Marker(String name, String icon, double screenX, double screenY, int color,
                         boolean labelVisible, boolean hovered) {
        public Marker {
            name = Objects.requireNonNull(name, "name");
            icon = Objects.requireNonNull(icon, "icon");
        }
    }
}

