package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/** Configures a local HUD crosshair without changing hit detection or packets. */
public final class BetterCrosshair extends Module {
    private final NumberSetting size;
    private final NumberSetting gap;
    private final NumberSetting thickness;
    private final BooleanSetting outline;
    private final ColorSetting color;
    private final BooleanSetting dynamicMovement;
    private final BooleanSetting hideVanilla;

    public BetterCrosshair() {
        super("better_crosshair", "BetterCrosshair", "Draws a configurable local crosshair.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        size = addSetting(new NumberSetting("size", "Size", "Length of each crosshair arm in pixels.", 5.0, 1.0, 24.0));
        gap = addSetting(new NumberSetting("gap", "Gap", "Distance from the center in pixels.", 2.0, 0.0, 16.0));
        thickness = addSetting(new NumberSetting("thickness", "Thickness", "Crosshair arm thickness in pixels.", 1.0, 1.0, 6.0));
        outline = addSetting(new BooleanSetting("outline", "Outline", "Draw a dark outline around each arm.", true));
        color = addSetting(new ColorSetting("color", "Color", "ARGB color, for example #FFFFFFFF.", 0xFFFFFFFF));
        dynamicMovement = addSetting(new BooleanSetting("dynamic_movement", "Dynamic movement", "Widen the gap slightly while moving.", true));
        hideVanilla = addSetting(new BooleanSetting("hide_vanilla", "Hide vanilla crosshair", "Suppress Minecraft's normal crosshair while this module is enabled.", true));
    }

    public int sizePixels() { return (int) Math.round(size.value()); }

    public int gapPixels() { return (int) Math.round(gap.value()); }

    public int thicknessPixels() { return (int) Math.round(thickness.value()); }

    public boolean outlineEnabled() { return outline.value(); }

    public int color() { return color.value(); }

    public boolean dynamicMovementEnabled() { return dynamicMovement.value(); }

    public boolean hidesVanillaCrosshair() { return isEnabled() && hideVanilla.value(); }
}
