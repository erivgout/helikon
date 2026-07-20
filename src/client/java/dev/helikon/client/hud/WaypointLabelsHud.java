package dev.helikon.client.hud;

import dev.helikon.client.module.render.Waypoints;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.render.ArrowProjection;
import dev.helikon.client.waypoint.WaypointLocation;
import dev.helikon.client.waypoint.WaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointMarkerPresentation;
import dev.helikon.client.waypoint.WaypointNavigation;
import dev.helikon.client.waypoint.WaypointRepository;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

/**
 * Projects Waypoints' labels into GUI space so distant destination text stays
 * readable even when Minecraft culls a ground-anchored world text gizmo.
 */
public final class WaypointLabelsHud implements HudElement {
    private static final int PANEL_COLOR = 0xD014161B;
    private static final int TEXT_COLOR = 0xFFF4F7FA;
    private static final int PADDING = 3;

    private final Waypoints module;
    private final WaypointRepository waypoints;
    private final WaypointLocationProvider locations;
    private final PanicState panicState;
    private WaypointLocation cachedLocation;
    private long cachedRevision = Long.MIN_VALUE;
    private int cachedMaximumMarkers = -1;
    private List<WaypointNavigation.LocatedWaypoint> cachedMarkers = List.of();

    public WaypointLabelsHud(Waypoints module, WaypointRepository waypoints,
                             WaypointLocationProvider locations, PanicState panicState) {
        this.module = Objects.requireNonNull(module, "module");
        this.waypoints = Objects.requireNonNull(waypoints, "waypoints");
        this.locations = Objects.requireNonNull(locations, "locations");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (!module.isEnabled() || !module.labels() || panicState.customHudHidden()
                || client.level == null || client.player == null) {
            clearCache();
            return;
        }
        WaypointLocation location = locations.currentLocation().orElse(null);
        if (location == null) {
            clearCache();
            return;
        }
        refresh(location);
        Camera camera = client.gameRenderer.mainCamera();
        Vec3 cameraPosition = camera.position();
        for (WaypointNavigation.LocatedWaypoint located : cachedMarkers) {
            WaypointMarkerPresentation.Marker marker = WaypointMarkerPresentation.marker(located);
            double x = located.waypoint().x() + 0.5D;
            double z = located.waypoint().z() + 0.5D;
            double y = WaypointMarkerPresentation.labelY(
                    located.waypoint().y(), cameraPosition.y, located.distance());
            Vec3 anchor = new Vec3(x, y, z);
            Vec3 delta = anchor.subtract(cameraPosition);
            if (ArrowProjection.project(delta.x, delta.y, delta.z, camera.yRot(), camera.xRot(), 179.0D).outside()) {
                continue;
            }
            Vec3 projected = client.gameRenderer.projectPointToScreen(anchor);
            if (!visible(projected)) {
                continue;
            }
            draw(graphics, client, marker, projected, located.waypoint().color() | 0xFF000000);
        }
    }

    private void refresh(WaypointLocation location) {
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
    }

    private void draw(GuiGraphicsExtractor graphics, Minecraft client,
                      WaypointMarkerPresentation.Marker marker, Vec3 projected, int color) {
        String icon = marker.icon();
        String label = marker.label();
        int iconWidth = client.font.width(icon);
        int gap = 2;
        int contentWidth = iconWidth + gap + client.font.width(label);
        int width = contentWidth + PADDING * 2;
        int height = client.font.lineHeight + PADDING * 2;
        float scale = marker.scale() * module.scale();
        int scaledWidth = Math.max(1, Math.round(width * scale));
        int scaledHeight = Math.max(1, Math.round(height * scale));
        int centerX = (int) Math.round((projected.x + 1.0D) * 0.5D * graphics.guiWidth());
        int centerY = (int) Math.round((1.0D - projected.y) * 0.5D * graphics.guiHeight());
        int x = Math.clamp(centerX - scaledWidth / 2, 2, Math.max(2, graphics.guiWidth() - scaledWidth - 2));
        int y = Math.clamp(centerY - scaledHeight / 2, 2, Math.max(2, graphics.guiHeight() - scaledHeight - 2));

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale);
        graphics.fill(0, 0, width, height, PANEL_COLOR);
        graphics.outline(0, 0, width, height, color);
        graphics.text(client.font, icon, PADDING, PADDING, color, true);
        graphics.text(client.font, label, PADDING + iconWidth + gap, PADDING, TEXT_COLOR, true);
        graphics.pose().popMatrix();
    }

    private static boolean visible(Vec3 projected) {
        return Double.isFinite(projected.x) && Double.isFinite(projected.y) && Double.isFinite(projected.z)
                && projected.x >= -1.05D && projected.x <= 1.05D
                && projected.y >= -1.05D && projected.y <= 1.05D;
    }

    private void clearCache() {
        cachedLocation = null;
        cachedRevision = Long.MIN_VALUE;
        cachedMaximumMarkers = -1;
        cachedMarkers = List.of();
    }
}
