package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

/** Configures Lunar-inspired world markers for saved Baritone waypoints. */
public final class Waypoints extends Module {
    private final BooleanSetting labels;
    private final BooleanSetting beams;
    private final BooleanSetting alwaysOnTop;
    private final IntegerSetting maximumWaypoints;
    private final NumberSetting scale;
    private final NumberSetting lineWidth;

    public Waypoints() {
        super("waypoints", "Waypoints",
                "Renders saved waypoints as colored initials, distance labels, and vertical beams.",
                ModuleCategory.RENDER, true, Keybind.unbound());
        labels = addSetting(new BooleanSetting("labels", "Labels",
                "Show each waypoint's colored initial, name, and distance.", true));
        beams = addSetting(new BooleanSetting("beams", "Beams",
                "Draw a thin colored vertical beam above each waypoint.", true));
        alwaysOnTop = addSetting(new BooleanSetting("always_on_top", "Always on top",
                "Keep waypoint markers visible through terrain.", true));
        maximumWaypoints = addSetting(new IntegerSetting("maximum_waypoints", "Maximum waypoints",
                "Hard cap for nearest current-dimension world markers.", 64, 1, 128));
        scale = addSetting(new NumberSetting("scale", "Scale",
                "Multiplier for projected label size and its screen-relative ceiling.", 1.0D, 0.5D, 2.0D));
        lineWidth = addSetting(new NumberSetting("line_width", "Beam width",
                "Width of waypoint beams.", 1.0D, 0.5D, 4.0D));
    }

    public boolean labels() {
        return labels.value();
    }

    public boolean beams() {
        return beams.value();
    }

    public boolean alwaysOnTop() {
        return alwaysOnTop.value();
    }

    public int maximumWaypoints() {
        return maximumWaypoints.value();
    }

    public float scale() {
        return scale.value().floatValue();
    }

    public float lineWidth() {
        return lineWidth.value().floatValue();
    }
}
