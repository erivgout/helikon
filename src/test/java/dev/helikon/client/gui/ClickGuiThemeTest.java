package dev.helikon.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickGuiThemeTest {
    @Test
    void resolvesBuiltInThemeIdsCaseInsensitively() {
        assertEquals(ClickGuiTheme.OCEAN, ClickGuiTheme.find("OCEAN").orElseThrow());
        assertTrue(ClickGuiTheme.find("missing").isEmpty());
    }

    @Test
    void windowStateDefaultsAndUpdatesTheme() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        assertEquals(ClickGuiTheme.MIDNIGHT, state.theme());
        state.setTheme(ClickGuiTheme.HIGH_CONTRAST);
        assertEquals(ClickGuiTheme.HIGH_CONTRAST, state.theme());
    }

    @Test
    void resettingOnlySizeDoesNotDiscardSelectedTheme() {
        ClickGuiWindowState state = new ClickGuiWindowState();
        state.setTheme(ClickGuiTheme.OCEAN);
        state.setSize(420, 260);
        state.resetSize();

        assertEquals(ClickGuiTheme.OCEAN, state.theme());
    }

    @Test
    void highContrastSelectedRowsRemainLegible() {
        ClickGuiTheme theme = ClickGuiTheme.HIGH_CONTRAST;

        assertTrue(contrastRatio(theme.rowSelected(), theme.text()) >= 4.5);
        assertTrue(contrastRatio(theme.rowSelected(), theme.accent()) >= 4.5);
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
        return normalized <= 0.04045 ? normalized / 12.92 : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }
}
