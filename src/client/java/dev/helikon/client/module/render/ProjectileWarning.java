package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/**
 * Locally highlights incoming projectiles that are on a short-horizon collision
 * course with the local player. The threat decision itself is Minecraft-free
 * ({@link dev.helikon.client.render.ProjectileThreatPolicy}); this module only
 * owns the validated settings and reuses the shared projectile-family
 * classification.
 */
public final class ProjectileWarning extends Module {
    private final BooleanSetting arrows;
    private final BooleanSetting tridents;
    private final BooleanSetting snowballs;
    private final BooleanSetting eggs;
    private final BooleanSetting enderPearls;
    private final BooleanSetting splashPotions;
    private final BooleanSetting excludeFriendProjectiles;
    private final BooleanSetting showLabel;
    private final NumberSetting hitRadius;
    private final NumberSetting warningSeconds;
    private final NumberSetting range;
    private final NumberSetting maximumProjectiles;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting fillColor;

    public ProjectileWarning() {
        super("projectile_warning", "ProjectileWarning",
                "Warns about incoming projectiles that are on a collision course with the local player.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        arrows = addSetting(new BooleanSetting("arrows", "Arrows", "Warn about incoming arrows.", true));
        tridents = addSetting(new BooleanSetting("tridents", "Tridents", "Warn about incoming tridents.", true));
        snowballs = addSetting(new BooleanSetting("snowballs", "Snowballs", "Warn about incoming snowballs.", true));
        eggs = addSetting(new BooleanSetting("eggs", "Eggs", "Warn about incoming eggs.", true));
        enderPearls = addSetting(new BooleanSetting("ender_pearls", "Ender pearls",
                "Warn about incoming ender pearls.", false));
        splashPotions = addSetting(new BooleanSetting("splash_potions", "Splash potions",
                "Warn about incoming splash potions.", true));
        excludeFriendProjectiles = addSetting(new BooleanSetting("exclude_friend_projectiles",
                "Exclude friend projectiles", "Ignore projectiles thrown by locally saved friends.", true));
        showLabel = addSetting(new BooleanSetting("show_label", "Show label",
                "Draw a local warning label with the seconds until closest approach.", true));
        hitRadius = addSetting(new NumberSetting("hit_radius", "Hit radius",
                "Blocks within which a projectile's closest approach counts as a threat.", 1.5D, 0.5D, 4.0D));
        warningSeconds = addSetting(new NumberSetting("warning_seconds", "Warning time",
                "Only warn when the predicted closest approach is at most this many seconds away.", 2.0D, 0.25D, 10.0D));
        range = addSetting(new NumberSetting("range", "Range",
                "Maximum local distance at which a projectile is considered.", 48.0D, 8.0D, 192.0D));
        maximumProjectiles = addSetting(new NumberSetting("maximum_projectiles", "Maximum projectiles",
                "Hard cap for locally highlighted projectiles per render frame.", 32.0D, 1.0D, 64.0D));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width", "Local warning box line width.",
                2.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local warning outline and label color.",
                0xFFFF5252));
        fillColor = addSetting(new ColorSetting("fill_color", "Fill color", "ARGB local warning box fill color.",
                0x40FF5252));
    }

    /** Reuses the shared render projectile-family classification for consistency with Trajectories. */
    public boolean includes(Trajectories.ProjectileType type) {
        return isEnabled() && switch (type) {
            case ARROW -> arrows.value();
            case TRIDENT -> tridents.value();
            case SNOWBALL -> snowballs.value();
            case EGG -> eggs.value();
            case ENDER_PEARL -> enderPearls.value();
            case SPLASH_POTION -> splashPotions.value();
        };
    }

    public boolean excludeFriendProjectiles() { return excludeFriendProjectiles.value(); }

    public boolean showLabel() { return showLabel.value(); }

    public double hitRadius() { return hitRadius.value(); }

    /** The configured warning lead time expressed in client ticks. */
    public double warningTicks() { return warningSeconds.value() * 20.0D; }

    public double detectionRange() { return range.value(); }

    public int maximumProjectiles() { return (int) Math.round(maximumProjectiles.value()); }

    public float lineWidth() { return (float) lineWidth.value().doubleValue(); }

    public int color() { return color.value(); }

    public int fillColor() { return fillColor.value(); }
}
