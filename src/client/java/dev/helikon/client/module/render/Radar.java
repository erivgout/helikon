package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.render.RadarProjection;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;

/** Configures a compact local entity radar HUD. */
public final class Radar extends Module {
    private final EnumSetting<RadarProjection.Shape> shape;
    private final BooleanSetting minimap;
    private final BooleanSetting rotate;
    private final NumberSetting zoom;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting items;
    private final BooleanSetting friends;
    private final NumberSetting maximumEntities;
    private final ColorSetting backgroundColor;
    private final ColorSetting entityColor;
    private final ColorSetting friendColor;
    private final ColorSetting hostileColor;
    private final ColorSetting passiveColor;
    private final ColorSetting itemColor;
    private final ColorSetting projectileColor;

    public Radar() {
        super("radar", "Radar", "Draws a compact local overhead entity radar HUD.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        shape = addSetting(new EnumSetting<>("shape", "Shape", "Radar boundary shape.",
                RadarProjection.Shape.class, RadarProjection.Shape.CIRCLE));
        minimap = addSetting(new BooleanSetting("minimap", "Minimap",
                "Draw nearby surface terrain beneath the radar entity points.", false));
        rotate = addSetting(new BooleanSetting("rotate", "Rotate", "Rotate local radar points with player yaw.", true));
        zoom = addSetting(new NumberSetting("zoom", "Zoom", "Horizontal local radar range in blocks.",
                64.0D, 8.0D, 192.0D));
        players = addSetting(new BooleanSetting("players", "Players", "Show non-friend players locally.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Show hostile mobs locally.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Show other living entities locally.", false));
        items = addSetting(new BooleanSetting("items", "Items", "Show dropped item entities locally.", false));
        friends = addSetting(new BooleanSetting("friends", "Friends", "Show locally saved friends.", true));
        maximumEntities = addSetting(new NumberSetting("maximum_entities", "Maximum entities",
                "Hard cap for local radar points per frame.", 128.0D, 1.0D, 512.0D));
        backgroundColor = addSetting(new ColorSetting("background_color", "Background color", "ARGB local radar background.",
                0xB014161B));
        entityColor = addSetting(new ColorSetting("entity_color", "Entity color", "ARGB local entity point color.",
                0xFFE8A33D));
        friendColor = addSetting(new ColorSetting("friend_color", "Friend color", "ARGB local friend point color.",
                0xFF61D17B));
        hostileColor = addSetting(new ColorSetting("hostile_color", "Hostile color", "ARGB hostile marker color.",
                0xFFFF5B5B));
        passiveColor = addSetting(new ColorSetting("passive_color", "Passive color", "ARGB passive marker color.",
                0xFFFFD166));
        itemColor = addSetting(new ColorSetting("item_color", "Item color", "ARGB dropped-item marker color.",
                0xFF55DDEE));
        projectileColor = addSetting(new ColorSetting("projectile_color", "Projectile color",
                "ARGB projectile marker color.", 0xFFC77DFF));
    }

    public boolean shouldRender(EntityRenderFilter.EntityType type, boolean friend, boolean localPlayer,
                                double distanceSquared) {
        return isEnabled() && EntityRenderFilter.shouldRender(options(), type, friend, localPlayer, distanceSquared);
    }

    public EntityRenderFilter.Options options() {
        return new EntityRenderFilter.Options(players.value(), hostiles.value(), passive.value(), items.value(), false,
                friends.value(), zoom.value());
    }

    public RadarProjection.Shape shape() { return shape.value(); }

    public boolean minimap() { return minimap.value(); }

    public boolean rotate() { return rotate.value(); }

    public double zoom() { return zoom.value(); }

    public int maximumEntities() { return (int) Math.round(maximumEntities.value()); }

    public int backgroundColor() { return backgroundColor.value(); }

    public int color(EntityRenderFilter.EntityType type, boolean friend) {
        if (friend) {
            return friendColor.value();
        }
        return switch (type) {
            case HOSTILE -> hostileColor.value();
            case PASSIVE -> passiveColor.value();
            case ITEM -> itemColor.value();
            case PROJECTILE -> projectileColor.value();
            case PLAYER, OTHER -> entityColor.value();
        };
    }
}
