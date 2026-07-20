package dev.helikon.client.gui;

import java.util.Locale;
import java.util.Optional;

/** Built-in, high-contrast palettes for the local ClickGUI. */
public enum ClickGuiTheme {
    SLATE("slate", "Slate", 0xF52A2D36, 0xFFBB92F5, 0xFF31353F, 0xFF2E313B,
            0x14FFFFFF, 0x33BB92F5, 0xFFBB92F5, 0xFFEDEFF4, 0xFF9AA0AC, 0xFF3A3E4A,
            0xFFFF6B6B, 0xFF565B6B),
    HELIKON("helikon", "Helikon Gold", 0xF20F0D09, 0xFFE0A93F, 0xFF14110C, 0xFF12100B,
            0x16FFFFFF, 0x33E0A93F, 0xFFE0A93F, 0xFFF2E9D6, 0xFF8D8672, 0xFF2B2619,
            0xFFFF6060, 0xFF6B5B2E),
    MIDNIGHT("midnight", "Midnight", 0xF014161B, 0xFF1C2027, 0xFF181B21, 0xFF171A20,
            0x22FFFFFF, 0x33E8A33D, 0xFFE8A33D, 0xFFE6E6E6, 0xFF9AA1AB, 0xFF2A2F38,
            0xFFFF6060, 0xFF3A4150),
    HIGH_CONTRAST("high_contrast", "High Contrast", 0xFF101010, 0xFF000000, 0xFF171717, 0xFF101010,
            0x44FFFFFF, 0xFF4A3B00, 0xFFFFFF00, 0xFFFFFFFF, 0xFFE0E0E0, 0xFFFFFFFF,
            0xFFFF5555, 0xFFFFFFFF),
    OCEAN("ocean", "Ocean", 0xF0101C2A, 0xFF14263A, 0xFF102237, 0xFF0E1D30,
            0x224CC9F0, 0x334CC9F0, 0xFF4CC9F0, 0xFFE7F6FF, 0xFF9EC8DB, 0xFF28506B,
            0xFFFF7878, 0xFF4A819E),
    EMERALD("emerald", "Emerald", 0xF20A100C, 0xFF3FBF6F, 0xFF0D140F, 0xFF0C120E,
            0x16FFFFFF, 0x333FBF6F, 0xFF3FBF6F, 0xFFE2F2E6, 0xFF7E907F, 0xFF1C2B20,
            0xFFFF6060, 0xFF2E6B45),
    CRIMSON("crimson", "Crimson", 0xF2120B0B, 0xFFE05252, 0xFF170E0E, 0xFF140D0D,
            0x16FFFFFF, 0x33E05252, 0xFFE05252, 0xFFF4E4E2, 0xFF97807C, 0xFF2E1B19,
            0xFFFFA640, 0xFF6B2E2E),
    AMETHYST("amethyst", "Amethyst", 0xF20E0B14, 0xFFB07CE8, 0xFF120E1A, 0xFF100D17,
            0x16FFFFFF, 0x33B07CE8, 0xFFB07CE8, 0xFFEEE6F6, 0xFF87809A, 0xFF251C33,
            0xFFFF6060, 0xFF54406B);

    private final String id;
    private final String displayName;
    private final int panel;
    private final int header;
    private final int sidebar;
    private final int settings;
    private final int rowHover;
    private final int rowSelected;
    private final int accent;
    private final int text;
    private final int textDim;
    private final int outline;
    private final int invalid;
    private final int scrollbar;

    ClickGuiTheme(String id, String displayName, int panel, int header, int sidebar, int settings,
                  int rowHover, int rowSelected, int accent, int text, int textDim, int outline,
                  int invalid, int scrollbar) {
        this.id = id;
        this.displayName = displayName;
        this.panel = panel;
        this.header = header;
        this.sidebar = sidebar;
        this.settings = settings;
        this.rowHover = rowHover;
        this.rowSelected = rowSelected;
        this.accent = accent;
        this.text = text;
        this.textDim = textDim;
        this.outline = outline;
        this.invalid = invalid;
        this.scrollbar = scrollbar;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public int panel() { return panel; }
    public int header() { return header; }
    public int sidebar() { return sidebar; }
    public int settings() { return settings; }
    public int rowHover() { return rowHover; }
    public int rowSelected() { return rowSelected; }
    public int accent() { return accent; }
    public int text() { return text; }
    public int textDim() { return textDim; }
    public int outline() { return outline; }
    public int invalid() { return invalid; }
    public int scrollbar() { return scrollbar; }

    /** Returns whichever opaque neutral text color has stronger contrast with the background. */
    public static int contrastingText(int background) {
        return contrastRatio(background, 0xFF000000) >= contrastRatio(background, 0xFFFFFFFF)
                ? 0xFF000000 : 0xFFFFFFFF;
    }

    public static Optional<ClickGuiTheme> find(String id) {
        if (id == null) return Optional.empty();
        String normalized = id.toLowerCase(Locale.ROOT);
        for (ClickGuiTheme theme : values()) if (theme.id.equals(normalized)) return Optional.of(theme);
        return Optional.empty();
    }

    private static double contrastRatio(int first, int second) {
        double firstLuminance = relativeLuminance(first);
        double secondLuminance = relativeLuminance(second);
        return (Math.max(firstLuminance, secondLuminance) + 0.05)
                / (Math.min(firstLuminance, secondLuminance) + 0.05);
    }

    private static double relativeLuminance(int color) {
        double red = linearChannel((color >> 16) & 0xFF);
        double green = linearChannel((color >> 8) & 0xFF);
        double blue = linearChannel(color & 0xFF);
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private static double linearChannel(int channel) {
        double normalized = channel / 255.0;
        return normalized <= 0.04045 ? normalized / 12.92
                : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }
}
