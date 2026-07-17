package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Locale;
import java.util.Optional;

/** Configures one render-only local highlight for Minecraft's current block target. */
public final class BlockSelection extends Module {
    private final ColorSetting outlineColor;
    private final BooleanSetting fill;
    private final NumberSetting lineWidth;
    private final BooleanSetting distanceLabel;

    public BlockSelection() {
        super("block_selection", "BlockSelection", "Highlights the current local block target.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        outlineColor = addSetting(new ColorSetting("outline_color", "Outline color",
                "ARGB color used by the local selected-block outline.", 0xFF80CBC4));
        fill = addSetting(new BooleanSetting("fill", "Fill",
                "Use a translucent local fill derived from the outline color.", true));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width",
                "Local selected-block outline width.", 1.0D, 0.5D, 4.0D));
        distanceLabel = addSetting(new BooleanSetting("distance_label", "Distance label",
                "Show the local eye-to-block distance above the selected block.", true));
    }

    public Options options() {
        return new Options(outlineColor.value(), fill.value(), (float) lineWidth.value().doubleValue(), distanceLabel.value());
    }

    /** Returns a bounded local label only while enabled and requested. */
    public Optional<String> distanceLabel(double distance) {
        if (!isEnabled() || !distanceLabel.value() || !Double.isFinite(distance) || distance < 0.0D || distance > 256.0D) {
            return Optional.empty();
        }
        return Optional.of(String.format(Locale.ROOT, "%.1f m", distance));
    }

    public record Options(int outlineColor, boolean fill, float lineWidth, boolean distanceLabel) {
    }
}
