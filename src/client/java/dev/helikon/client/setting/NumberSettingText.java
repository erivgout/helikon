package dev.helikon.client.setting;

/**
 * Text conversion for {@link NumberSetting} values, shared by the ClickGUI
 * number fields and the {@code .setting} command. Kept free of Minecraft
 * classes so the commit-on-valid-input rules stay unit-testable.
 */
public final class NumberSettingText {
    private NumberSettingText() {
    }

    /** Formats a value the way edit fields and command feedback display it. */
    public static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    /**
     * Applies the typed text when it parses to a finite value inside the
     * setting range. Otherwise the current value is kept and {@code false} is
     * returned so the caller can report invalid input.
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
