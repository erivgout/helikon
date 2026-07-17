package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleTimingMetrics;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Opt-in local diagnostics that enable lightweight module timing collection. */
public final class DebugOverlay extends Module {
    private final ModuleTimingMetrics timingMetrics;
    private final NumberSetting page;

    public DebugOverlay(ModuleTimingMetrics timingMetrics) {
        super("debug_overlay", "Debug Overlay", "Shows local Helikon timing, cache, event, and save diagnostics.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        this.timingMetrics = Objects.requireNonNull(timingMetrics, "timingMetrics");
        page = addSetting(new NumberSetting("page", "Page", "Local diagnostic module page.",
                1.0D, 1.0D, 10.0D));
    }

    public int page() {
        return (int) Math.round(page.value());
    }

    @Override
    protected void onEnable() {
        timingMetrics.setRecording(true);
    }

    @Override
    protected void onDisable() {
        timingMetrics.setRecording(false);
    }
}
