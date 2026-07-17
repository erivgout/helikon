package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.ChamsColorPolicy;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/**
 * Renders selected entities with Minecraft's occlusion-visible outline material
 * so they remain visible through walls. The server stays authoritative; this is
 * a purely local render effect that changes no entity, world, or packet state.
 */
public final class Chams extends Module {
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting friends;
    private final NumberSetting range;
    private final NumberSetting maximumEntities;
    private final BooleanSetting healthColor;
    private final ColorSetting color;
    private final ColorSetting friendColor;

    public Chams() {
        super("chams", "Chams", "Renders selected entities with an occlusion-visible outline material so they show "
                + "through walls.", ModuleCategory.RENDER, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Show non-friend players locally.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Show hostile mobs locally.", false));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Show other living entities locally.", false));
        friends = addSetting(new BooleanSetting("friends", "Friends", "Show locally saved friends.", false));
        range = addSetting(new NumberSetting("range", "Range", "Maximum local render distance in blocks.",
                96.0D, 8.0D, 256.0D));
        maximumEntities = addSetting(new NumberSetting("maximum_entities", "Maximum entities",
                "Hard cap for local Chams targets per tick.", 64.0D, 1.0D, 512.0D));
        healthColor = addSetting(new BooleanSetting("health_color", "Health color",
                "Color living targets from green (full) to red (low) instead of the base color.", false));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local base silhouette color.", 0xFFFF4D4D));
        friendColor = addSetting(new ColorSetting("friend_color", "Friend color",
                "ARGB local friend silhouette color.", 0xFF61D17B));
    }

    /** Immutable category/friend/range gate for the current settings. */
    public EntityRenderFilter.Options options() {
        return new EntityRenderFilter.Options(players.value(), hostiles.value(), passive.value(), false, false,
                friends.value(), range.value());
    }

    public int maximumEntities() {
        return (int) Math.round(maximumEntities.value());
    }

    public boolean healthColor() {
        return healthColor.value();
    }

    /** Opaque outline color for one target, honoring friend and health-color rules. */
    public int colorFor(boolean friend, boolean living, double healthFraction) {
        return ChamsColorPolicy.colorFor(friend, friendColor.value(), healthColor.value(), living, healthFraction,
                color.value());
    }

    @Override
    protected void onDisable() {
        ChamsRenderAccess.clear();
    }
}
