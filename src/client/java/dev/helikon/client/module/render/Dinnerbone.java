package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.setting.BooleanSetting;

/** Locally applies Minecraft's upside-down living-entity transform to selected categories. */
public final class Dinnerbone extends Module {
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;

    public Dinnerbone() {
        super("dinnerbone", "Dinnerbone", "Renders selected living entities upside down locally.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Flip player models locally.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Flip hostile mob models locally.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Flip other living-entity models locally.", false));
    }

    /** Returns whether a Minecraft-free living-entity category should use the upside-down transform. */
    public boolean shouldFlip(EntityRenderFilter.EntityType entityType) {
        if (!isEnabled() || entityType == null) {
            return false;
        }
        return switch (entityType) {
            case PLAYER -> players.value();
            case HOSTILE -> hostiles.value();
            case PASSIVE -> passive.value();
            case ITEM, PROJECTILE, OTHER -> false;
        };
    }
}
