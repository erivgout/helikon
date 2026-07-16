package dev.helikon.client.hud;

import dev.helikon.client.waypoint.WaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointLocation;
import dev.helikon.client.waypoint.WaypointManager;
import dev.helikon.client.waypoint.WaypointNavigation;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.Objects;

/** Minimal HUD indicator for the nearest enabled local waypoints. */
public final class WaypointHud implements HudElement {
    private static final int X = 5;
    private static final int Y = 50;
    private static final int PADDING = 3;
    private static final int BACKGROUND = 0xB014161B;
    private static final int MAX_LINES = 3;

    private final WaypointManager waypoints;
    private final WaypointLocationProvider locations;
    private WaypointLocation cachedLocation;
    private long cachedRevision = Long.MIN_VALUE;
    private List<WaypointNavigation.LocatedWaypoint> cachedNearest = List.of();

    public WaypointHud(WaypointManager waypoints, WaypointLocationProvider locations) {
        this.waypoints = Objects.requireNonNull(waypoints, "waypoints");
        this.locations = Objects.requireNonNull(locations, "locations");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        locations.currentLocation().ifPresentOrElse(location -> {
            long revision = waypoints.revision();
            if (!location.equals(cachedLocation) || revision != cachedRevision) {
                cachedLocation = location;
                cachedRevision = revision;
                cachedNearest = WaypointNavigation.nearestVisible(
                        waypoints.snapshotForContext(location.context()), location, MAX_LINES);
            }
            draw(graphics, Minecraft.getInstance().font, cachedNearest);
        }, this::clearCache);
    }

    private void clearCache() {
        cachedLocation = null;
        cachedRevision = Long.MIN_VALUE;
        cachedNearest = List.of();
    }

    private static void draw(GuiGraphicsExtractor graphics, Font font, List<WaypointNavigation.LocatedWaypoint> located) {
        int lineCount = located.size();
        if (lineCount == 0) {
            return;
        }
        int width = 0;
        for (int index = 0; index < lineCount; index++) {
            width = Math.max(width, font.width(line(located.get(index))));
        }
        int height = lineCount * font.lineHeight + PADDING * 2;
        graphics.fill(X, Y, X + width + PADDING * 2, Y + height, BACKGROUND);
        for (int index = 0; index < lineCount; index++) {
            WaypointNavigation.LocatedWaypoint waypoint = located.get(index);
            graphics.text(font, line(waypoint), X + PADDING, Y + PADDING + index * font.lineHeight,
                    waypoint.waypoint().color(), true);
        }
    }

    private static String line(WaypointNavigation.LocatedWaypoint waypoint) {
        String icon = waypoint.waypoint().icon().isEmpty() ? "" : "[" + waypoint.waypoint().icon() + "] ";
        return icon + waypoint.waypoint().name() + " " + waypoint.distance() + "m " + waypoint.direction();
    }
}
