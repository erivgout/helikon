package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.HeldProjectilePreview;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/**
 * Locally predicts the path a currently held projectile would follow if fired or thrown
 * from the player's current aim, before it exists as an entity. Complements Trajectories,
 * which only visualizes projectiles that are already in flight.
 */
public final class ProjectilePreview extends Module {
    private final BooleanSetting bows;
    private final BooleanSetting crossbows;
    private final BooleanSetting tridents;
    private final BooleanSetting throwables;
    private final BooleanSetting offhand;
    private final NumberSetting maximumSteps;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting impactColor;

    public ProjectilePreview() {
        super("projectilepreview", "Projectile Preview",
                "Predicts where a held projectile would travel before it is fired or thrown.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        bows = addSetting(new BooleanSetting("bows", "Bows",
                "Preview the arrow path while a bow is being drawn.", true));
        crossbows = addSetting(new BooleanSetting("crossbows", "Crossbows",
                "Preview the arrow path of a loaded crossbow.", true));
        tridents = addSetting(new BooleanSetting("tridents", "Tridents",
                "Preview the thrown path of a held trident.", true));
        throwables = addSetting(new BooleanSetting("throwables", "Throwables",
                "Preview snowball, egg, ender-pearl, and splash-potion paths.", true));
        offhand = addSetting(new BooleanSetting("offhand", "Offhand",
                "Also preview a throwable projectile held in the offhand.", true));
        maximumSteps = addSetting(new NumberSetting("maximum_steps", "Maximum steps",
                "Maximum future local simulation ticks for the predicted path.", 80.0D, 8.0D, 200.0D));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width", "Local path line width.",
                1.0D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local predicted-path color.", 0xFFB2FF59));
        impactColor = addSetting(new ColorSetting("impact_color", "Impact color",
                "ARGB local predicted impact color.", 0xFFFFD180));
    }

    /** Whether the given held family should be previewed, honoring the module toggle. */
    public boolean includes(HeldProjectilePreview.Kind kind) {
        return isEnabled() && switch (kind) {
            case BOW -> bows.value();
            case CROSSBOW -> crossbows.value();
            case TRIDENT -> tridents.value();
            case THROWABLE, SPLASH_POTION -> throwables.value();
        };
    }

    public boolean previewsOffhand() {
        return offhand.value();
    }

    public int maximumSteps() {
        return (int) Math.round(maximumSteps.value());
    }

    public float lineWidth() {
        return (float) lineWidth.value().doubleValue();
    }

    public int color() {
        return color.value();
    }

    public int impactColor() {
        return impactColor.value();
    }
}
