package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;

/** Locally draws bounded boxes around selected rendered entity categories. */
public final class EntityEsp extends Module {
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting items;
    private final BooleanSetting projectiles;
    private final BooleanSetting friends;
    private final NumberSetting range;
    private final NumberSetting maximumEntities;
    private final EnumSetting<EntityEspMode> mode;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting friendColor;
    private final ColorSetting fillColor;

    public EntityEsp() {
        super("entity_esp", "EntityESP", "Locally highlights selected rendered entities with reversible visual modes.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Show non-friend players locally.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Show hostile mobs locally.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Show other living entities locally.", false));
        items = addSetting(new BooleanSetting("items", "Items", "Show dropped item entities locally.", false));
        projectiles = addSetting(new BooleanSetting("projectiles", "Projectiles", "Show projectile entities locally.", false));
        friends = addSetting(new BooleanSetting("friends", "Friends", "Show locally saved friends.", true));
        range = addSetting(new NumberSetting("range", "Range", "Maximum local render distance in blocks.",
                96.0D, 8.0D, 256.0D));
        maximumEntities = addSetting(new NumberSetting("maximum_entities", "Maximum entities",
                "Hard cap for local EntityESP targets per frame.", 128.0D, 1.0D, 512.0D));
        mode = addSetting(new EnumSetting<>("mode", "Mode",
                "Outline draws a wireframe; Box adds a fill; Glow uses Minecraft's native team-color outline; "
                        + "Shader uses that native outline with EntityESP colors.",
                EntityEspMode.class, EntityEspMode.OUTLINE));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width", "Local box outline width.",
                1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local outline color.", 0xFF66CCFF));
        friendColor = addSetting(new ColorSetting("friend_color", "Friend color", "ARGB local friend outline color.",
                0xFF61D17B));
        fillColor = addSetting(new ColorSetting("fill_color", "Fill color", "ARGB local box fill color.", 0x3033AAFF));
        mode.addChangeListener(ignored -> {
            if (!mode.value().usesNativeOutline()) {
                EntityEspRenderAccess.clear();
            }
        });
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

    public EntityEspMode mode() { return mode.value(); }

    public float lineWidth() { return (float) lineWidth.value().doubleValue(); }

    public int color(boolean friend) { return friend ? friendColor.value() : color.value(); }

    public int fillColor() { return fillColor.value(); }

    @Override
    protected void onDisable() {
        EntityEspRenderAccess.clear();
    }
}
