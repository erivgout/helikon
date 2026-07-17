package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Locally identifies loaded fishing sites that match vanilla's open-water layer rule. */
public final class OpenWaterEsp extends Module {
    public static final int LAYER_COUNT = 4;
    public static final int CELLS_PER_LAYER = 25;

    public enum CellType {
        ABOVE_WATER,
        INSIDE_WATER,
        INVALID
    }

    private final IntegerSetting horizontalRange;
    private final IntegerSetting verticalSearch;
    private final IntegerSetting scanBudget;
    private final IntegerSetting maximumSites;
    private final BooleanSetting alwaysOnTop;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting fillColor;
    private long scanRevision;
    private Runnable cacheClearer = () -> {
    };

    public OpenWaterEsp() {
        super("open_water_esp", "OpenWaterESP",
                "Highlights loaded water surfaces that satisfy vanilla's open-water fishing shape.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        horizontalRange = addSetting(new IntegerSetting("horizontal_range", "Horizontal range",
                "Horizontal local scan radius in blocks.", 16, 8, 32));
        verticalSearch = addSetting(new IntegerSetting("vertical_search", "Vertical search",
                "Vertical search radius around the local player.", 12, 4, 32));
        scanBudget = addSetting(new IntegerSetting("scan_budget", "Scan budget",
                "Maximum local columns checked per client tick.", 8, 1, 32));
        maximumSites = addSetting(new IntegerSetting("maximum_sites", "Maximum sites",
                "Hard cap for retained local open-water markers.", 128, 16, 512));
        alwaysOnTop = addSetting(new BooleanSetting("always_on_top", "Always on top",
                "Draw qualifying water-surface markers through nearby terrain.", true));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width",
                "Local marker outline width.", 1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color",
                "ARGB local open-water marker outline color.", 0xFF29B6F6));
        fillColor = addSetting(new ColorSetting("fill_color", "Fill color",
                "ARGB local open-water marker fill color.", 0x4029B6F6));
        horizontalRange.addChangeListener(ignored -> scanRevision++);
        verticalSearch.addChangeListener(ignored -> scanRevision++);
        maximumSites.addChangeListener(ignored -> scanRevision++);
    }

    public int horizontalRange() {
        return horizontalRange.value();
    }

    public int verticalSearch() {
        return verticalSearch.value();
    }

    public int scanBudget() {
        return scanBudget.value();
    }

    public int maximumSites() {
        return maximumSites.value();
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

    /** Installs the narrow local render-cache cleanup hook without Minecraft types. */
    public void setCacheClearer(Runnable cacheClearer) {
        this.cacheClearer = Objects.requireNonNull(cacheClearer, "cacheClearer");
    }

    /**
     * Applies vanilla's four-layer ordering rule to 5x5 layers ordered bottom-to-top.
     * Each complete layer must be uniform, water cannot occur above air, and the first
     * layer must be water.
     */
    public static boolean isValidSite(CellType[] cells) {
        if (cells == null || cells.length != LAYER_COUNT * CELLS_PER_LAYER) {
            return false;
        }
        CellType previous = CellType.INVALID;
        for (int layer = 0; layer < LAYER_COUNT; layer++) {
            int offset = layer * CELLS_PER_LAYER;
            CellType current = cells[offset];
            if (current == null || current == CellType.INVALID) {
                return false;
            }
            for (int index = 1; index < CELLS_PER_LAYER; index++) {
                if (cells[offset + index] != current) {
                    return false;
                }
            }
            if (current == CellType.ABOVE_WATER && previous == CellType.INVALID) {
                return false;
            }
            if (current == CellType.INSIDE_WATER && previous == CellType.ABOVE_WATER) {
                return false;
            }
            previous = current;
        }
        return true;
    }

    @Override
    protected void onDisable() {
        scanRevision++;
        cacheClearer.run();
    }
}
