package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/** Configures the fixed local HUD miniature of the current player model. */
public final class MiniPlayer extends Module {
    private final NumberSetting rotation;
    private final NumberSetting scale;
    private final BooleanSetting armor;
    private final ColorSetting backgroundColor;

    public MiniPlayer() {
        super("mini_player", "MiniPlayer", "Draws a local HUD miniature of the current player model.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        rotation = addSetting(new NumberSetting("rotation", "Rotation",
                "Local mini-player yaw in degrees.", 0.0D, -180.0D, 180.0D));
        scale = addSetting(new NumberSetting("scale", "Scale",
                "Local mini-player render scale.", 1.0D, 0.5D, 2.0D));
        armor = addSetting(new BooleanSetting("armor", "Armor", "Render the local player's equipped armor.", true));
        backgroundColor = addSetting(new ColorSetting("background_color", "Background color",
                "ARGB local mini-player panel background.", 0xB014161B));
    }

    public double rotation() { return rotation.value(); }

    public double scale() { return scale.value(); }

    public boolean armorEnabled() { return armor.value(); }

    public int backgroundColor() { return backgroundColor.value(); }
}
