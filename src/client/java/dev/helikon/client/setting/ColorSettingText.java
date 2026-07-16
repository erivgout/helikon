package dev.helikon.client.setting;

import java.util.Locale;
import java.util.Objects;

/** Parsing and formatting rules shared by command and ClickGUI color editing. */
public final class ColorSettingText {
    private ColorSettingText() {
    }

    public static String format(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    public static int parse(String text) {
        Objects.requireNonNull(text, "text");
        String trimmed = text.trim();
        if (!trimmed.matches("#[0-9a-fA-F]{8}")) {
            throw new IllegalArgumentException("Expected #AARRGGBB");
        }
        return (int) Long.parseLong(trimmed.substring(1), 16);
    }

    /** Applies a complete color token and leaves the last valid value intact on failure. */
    public static boolean tryApply(ColorSetting setting, String text) {
        Objects.requireNonNull(setting, "setting");
        try {
            setting.set(parse(text));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
