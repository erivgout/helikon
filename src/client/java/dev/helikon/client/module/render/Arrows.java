package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/** Configures local directional arrows that point toward enemies outside the field of view. */
public final class Arrows extends Module {
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting friends;
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting ringRadius;
    private final NumberSetting arrowLength;
    private final NumberSetting arrowWidth;
    private final NumberSetting maximumTargets;
    private final ColorSetting color;
    private final ColorSetting friendColor;

    public Arrows() {
        super("arrows", "Arrows",
                "Draws local directional arrows pointing toward enemies outside the field of view.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Point at non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Point at hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Point at other living entities.", false));
        friends = addSetting(new BooleanSetting("friends", "Friends", "Point at locally saved friends.", false));
        range = addSetting(new NumberSetting("range", "Range", "Maximum local target distance in blocks.",
                48.0D, 4.0D, 256.0D));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view",
                "Angular cone in degrees treated as on-screen; targets within it are not marked.",
                70.0D, 30.0D, 170.0D));
        ringRadius = addSetting(new NumberSetting("ring_radius", "Ring radius",
                "Arrow distance from the screen center in pixels.", 40.0D, 8.0D, 160.0D));
        arrowLength = addSetting(new NumberSetting("arrow_length", "Arrow length",
                "Arrow length in pixels.", 9.0D, 3.0D, 32.0D));
        arrowWidth = addSetting(new NumberSetting("arrow_width", "Arrow width",
                "Arrow base width in pixels.", 8.0D, 2.0D, 32.0D));
        maximumTargets = addSetting(new NumberSetting("maximum_targets", "Maximum targets",
                "Hard cap for arrows drawn per frame.", 16.0D, 1.0D, 64.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB arrow color.", 0xFFE84C3D));
        friendColor = addSetting(new ColorSetting("friend_color", "Friend color", "ARGB friend arrow color.",
                0xFF61D17B));
    }

    public EntityRenderFilter.Options options() {
        return new EntityRenderFilter.Options(players.value(), hostiles.value(), passive.value(), false, false,
                friends.value(), range.value());
    }

    public double fieldOfView() { return fieldOfView.value(); }

    public double ringRadius() { return ringRadius.value(); }

    public double arrowLength() { return arrowLength.value(); }

    public double arrowHalfWidth() { return arrowWidth.value() / 2.0D; }

    public int maximumTargets() { return (int) Math.round(maximumTargets.value()); }

    public int color(boolean friend) { return friend ? friendColor.value() : color.value(); }
}
