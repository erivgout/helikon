package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/** Draws local eye-to-entity lines for selected, nearby rendered entities. */
public final class Tracers extends Module {
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting items;
    private final BooleanSetting projectiles;
    private final BooleanSetting friends;
    private final NumberSetting range;
    private final NumberSetting maximumEntities;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting friendColor;

    public Tracers() {
        super("tracers", "Tracers", "Draws local eye-to-entity lines for selected nearby entities.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Draw lines to non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Draw lines to hostile mobs.", false));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Draw lines to other living entities.", false));
        items = addSetting(new BooleanSetting("items", "Items", "Draw lines to dropped item entities.", false));
        projectiles = addSetting(new BooleanSetting("projectiles", "Projectiles", "Draw lines to projectile entities.", false));
        friends = addSetting(new BooleanSetting("friends", "Friends", "Draw lines to locally saved friends.", true));
        range = addSetting(new NumberSetting("range", "Range", "Maximum local line distance in blocks.",
                128.0D, 8.0D, 256.0D));
        maximumEntities = addSetting(new NumberSetting("maximum_entities", "Maximum entities",
                "Hard cap for local tracer lines per frame.", 128.0D, 1.0D, 512.0D));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width", "Local tracer line width.",
                1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local tracer color.", 0xFFE8A33D));
        friendColor = addSetting(new ColorSetting("friend_color", "Friend color", "ARGB local friend tracer color.",
                0xFF61D17B));
    }

    public boolean shouldRender(EntityRenderFilter.EntityType type, boolean friend, boolean localPlayer,
                                double distanceSquared) {
        return isEnabled() && EntityRenderFilter.shouldRender(options(), type, friend, localPlayer, distanceSquared);
    }

    public EntityRenderFilter.Options options() {
        return new EntityRenderFilter.Options(players.value(), hostiles.value(), passive.value(), items.value(),
                projectiles.value(), friends.value(), range.value());
    }

    public int maximumEntities() { return (int) Math.round(maximumEntities.value()); }

    public float lineWidth() { return (float) lineWidth.value().doubleValue(); }

    public int color(boolean friend) { return friend ? friendColor.value() : color.value(); }
}
