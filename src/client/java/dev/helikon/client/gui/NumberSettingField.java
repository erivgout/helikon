package dev.helikon.client.gui;

import dev.helikon.client.setting.NumberSetting;

/**
 * Text conversion for {@link NumberSetting} edit fields. Kept free of
 * Minecraft classes so the commit-on-valid-input rules stay unit-testable.
 */
public final class NumberSettingField {
    private NumberSettingField() {
    }

    /** Formats a value the way the edit field initially displays it. */
    public static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    /**
     * Applies the typed text when it parses to a finite value inside the
     * setting range. Otherwise the current value is kept and {@code false} is
     * returned so the field can mark the text as invalid.
     */
    public static boolean tryApply(NumberSetting setting, String text) {
        if (setting == null || text == null) {
            return false;
        }

        double parsed;
        try {
            parsed = Double.parseDouble(text.trim());
        } catch (NumberFormatException exception) {
            return false;
        }

        if (!Double.isFinite(parsed) || parsed < setting.minimum() || parsed > setting.maximum()) {
            return false;
        }
        setting.set(parsed);
        return true;
    }
}
