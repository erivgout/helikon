package dev.helikon.client.hud;

import dev.helikon.client.module.miscellaneous.CoordinateEntry;
import dev.helikon.client.module.miscellaneous.CoordinateKind;
import dev.helikon.client.module.miscellaneous.CoordinateTracker;
import dev.helikon.client.module.miscellaneous.DeathCoordinates;
import dev.helikon.client.module.miscellaneous.LogoutCoordinates;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.waypoint.WaypointLocationProvider;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Displays the enabled session-local death and logout coordinate snapshots without creating waypoints. */
public final class CoordinateHud implements HudElement {
    private static final int X = 5;
    private static final int PADDING = 3;

    private final DeathCoordinates deathCoordinates;
    private final LogoutCoordinates logoutCoordinates;
    private final CoordinateTracker tracker;
    private final WaypointLocationProvider locations;
    private final PanicState panicState;
    private final HudLayout layout;

    public CoordinateHud(DeathCoordinates deathCoordinates, LogoutCoordinates logoutCoordinates,
                         CoordinateTracker tracker, WaypointLocationProvider locations, PanicState panicState) {
        this(deathCoordinates, logoutCoordinates, tracker, locations, panicState, new HudLayout());
    }

    public CoordinateHud(DeathCoordinates deathCoordinates, LogoutCoordinates logoutCoordinates,
                         CoordinateTracker tracker, WaypointLocationProvider locations, PanicState panicState,
                         HudLayout layout) {
        this.deathCoordinates = Objects.requireNonNull(deathCoordinates, "deathCoordinates");
        this.logoutCoordinates = Objects.requireNonNull(logoutCoordinates, "logoutCoordinates");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.locations = Objects.requireNonNull(locations, "locations");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        HudElementPlacement placement = layout.element(HudElementId.COORDINATES);
        if (!placement.enabled() || panicState.customHudHidden()) {
            return;
        }
        List<String> lines = locations.currentLocation().map(location -> lines(location.context().scope())).orElseGet(List::of);
        if (lines.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        HudPresentation.drawLines(graphics, client.font, lines, placement);
    }

    private List<String> lines(String scope) {
        List<String> lines = new ArrayList<>(2);
        if (deathCoordinates.isEnabled()) {
            tracker.latestForScope(CoordinateKind.DEATH, scope).map(CoordinateEntry::displayText).ifPresent(lines::add);
        }
        if (logoutCoordinates.isEnabled()) {
            tracker.latestForScope(CoordinateKind.LOGOUT, scope).map(CoordinateEntry::displayText).ifPresent(lines::add);
        }
        return List.copyOf(lines);
    }
}
