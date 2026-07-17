package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.BreadcrumbTrail;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;

/** Keeps a bounded client-session movement trail for local world rendering. */
public final class Breadcrumbs extends Module {
    private final NumberSetting maximumPoints;
    private final NumberSetting maximumAgeSeconds;
    private final NumberSetting samplingDistance;
    private final BooleanSetting alwaysOnTop;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final BreadcrumbTrail trail = new BreadcrumbTrail();

    public Breadcrumbs() {
        super("breadcrumbs", "Breadcrumbs", "Draws a bounded local trail of recent player positions.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        maximumPoints = addSetting(new NumberSetting("maximum_points", "Maximum points",
                "Maximum client-session trail points.", 256.0D, 8.0D, 1_024.0D));
        maximumAgeSeconds = addSetting(new NumberSetting("maximum_age_seconds", "Maximum age",
                "Oldest local trail age in seconds.", 120.0D, 1.0D, 600.0D));
        samplingDistance = addSetting(new NumberSetting("sampling_distance", "Sampling distance",
                "Minimum distance between local trail samples.", 1.0D, 0.25D, 16.0D));
        alwaysOnTop = addSetting(new BooleanSetting("always_on_top", "Always on top",
                "Render the local trail through terrain.", false));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width", "Local trail line width.",
                1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local trail color.", 0xFFB388FF));
    }

    public void sample(double x, double y, double z, long timestampMillis) {
        if (isEnabled()) {
            trail.sample(x, y, z, timestampMillis, samplingDistance.value(), maximumPoints(), maximumAgeMillis());
        }
    }

    public void clearTrail() { trail.clear(); }

    public List<BreadcrumbTrail.Point> trail() { return trail.snapshot(); }

    public Iterable<BreadcrumbTrail.Point> points() { return trail.points(); }

    public boolean alwaysOnTop() { return alwaysOnTop.value(); }

    public float lineWidth() { return (float) lineWidth.value().doubleValue(); }

    public int color() { return color.value(); }

    @Override
    protected void onDisable() {
        trail.clear();
    }

    private int maximumPoints() { return (int) Math.round(maximumPoints.value()); }

    private long maximumAgeMillis() { return Math.round(maximumAgeSeconds.value() * 1_000.0D); }
}
