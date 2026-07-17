package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.BlockIdList;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Finds bounded clusters of configured, already-loaded blocks that can indicate a player-built base. */
public final class BaseFinder extends Module {
    private final StringSetting evidenceBlocks;
    private final IntegerSetting minimumEvidence;
    private final IntegerSetting clusterRadius;
    private final IntegerSetting horizontalRange;
    private final IntegerSetting verticalRange;
    private final IntegerSetting scanBudget;
    private final IntegerSetting maximumMarkers;
    private final BooleanSetting tracers;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting fillColor;
    private Set<String> targetBlocks;
    private long scanRevision;
    private Runnable cacheClearer = () -> {
    };

    public BaseFinder() {
        super("base_finder", "BaseFinder", "Highlights loaded clusters of configurable player-built-base evidence.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        evidenceBlocks = addSetting(new StringSetting("evidence_blocks", "Evidence blocks",
                "Semicolon-separated block IDs treated as possible base evidence.",
                "minecraft:chest;minecraft:trapped_chest;minecraft:ender_chest;minecraft:barrel;"
                        + "minecraft:crafting_table;minecraft:furnace;minecraft:blast_furnace;minecraft:smoker;"
                        + "minecraft:brewing_stand;minecraft:enchanting_table;minecraft:anvil;"
                        + "minecraft:chipped_anvil;minecraft:damaged_anvil;minecraft:hopper;"
                        + "minecraft:beacon;minecraft:respawn_anchor",
                2_048, false));
        minimumEvidence = addSetting(new IntegerSetting("minimum_evidence", "Minimum evidence",
                "Nearby configured blocks required before a location is highlighted.", 3, 1, 16));
        clusterRadius = addSetting(new IntegerSetting("cluster_radius", "Cluster radius",
                "Maximum three-dimensional distance between evidence blocks in a cluster.", 8, 2, 24));
        horizontalRange = addSetting(new IntegerSetting("horizontal_range", "Horizontal range",
                "Horizontal local scan radius in blocks.", 32, 8, 64));
        verticalRange = addSetting(new IntegerSetting("vertical_range", "Vertical range",
                "Vertical local scan radius around the player in blocks.", 24, 8, 64));
        scanBudget = addSetting(new IntegerSetting("scan_budget", "Scan budget",
                "Maximum loaded blocks checked per client tick.", 1_024, 64, 2_048));
        maximumMarkers = addSetting(new IntegerSetting("maximum_markers", "Maximum markers",
                "Hard cap for local evidence markers rendered per frame.", 128, 1, 512));
        tracers = addSetting(new BooleanSetting("tracers", "Tracers",
                "Draw local camera-to-evidence lines for confirmed clusters.", true));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width",
                "Local marker and tracer line width.", 1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color",
                "ARGB local outline and tracer color.", 0xFFFF7043));
        fillColor = addSetting(new ColorSetting("fill_color", "Fill color",
                "ARGB local evidence-marker fill color.", 0x30FF7043));
        targetBlocks = BlockIdList.parse(evidenceBlocks.value());
        evidenceBlocks.addChangeListener(ignored -> refreshTargets());
        horizontalRange.addChangeListener(ignored -> scanRevision++);
        verticalRange.addChangeListener(ignored -> scanRevision++);
    }

    public Set<String> targetBlocks() {
        return targetBlocks;
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

    /**
     * Minecraft-free cluster decision. The candidate counts itself, and distance is truly
     * three-dimensional so evidence on a distant floor does not confirm the marker.
     */
    public boolean shouldHighlight(Evidence candidate, List<Evidence> loadedEvidence) {
        if (candidate == null || loadedEvidence == null || loadedEvidence.isEmpty()) {
            return false;
        }
        long radiusSquared = (long) clusterRadius.value() * clusterRadius.value();
        int nearby = 0;
        for (Evidence evidence : loadedEvidence) {
            if (evidence != null && candidate.squaredDistanceTo(evidence) <= radiusSquared
                    && ++nearby >= minimumEvidence.value()) {
                return true;
            }
        }
        return false;
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

    private void refreshTargets() {
        targetBlocks = BlockIdList.parse(evidenceBlocks.value());
        scanRevision++;
    }

    /** Integer block position used by the Minecraft-free cluster decision. */
    public record Evidence(int x, int y, int z) {
        public long squaredDistanceTo(Evidence other) {
            Objects.requireNonNull(other, "other");
            long deltaX = (long) x - other.x;
            long deltaY = (long) y - other.y;
            long deltaZ = (long) z - other.z;
            return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        }
    }
}
