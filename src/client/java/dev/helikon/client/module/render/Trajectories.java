package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/** Locally predicts visible in-flight projectile paths and their first block collision. */
public final class Trajectories extends Module {
    private final BooleanSetting arrows;
    private final BooleanSetting tridents;
    private final BooleanSetting snowballs;
    private final BooleanSetting eggs;
    private final BooleanSetting enderPearls;
    private final BooleanSetting splashPotions;
    private final NumberSetting maximumSteps;
    private final NumberSetting maximumProjectiles;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting impactColor;

    public Trajectories() {
        super("trajectories", "Trajectories", "Predicts local paths for visible in-flight projectile entities.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        arrows = addSetting(new BooleanSetting("arrows", "Arrows", "Predict local bow and crossbow arrow paths.", true));
        tridents = addSetting(new BooleanSetting("tridents", "Tridents", "Predict local in-flight trident paths.", true));
        snowballs = addSetting(new BooleanSetting("snowballs", "Snowballs", "Predict local snowball paths.", true));
        eggs = addSetting(new BooleanSetting("eggs", "Eggs", "Predict local egg paths.", true));
        enderPearls = addSetting(new BooleanSetting("ender_pearls", "Ender pearls", "Predict local ender-pearl paths.", true));
        splashPotions = addSetting(new BooleanSetting("splash_potions", "Splash potions", "Predict local splash-potion paths.", true));
        maximumSteps = addSetting(new NumberSetting("maximum_steps", "Maximum steps",
                "Maximum future local simulation ticks per projectile.", 80.0D, 8.0D, 200.0D));
        maximumProjectiles = addSetting(new NumberSetting("maximum_projectiles", "Maximum projectiles",
                "Hard cap for locally simulated projectiles per render frame.", 16.0D, 1.0D, 64.0D));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width", "Local path line width.",
                1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local trajectory color.", 0xFF80DEEA));
        impactColor = addSetting(new ColorSetting("impact_color", "Impact color", "ARGB local predicted impact color.",
                0xFFFF8A80));
    }

    public boolean includes(ProjectileType type) {
        return isEnabled() && switch (type) {
            case ARROW -> arrows.value();
            case TRIDENT -> tridents.value();
            case SNOWBALL -> snowballs.value();
            case EGG -> eggs.value();
            case ENDER_PEARL -> enderPearls.value();
            case SPLASH_POTION -> splashPotions.value();
        };
    }

    public int maximumSteps() { return (int) Math.round(maximumSteps.value()); }

    public int maximumProjectiles() { return (int) Math.round(maximumProjectiles.value()); }

    public float lineWidth() { return (float) lineWidth.value().doubleValue(); }

    public int color() { return color.value(); }

    public int impactColor() { return impactColor.value(); }

    public enum ProjectileType {
        ARROW,
        TRIDENT,
        SNOWBALL,
        EGG,
        ENDER_PEARL,
        SPLASH_POTION
    }
}
