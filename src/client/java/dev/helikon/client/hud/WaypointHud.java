package dev.helikon.client.hud;

import dev.helikon.client.waypoint.WaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointLocation;
import dev.helikon.client.waypoint.WaypointNavigation;
import dev.helikon.client.waypoint.WaypointRepository;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.Objects;

/** Minimal HUD indicator for the nearest enabled local waypoints. */
public final class WaypointHud implements HudElement {
    private static final int MAX_LINES = 3;

    private final WaypointRepository waypoints;
    private final WaypointLocationProvider locations;
    private final PanicState panicState;
    private final HudLayout layout;
    private WaypointLocation cachedLocation;
    private long cachedRevision = Long.MIN_VALUE;
    private List<WaypointNavigation.LocatedWaypoint> cachedNearest = List.of();

    public WaypointHud(WaypointRepository waypoints, WaypointLocationProvider locations) {
        this(waypoints, locations, new PanicState());
    }

    public WaypointHud(WaypointRepository waypoints, WaypointLocationProvider locations, PanicState panicState) {
        this(waypoints, locations, panicState, new HudLayout());
    }

    public WaypointHud(WaypointRepository waypoints, WaypointLocationProvider locations, PanicState panicState,
                       HudLayout layout) {
        this.waypoints = Objects.requireNonNull(waypoints, "waypoints");
        this.locations = Objects.requireNonNull(locations, "locations");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!layout.element(HudElementId.WAYPOINTS).enabled() || panicState.customHudHidden()) {
            clearCache();
            return;
        }
        locations.currentLocation().ifPresentOrElse(location -> {
            long revision = waypoints.revision();
            if (!location.equals(cachedLocation) || revision != cachedRevision) {
                cachedLocation = location;
                cachedRevision = revision;
                cachedNearest = WaypointNavigation.nearestVisible(
                        waypoints.snapshotForContext(location.context()), location, MAX_LINES);
            }
            draw(graphics, Minecraft.getInstance().font, cachedNearest, layout.element(HudElementId.WAYPOINTS));
        }, this::clearCache);
    }

    private void clearCache() {
        cachedLocation = null;
        cachedRevision = Long.MIN_VALUE;
        cachedNearest = List.of();
    }

    private static void draw(GuiGraphicsExtractor graphics, Font font, List<WaypointNavigation.LocatedWaypoint> located,
                             HudElementPlacement placement) {
        if (located.isEmpty()) {
            return;
        }
        HudPresentation.drawLines(graphics, font, located.stream().map(WaypointHud::line).toList(), placement);
    }

    private static String line(WaypointNavigation.LocatedWaypoint waypoint) {
        String icon = waypoint.waypoint().icon().isEmpty() ? "" : "[" + waypoint.waypoint().icon() + "] ";
        return icon + waypoint.waypoint().name() + " " + waypoint.distance() + "m " + waypoint.direction();
    }
}
