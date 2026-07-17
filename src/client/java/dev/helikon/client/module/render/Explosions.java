package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

/**
 * Draws a local wireframe sphere at the expected entity-damage radius of nearby
 * primed explosion sources (TNT, creepers, TNT minecarts, end crystals).
 *
 * <p>The radius shown is the vanilla entity-damage radius, {@code power * 2.0},
 * which is exact. Block destruction is ray-traced and terrain dependent, so the
 * sphere is an awareness aid, not a guaranteed break boundary. Nothing here is
 * sent to the server; it reads only already-loaded local entities.
 */
public final class Explosions extends Module {
    private final BooleanSetting tnt;
    private final BooleanSetting creepers;
    private final BooleanSetting tntMinecarts;
    private final BooleanSetting endCrystals;
    private final BooleanSetting armedCreepersOnly;
    private final NumberSetting segments;
    private final NumberSetting maximumSources;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final BooleanSetting showRadiusLabel;

    public Explosions() {
        super("explosions", "Explosions", "Shows the expected damage radius of nearby primed explosion sources.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        tnt = addSetting(new BooleanSetting("tnt", "TNT", "Show the radius for primed TNT entities.", true));
        creepers = addSetting(new BooleanSetting("creepers", "Creepers", "Show the radius for creepers.", true));
        tntMinecarts = addSetting(new BooleanSetting("tnt_minecarts", "TNT minecarts",
                "Show the radius for primed TNT minecarts.", true));
        endCrystals = addSetting(new BooleanSetting("end_crystals", "End crystals",
                "Show the radius for end crystals.", true));
        armedCreepersOnly = addSetting(new BooleanSetting("armed_creepers_only", "Armed creepers only",
                "Only show creepers that are actively swelling or ignited.", true));
        segments = addSetting(new NumberSetting("segments", "Segments",
                "Line segments per great circle of the wireframe sphere.", 24.0D, 8.0D, 64.0D));
        maximumSources = addSetting(new NumberSetting("maximum_sources", "Maximum sources",
                "Hard cap for locally rendered explosion sources per frame.", 32.0D, 1.0D, 128.0D));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width", "Local sphere line width.",
                1.5D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local explosion-radius color.", 0xFFFF7043));
        showRadiusLabel = addSetting(new BooleanSetting("show_radius_label", "Show radius label",
                "Draw the numeric radius above each source.", true));
    }

    /** Whether the given source category is currently shown. */
    public boolean includes(Source source) {
        return isEnabled() && switch (source) {
            case TNT -> tnt.value();
            case CREEPER, CHARGED_CREEPER -> creepers.value();
            case TNT_MINECART -> tntMinecarts.value();
            case END_CRYSTAL -> endCrystals.value();
        };
    }

    public boolean armedCreepersOnly() {
        return armedCreepersOnly.value();
    }

    public int segments() {
        return (int) Math.round(segments.value());
    }

    public int maximumSources() {
        return (int) Math.round(maximumSources.value());
    }

    public float lineWidth() {
        return (float) lineWidth.value().doubleValue();
    }

    public int color() {
        return color.value();
    }

    public boolean showRadiusLabel() {
        return showRadiusLabel.value();
    }

    /**
     * A local explosion source, carrying its vanilla explosion power. The
     * entity-damage radius is {@code power * 2.0}.
     */
    public enum Source {
        TNT(4.0D),
        CREEPER(3.0D),
        CHARGED_CREEPER(6.0D),
        TNT_MINECART(4.0D),
        END_CRYSTAL(6.0D);

        private final double explosionPower;

        Source(double explosionPower) {
            this.explosionPower = explosionPower;
        }

        public double explosionPower() {
            return explosionPower;
        }

        /** Vanilla entity-damage radius: entities within this distance can be hurt. */
        public double damageRadius() {
            return explosionPower * 2.0D;
        }
    }
}
