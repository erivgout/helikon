package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.render.RenderColor;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/** Shows local translucent boxes for selected invisible entities without changing their actual render state. */
public final class TrueSight extends Module {
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final NumberSetting range;
    private final NumberSetting maximumEntities;
    private final NumberSetting transparency;
    private final NumberSetting lineWidth;
    private final ColorSetting color;

    public TrueSight() {
        super("true_sight", "TrueSight", "Shows local translucent boxes for selected invisible entities.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Show invisible players locally.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Show invisible hostile mobs locally.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Show other invisible living entities locally.", false));
        range = addSetting(new NumberSetting("range", "Range", "Maximum local TrueSight range in blocks.",
                64.0D, 8.0D, 192.0D));
        maximumEntities = addSetting(new NumberSetting("maximum_entities", "Maximum entities",
                "Hard cap for local invisible-entity boxes per frame.", 64.0D, 1.0D, 256.0D));
        transparency = addSetting(new NumberSetting("transparency", "Transparency",
                "Local box alpha multiplier for invisible-entity visualization.", 0.45D, 0.05D, 1.0D));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width", "Local TrueSight outline width.",
                1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local invisible-entity color.", 0xFFAB47BC));
    }

    public boolean shouldRender(EntityRenderFilter.EntityType type, boolean localPlayer, double distanceSquared) {
        return isEnabled() && EntityRenderFilter.shouldRender(options(), type, false, localPlayer, distanceSquared);
    }

    public EntityRenderFilter.Options options() {
        return new EntityRenderFilter.Options(players.value(), hostiles.value(), passive.value(), false, false, false,
                range.value());
    }

    public int maximumEntities() { return (int) Math.round(maximumEntities.value()); }

    public float lineWidth() { return (float) lineWidth.value().doubleValue(); }

    public int transparentColor() { return RenderColor.withAlpha(color.value(), transparency.value()); }
}
