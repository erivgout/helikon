package dev.helikon.client.render;

import dev.helikon.client.module.render.Waypoints;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.waypoint.WaypointLocation;
import dev.helikon.client.waypoint.WaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointNavigation;
import dev.helikon.client.waypoint.WaypointRepository;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

/** Draws the world-space beam portion of saved waypoint markers. */
public final class MinecraftWaypointRenderer {
    private final Waypoints module;
    private final WaypointRepository waypoints;
    private final WaypointLocationProvider locations;
    private final PanicState panicState;
    private WaypointLocation cachedLocation;
    private long cachedRevision = Long.MIN_VALUE;
    private int cachedMaximumMarkers = -1;
    private List<WaypointNavigation.LocatedWaypoint> cachedMarkers = List.of();

    public MinecraftWaypointRenderer(Waypoints module, WaypointRepository waypoints,
                                     WaypointLocationProvider locations, PanicState panicState) {
        this.module = Objects.requireNonNull(module, "module");
        this.waypoints = Objects.requireNonNull(waypoints, "waypoints");
        this.locations = Objects.requireNonNull(locations, "locations");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
    }

    public void render() {
        Minecraft client = Minecraft.getInstance();
        if (!module.isEnabled() || panicState.customHudHidden() || client.level == null || client.player == null) {
            clearCache();
            return;
        }
        WaypointLocation location = locations.currentLocation().orElse(null);
        if (location == null) {
            clearCache();
            return;
        }
        long revision = waypoints.revision();
        int maximumMarkers = module.maximumWaypoints();
        if (!location.equals(cachedLocation) || revision != cachedRevision
                || maximumMarkers != cachedMaximumMarkers) {
            cachedLocation = location;
            cachedRevision = revision;
            cachedMaximumMarkers = maximumMarkers;
            cachedMarkers = WaypointNavigation.nearestVisible(
                    waypoints.snapshotForContext(location.context()), location, maximumMarkers);
        }
        for (WaypointNavigation.LocatedWaypoint located : cachedMarkers) {
            draw(client, located);
        }
    }

    private void draw(Minecraft client, WaypointNavigation.LocatedWaypoint located) {
        int color = located.waypoint().color() | 0xFF000000;
        double x = located.waypoint().x() + 0.5D;
        double z = located.waypoint().z() + 0.5D;
        double beamStartY = located.waypoint().y() + 1.3D;
        double beamEndY = Math.max(beamStartY + 16.0D, client.level.getMaxY() + 16.0D);

        if (module.beams()) {
            depth(Gizmos.line(new Vec3(x, beamStartY, z), new Vec3(x, beamEndY, z),
                    color, module.lineWidth()));
        }
    }

    private void depth(GizmoProperties properties) {
        if (module.alwaysOnTop()) {
            properties.setAlwaysOnTop();
        }
    }

    private void clearCache() {
        cachedLocation = null;
        cachedRevision = Long.MIN_VALUE;
        cachedMaximumMarkers = -1;
        cachedMarkers = List.of();
    }
}
