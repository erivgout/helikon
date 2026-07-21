package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Local-only policy for richer player and living-entity name-tag text. */
public final class BetterNametags extends Module {
    private final BooleanSetting players;
    private final BooleanSetting entities;
    private final BooleanSetting health;
    private final BooleanSetting distance;
    private final BooleanSetting armor;
    private final BooleanSetting heldItem;
    private final BooleanSetting friendStatus;
    private final NumberSetting range;

    public BetterNametags() {
        super("better_nametags", "Better Nametags",
                "Shows local names, health, distance, armor, and held items for players and living entities.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Show remote-player name tags.", true));
        entities = addSetting(new BooleanSetting("entities", "Entities", "Show non-player living-entity name tags.", true));
        health = addSetting(new BooleanSetting("health", "Health", "Show locally observed health.", true));
        distance = addSetting(new BooleanSetting("distance", "Distance", "Show locally observed distance.", true));
        armor = addSetting(new BooleanSetting("armor", "Armor", "Show locally observed armor points.", true));
        heldItem = addSetting(new BooleanSetting("held_item", "Held item", "Show the local held-item ID.", true));
        friendStatus = addSetting(new BooleanSetting("friend_status", "Friend status", "Mark local friends.", true));
        range = addSetting(new NumberSetting("range", "Range", "Maximum local name-tag render distance.", 64.0D, 4.0D, 128.0D));
    }

    public Options options() {
        return new Options(health.value(), distance.value(), armor.value(), heldItem.value(), friendStatus.value(), range.value());
    }

    /** Whether the selected target category is enabled. Player and non-player living entities are independent. */
    public boolean targets(boolean playerEntity) {
        return playerEntity ? players.value() : entities.value();
    }

    /** Whether Helikon will draw a complete replacement for this living entity's vanilla label. */
    public boolean replacesVanillaName(boolean eligibleTarget, boolean visible, boolean lineOfSight,
                                       double distanceSquared) {
        return isEnabled() && eligibleTarget && visible && lineOfSight && Double.isFinite(distanceSquared)
                && distanceSquared >= 0.0D && distanceSquared <= range.value() * range.value();
    }

    public record Options(boolean health, boolean distance, boolean armor, boolean heldItem, boolean friendStatus, double range) {
    }
}
