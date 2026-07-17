package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.DamageIndicatorTracker;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;

/** Displays bounded local indicators for observed nearby living-entity health losses. */
public final class DamageIndicators extends Module {
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final NumberSetting range;
    private final NumberSetting maximumTrackedEntities;
    private final NumberSetting maximumIndicators;
    private final NumberSetting lifetimeSeconds;
    private final NumberSetting riseDistance;
    private final ColorSetting color;
    private final DamageIndicatorTracker tracker = new DamageIndicatorTracker();

    public DamageIndicators() {
        super("damage_indicators", "DamageIndicators", "Shows local fading damage amounts for observed nearby entities.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Show observed damage to non-local players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Show observed damage to hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Show observed damage to passive mobs.", false));
        range = addSetting(new NumberSetting("range", "Range", "Maximum local observed entity distance.",
                64.0D, 8.0D, 192.0D));
        maximumTrackedEntities = addSetting(new NumberSetting("maximum_tracked_entities", "Maximum tracked entities",
                "Hard cap for local health snapshots per client tick.", 128.0D, 1.0D, 512.0D));
        maximumIndicators = addSetting(new NumberSetting("maximum_indicators", "Maximum indicators",
                "Hard cap for locally retained damage labels.", 32.0D, 1.0D, 128.0D));
        lifetimeSeconds = addSetting(new NumberSetting("lifetime_seconds", "Lifetime", "Local indicator fade duration.",
                1.2D, 0.25D, 5.0D));
        riseDistance = addSetting(new NumberSetting("rise_distance", "Rise distance", "Local upward label travel in blocks.",
                0.75D, 0.0D, 3.0D));
        color = addSetting(new ColorSetting("color", "Color", "ARGB local damage-label color.", 0xFFFF6B6B));
    }

    public EntityRenderFilter.Options options() {
        return new EntityRenderFilter.Options(players.value(), hostiles.value(), passive.value(), false, false, false,
                range.value());
    }

    public int maximumIndicators() { return (int) Math.round(maximumIndicators.value()); }

    public int maximumTrackedEntities() { return (int) Math.round(maximumTrackedEntities.value()); }

    public long lifetimeMillis() { return Math.round(lifetimeSeconds.value() * 1_000.0D); }

    public double riseDistance() { return riseDistance.value(); }

    public int color() { return color.value(); }

    public void observe(List<DamageIndicatorTracker.ObservedEntity> entities, long nowMillis) {
        tracker.observe(entities, nowMillis, maximumIndicators(), maximumTrackedEntities());
    }

    public List<DamageIndicatorTracker.RenderedIndicator> renderedIndicators(long nowMillis) {
        return tracker.render(nowMillis, lifetimeMillis(), riseDistance());
    }

    @Override
    protected void onDisable() {
        tracker.clear();
    }

    /** Clears stale local observations when the client changes worlds. */
    public void clear() {
        tracker.clear();
    }
}
