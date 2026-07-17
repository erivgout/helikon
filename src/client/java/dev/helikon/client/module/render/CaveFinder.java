package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Detects bounded samples of already-loaded, walkable underground air space. */
public final class CaveFinder extends Module {
    private final IntegerSetting minimumDepth;
    private final IntegerSetting scanDepth;
    private final IntegerSetting horizontalRange;
    private final IntegerSetting scanBudget;
    private final IntegerSetting sampleSpacing;
    private final IntegerSetting minimumOpenSides;
    private final BooleanSetting requireFloor;
    private final BooleanSetting requireBuried;
    private final IntegerSetting maximumMarkers;
    private final BooleanSetting tracers;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting fillColor;
    private long scanRevision;
    private Runnable cacheClearer = () -> {
    };

    public CaveFinder() {
        super("cave_finder", "CaveFinder", "Highlights sampled walkable air pockets in loaded terrain below you.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        minimumDepth = addSetting(new IntegerSetting("minimum_depth", "Minimum depth",
                "Minimum number of blocks below the local player for a cave marker.", 6, 2, 24));
        scanDepth = addSetting(new IntegerSetting("scan_depth", "Scan depth",
                "Maximum number of blocks below the local player to inspect.", 48, 24, 64));
        horizontalRange = addSetting(new IntegerSetting("horizontal_range", "Horizontal range",
                "Horizontal local scan radius in blocks.", 32, 8, 64));
        scanBudget = addSetting(new IntegerSetting("scan_budget", "Scan budget",
                "Maximum loaded positions inspected per client tick.", 1_024, 64, 2_048));
        sampleSpacing = addSetting(new IntegerSetting("sample_spacing", "Sample spacing",
                "World-grid spacing between possible cave markers.", 4, 1, 8));
        minimumOpenSides = addSetting(new IntegerSetting("minimum_open_sides", "Minimum open sides",
                "Horizontal two-block-high exits required for a sampled air pocket.", 2, 1, 4));
        requireFloor = addSetting(new BooleanSetting("require_floor", "Require floor",
                "Require a non-empty collision shape beneath the sampled air pocket.", true));
        requireBuried = addSetting(new BooleanSetting("require_buried", "Require buried",
                "Reject sampled air pockets at or above the local motion-blocking heightmap.", true));
        maximumMarkers = addSetting(new IntegerSetting("maximum_markers", "Maximum markers",
                "Hard cap for local cave markers rendered per frame.", 128, 1, 512));
        tracers = addSetting(new BooleanSetting("tracers", "Tracers",
                "Draw local camera-to-cave-marker lines.", true));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width",
                "Local marker and tracer line width.", 1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color",
                "ARGB local cave-marker outline and tracer color.", 0xFF4DD0E1));
        fillColor = addSetting(new ColorSetting("fill_color", "Fill color",
                "ARGB local cave-marker fill color.", 0x304DD0E1));
        minimumDepth.addChangeListener(ignored -> scanRevision++);
        scanDepth.addChangeListener(ignored -> scanRevision++);
        horizontalRange.addChangeListener(ignored -> scanRevision++);
        sampleSpacing.addChangeListener(ignored -> scanRevision++);
        minimumOpenSides.addChangeListener(ignored -> scanRevision++);
        requireFloor.addChangeListener(ignored -> scanRevision++);
        requireBuried.addChangeListener(ignored -> scanRevision++);
    }

    public int minimumDepth() {
        return minimumDepth.value();
    }

    public int scanDepth() {
        return scanDepth.value();
    }

    public int horizontalRange() {
        return horizontalRange.value();
    }

    public int scanBudget() {
        return scanBudget.value();
    }

    public int maximumMarkers() {
        return maximumMarkers.value();
    }

    public boolean tracersEnabled() {
        return tracers.value();
    }

    public float lineWidth() {
        return (float) lineWidth.value().doubleValue();
    }

    public int color() {
        return color.value();
    }

    public int fillColor() {
        return fillColor.value();
    }

    public long scanRevision() {
        return scanRevision;
    }

    /** Cheap Minecraft-free gate used before the adapter reads neighboring blocks. */
    public boolean samplesColumn(int blockX, int blockZ) {
        int spacing = sampleSpacing.value();
        return Math.floorMod(blockX, spacing) == 0 && Math.floorMod(blockZ, spacing) == 0;
    }

    /** Minecraft-free cave classification from facts collected by the thin client adapter. */
    public boolean shouldMark(CaveSample sample) {
        if (sample == null || !samplesColumn(sample.x(), sample.z())) {
            return false;
        }
        int depth = sample.playerBlockY() - sample.y();
        return depth >= minimumDepth.value()
                && depth <= scanDepth.value()
                && sample.feetAir()
                && sample.headAir()
                && sample.openSides() >= minimumOpenSides.value()
                && (!requireFloor.value() || sample.floorCollision())
                && (!requireBuried.value() || sample.belowSurface());
    }

    /** Culls cached results that belonged to an older nearby scan region. */
    public boolean withinCurrentRange(CaveSample sample, double playerX, double playerY, double playerZ) {
        if (sample == null || !Double.isFinite(playerX) || !Double.isFinite(playerY)
                || !Double.isFinite(playerZ)) {
            return false;
        }
        int depth = sample.playerBlockY() - sample.y();
        return Math.abs(sample.x() + 0.5D - playerX) <= horizontalRange.value()
                && Math.abs(sample.z() + 0.5D - playerZ) <= horizontalRange.value()
                && depth >= minimumDepth.value()
                && depth <= scanDepth.value();
    }

    /** Installs the thin local render-cache cleanup hook without introducing Minecraft types. */
    public void setCacheClearer(Runnable cacheClearer) {
        this.cacheClearer = Objects.requireNonNull(cacheClearer, "cacheClearer");
    }

    @Override
    protected void onDisable() {
        scanRevision++;
        cacheClearer.run();
    }

    /** Immutable world facts used only by the Minecraft-free cave decision. */
    public record CaveSample(
            int x,
            int y,
            int z,
            int playerBlockY,
            boolean feetAir,
            boolean headAir,
            boolean floorCollision,
            int openSides,
            boolean belowSurface
    ) {
    }
}
