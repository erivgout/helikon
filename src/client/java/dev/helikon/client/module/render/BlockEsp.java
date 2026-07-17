package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.BlockIdList;
import dev.helikon.client.render.BlockColorMap;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Objects;
import java.util.Map;
import java.util.Set;

/** Locally scans a bounded nearby cube for a validated configured block-ID list. */
public final class BlockEsp extends Module {
    private final StringSetting blocks;
    private final NumberSetting horizontalRange;
    private final NumberSetting verticalRange;
    private final NumberSetting scanBudget;
    private final BooleanSetting tracers;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting fillColor;
    private final StringSetting blockColors;
    private Set<String> targetBlocks;
    private Map<String, Integer> colorOverrides;
    private long scanRevision;
    private Runnable cacheClearer = () -> {
    };

    public BlockEsp() {
        super("block_esp", "BlockESP", "Locally scans a bounded nearby cube for configured block IDs.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        blocks = addSetting(new StringSetting("blocks", "Blocks",
                "Semicolon-separated block IDs; invalid entries are ignored locally.",
                "minecraft:diamond_ore;minecraft:deepslate_diamond_ore", 1_024, false));
        horizontalRange = addSetting(new NumberSetting("horizontal_range", "Horizontal range",
                "Horizontal local scan radius in blocks.", 24.0D, 8.0D, 64.0D));
        verticalRange = addSetting(new NumberSetting("vertical_range", "Vertical range",
                "Vertical local scan radius around the player in blocks.", 24.0D, 8.0D, 64.0D));
        scanBudget = addSetting(new NumberSetting("scan_budget", "Scan budget",
                "Maximum local blocks checked per client tick.", 512.0D, 64.0D, 2_048.0D));
        tracers = addSetting(new BooleanSetting("tracers", "Tracers", "Draw local lines to cached matching blocks.", false));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width", "Local box and line width.",
                1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local outline and tracer color.", 0xFFFFD54F));
        fillColor = addSetting(new ColorSetting("fill_color", "Fill color", "ARGB local block-box fill color.", 0x30FFD54F));
        blockColors = addSetting(new StringSetting("block_colors", "Block colors",
                "Semicolon-separated block=#RRGGBB or block=#AARRGGBB local overrides.", "", 2_048, true));
        targetBlocks = BlockIdList.parse(blocks.value());
        colorOverrides = BlockColorMap.parse(blockColors.value());
        blocks.addChangeListener(ignored -> refreshTargets());
        blockColors.addChangeListener(ignored -> refreshColors());
        horizontalRange.addChangeListener(ignored -> scanRevision++);
        verticalRange.addChangeListener(ignored -> scanRevision++);
    }

    public Set<String> targetBlocks() { return targetBlocks; }

    public int horizontalRange() { return (int) Math.round(horizontalRange.value()); }

    public int verticalRange() { return (int) Math.round(verticalRange.value()); }

    public int scanBudget() { return (int) Math.round(scanBudget.value()); }

    public boolean tracersEnabled() { return tracers.value(); }

    public float lineWidth() { return (float) lineWidth.value().doubleValue(); }

    public int color() { return color.value(); }

    public int color(String blockId) { return colorOverrides.getOrDefault(blockId, color.value()); }

    public int fillColor() { return fillColor.value(); }

    public long scanRevision() { return scanRevision; }

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
        targetBlocks = BlockIdList.parse(blocks.value());
        scanRevision++;
    }

    private void refreshColors() {
        colorOverrides = BlockColorMap.parse(blockColors.value());
    }
}
