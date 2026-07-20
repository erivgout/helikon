package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Local-only policy for richer player name-tag text. */
public final class BetterNametags extends Module {
    private final BooleanSetting health;
    private final BooleanSetting distance;
    private final BooleanSetting armor;
    private final BooleanSetting heldItem;
    private final BooleanSetting friendStatus;
    private final NumberSetting range;

    public BetterNametags() {
        super("better_nametags", "Better Nametags", "Shows local health, distance, armor, held item, and friend facts.",
                ModuleCategory.RENDER, false, Keybind.unbound());
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

    /** Whether Helikon will draw a complete replacement for this vanilla player label. */
    public boolean replacesVanillaName(boolean remotePlayer, boolean visible, boolean lineOfSight,
                                       double distanceSquared) {
        return isEnabled() && remotePlayer && visible && lineOfSight && Double.isFinite(distanceSquared)
                && distanceSquared >= 0.0D && distanceSquared <= range.value() * range.value();
    }

    public record Options(boolean health, boolean distance, boolean armor, boolean heldItem, boolean friendStatus, double range) {
    }
}
