package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.StorageEspTargets;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Objects;
import java.util.Set;

/** Locally scans bounded loaded chunks for selected block-entity storage types. */
public final class StorageEsp extends Module {
    private final BooleanSetting chests;
    private final BooleanSetting barrels;
    private final BooleanSetting shulkers;
    private final BooleanSetting furnaces;
    private final BooleanSetting hoppers;
    private final BooleanSetting spawners;
    private final StringSetting customBlockEntities;
    private final NumberSetting horizontalRange;
    private final NumberSetting verticalRange;
    private final NumberSetting scanBudget;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting fillColor;
    private Set<String> targetBlocks;
    private long scanRevision;
    private Runnable cacheClearer = () -> {
    };

    public StorageEsp() {
        super("storage_esp", "StorageESP", "Locally highlights configured nearby loaded block entities.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        chests = addSetting(new BooleanSetting("chests", "Chests", "Include chests, trapped chests, and ender chests.", true));
        barrels = addSetting(new BooleanSetting("barrels", "Barrels", "Include barrels.", true));
        shulkers = addSetting(new BooleanSetting("shulkers", "Shulker boxes", "Include all shulker-box colors.", true));
        furnaces = addSetting(new BooleanSetting("furnaces", "Furnaces", "Include furnaces, blast furnaces, and smokers.", false));
        hoppers = addSetting(new BooleanSetting("hoppers", "Hoppers", "Include hoppers.", true));
        spawners = addSetting(new BooleanSetting("spawners", "Spawners", "Include mob spawners.", true));
        customBlockEntities = addSetting(new StringSetting("custom_block_entities", "Custom block entities",
                "Semicolon-separated block IDs; only loaded blocks with block entities are highlighted.", "", 1_024, true));
        horizontalRange = addSetting(new NumberSetting("horizontal_range", "Horizontal range",
                "Horizontal local scan radius in blocks.", 32.0D, 8.0D, 64.0D));
        verticalRange = addSetting(new NumberSetting("vertical_range", "Vertical range",
                "Vertical local scan radius around the player in blocks.", 24.0D, 8.0D, 64.0D));
        scanBudget = addSetting(new NumberSetting("scan_budget", "Scan budget",
                "Maximum local blocks checked per client tick.", 512.0D, 64.0D, 2_048.0D));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width", "Local box line width.",
                1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local storage-box outline color.", 0xFFCE93D8));
        fillColor = addSetting(new ColorSetting("fill_color", "Fill color", "ARGB local storage-box fill color.", 0x30CE93D8));
        targetBlocks = targetsFromSettings();
        chests.addChangeListener(ignored -> refreshTargets());
        barrels.addChangeListener(ignored -> refreshTargets());
        shulkers.addChangeListener(ignored -> refreshTargets());
        furnaces.addChangeListener(ignored -> refreshTargets());
        hoppers.addChangeListener(ignored -> refreshTargets());
        spawners.addChangeListener(ignored -> refreshTargets());
        customBlockEntities.addChangeListener(ignored -> refreshTargets());
        horizontalRange.addChangeListener(ignored -> scanRevision++);
        verticalRange.addChangeListener(ignored -> scanRevision++);
    }

    public Set<String> targetBlocks() { return targetBlocks; }

    public int horizontalRange() { return (int) Math.round(horizontalRange.value()); }

    public int verticalRange() { return (int) Math.round(verticalRange.value()); }

    public int scanBudget() { return (int) Math.round(scanBudget.value()); }

    public float lineWidth() { return (float) lineWidth.value().doubleValue(); }

    public int color() { return color.value(); }

    public int fillColor() { return fillColor.value(); }

    public long scanRevision() { return scanRevision; }

    /** Installs the thin local render-cache cleanup hook without Minecraft types. */
    public void setCacheClearer(Runnable cacheClearer) {
        this.cacheClearer = Objects.requireNonNull(cacheClearer, "cacheClearer");
    }

    @Override
    protected void onDisable() {
        scanRevision++;
        cacheClearer.run();
    }

    private Set<String> targetsFromSettings() {
        return StorageEspTargets.resolve(chests.value(), barrels.value(), shulkers.value(), furnaces.value(),
                hoppers.value(), spawners.value(), customBlockEntities.value());
    }

    private void refreshTargets() {
        targetBlocks = targetsFromSettings();
        scanRevision++;
    }
}
