package dev.helikon.client.module.render;

/** Narrow bridge used by the ClickGUI to read the optional animated accent. */
public final class RainbowUiAccess {
    private static volatile RainbowUi module;

    private RainbowUiAccess() {
    }

    public static void install(RainbowUi rainbowUi) {
        module = rainbowUi;
    }

    public static int accent(long nowMillis, int fallback) {
        RainbowUi current = module;
        int accent = current == null ? 0 : current.accent(nowMillis);
        return accent == 0 ? fallback : accent;
    }
}
