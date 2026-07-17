package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Classifies bounded loaded-world samples as possible ordinary hostile spawn positions. */
public final class MobSpawnEsp extends Module {
    public static final double VANILLA_MINIMUM_PLAYER_DISTANCE = 24.0D;

    private final IntegerSetting horizontalRange;
    private final IntegerSetting verticalRange;
    private final IntegerSetting scanBudget;
    private final IntegerSetting sampleSpacing;
    private final IntegerSetting maximumMarkers;
    private final IntegerSetting maximumBlockLight;
    private final IntegerSetting maximumSkyLight;
    private final BooleanSetting respectPlayerDistance;
    private final BooleanSetting alwaysOnTop;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting fillColor;
    private long scanRevision;
    private Runnable cacheClearer = () -> {
    };

    public MobSpawnEsp() {
        super("mob_spawn_esp", "MobSpawnESP",
                "Highlights loaded floor positions that approximate ordinary hostile-mob spawn conditions.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        horizontalRange = addSetting(new IntegerSetting("horizontal_range", "Horizontal range",
                "Horizontal local scan radius in blocks.", 16, 8, 48));
        verticalRange = addSetting(new IntegerSetting("vertical_range", "Vertical range",
                "Vertical local scan radius around the player in blocks.", 12, 4, 32));
        scanBudget = addSetting(new IntegerSetting("scan_budget", "Scan budget",
                "Maximum loaded positions inspected per client tick.", 1_024, 64, 2_048));
        sampleSpacing = addSetting(new IntegerSetting("sample_spacing", "Sample spacing",
                "World-grid spacing between possible spawn markers.", 2, 1, 4));
        maximumMarkers = addSetting(new IntegerSetting("maximum_markers", "Maximum markers",
                "Hard cap for local spawn markers rendered per frame.", 256, 1, 512));
        maximumBlockLight = addSetting(new IntegerSetting("maximum_block_light", "Maximum block light",
                "Highest loaded block-light value considered spawnable.", 0, 0, 15));
        maximumSkyLight = addSetting(new IntegerSetting("maximum_sky_light", "Maximum sky light",
                "Highest loaded sky-light value considered spawnable.", 7, 0, 15));
        respectPlayerDistance = addSetting(new BooleanSetting("respect_player_distance", "Respect player distance",
                "Hide positions inside vanilla's 24-block natural-spawn exclusion radius.", false));
        alwaysOnTop = addSetting(new BooleanSetting("always_on_top", "Always on top",
                "Draw markers through intervening blocks.", true));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width",
                "Local spawn-marker line width.", 1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color",
                "ARGB local spawn-marker outline color.", 0xFFFF5252));
        fillColor = addSetting(new ColorSetting("fill_color", "Fill color",
                "ARGB local spawn-marker fill color.", 0x40FF5252));
        horizontalRange.addChangeListener(ignored -> scanRevision++);
        verticalRange.addChangeListener(ignored -> scanRevision++);
        sampleSpacing.addChangeListener(ignored -> scanRevision++);
        maximumBlockLight.addChangeListener(ignored -> scanRevision++);
        maximumSkyLight.addChangeListener(ignored -> scanRevision++);
        respectPlayerDistance.addChangeListener(ignored -> scanRevision++);
    }

    public int horizontalRange() {
        return horizontalRange.value();
    }

    public int verticalRange() {
        return verticalRange.value();
    }

    public int scanBudget() {
        return scanBudget.value();
    }

    public int maximumMarkers() {
        return maximumMarkers.value();
    }

    public boolean alwaysOnTop() {
        return alwaysOnTop.value();
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

    /** Cheap Minecraft-free gate used before the adapter reads block and light state. */
    public boolean samplesColumn(int blockX, int blockZ) {
        int spacing = sampleSpacing.value();
        return Math.floorMod(blockX, spacing) == 0 && Math.floorMod(blockZ, spacing) == 0;
    }

    /** Minecraft-free approximation of shared ordinary hostile spawn constraints. */
    public boolean shouldMark(SpawnSample sample) {
        if (sample == null || !samplesColumn(sample.x(), sample.z())
                || sample.blockLight() < 0 || sample.blockLight() > 15
                || sample.skyLight() < 0 || sample.skyLight() > 15
                || !Double.isFinite(sample.squaredPlayerDistance())) {
            return false;
        }
        return !sample.peacefulDifficulty()
                && sample.feetAir()
                && sample.headAir()
                && sample.validZombieFloor()
                && sample.blockLight() <= maximumBlockLight.value()
                && sample.skyLight() <= maximumSkyLight.value()
                && (!respectPlayerDistance.value()
                || sample.squaredPlayerDistance()
                >= VANILLA_MINIMUM_PLAYER_DISTANCE * VANILLA_MINIMUM_PLAYER_DISTANCE);
    }

    /** Culls results retained from an older scan anchor and optionally reapplies the distance exclusion. */
    public boolean withinCurrentRange(Marker marker, double playerX, double playerY, double playerZ) {
        if (marker == null || !Double.isFinite(playerX) || !Double.isFinite(playerY) || !Double.isFinite(playerZ)) {
            return false;
        }
        double deltaX = marker.x() + 0.5D - playerX;
        double deltaY = marker.y() - playerY;
        double deltaZ = marker.z() + 0.5D - playerZ;
        double squaredDistance = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        return Math.abs(deltaX) <= horizontalRange.value()
                && Math.abs(deltaY) <= verticalRange.value()
                && Math.abs(deltaZ) <= horizontalRange.value()
                && (!respectPlayerDistance.value()
                || squaredDistance >= VANILLA_MINIMUM_PLAYER_DISTANCE * VANILLA_MINIMUM_PLAYER_DISTANCE);
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

    /** Immutable loaded-world facts collected by the Minecraft adapter. */
    public record SpawnSample(
            int x,
            int y,
            int z,
            boolean feetAir,
            boolean headAir,
            boolean validZombieFloor,
            int blockLight,
            int skyLight,
            boolean peacefulDifficulty,
            double squaredPlayerDistance
    ) {
    }

    /** Integer block position retained by the bounded render cache. */
    public record Marker(int x, int y, int z) {
    }
}
