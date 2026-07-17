package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Alternates the local player skin-layer set and restores untouched user state on every exit path. */
public final class SkinBlinker extends Module {
    private final SkinLayerAccess layers;
    private final NumberSetting halfCycleTicks;
    private final Map<SkinLayer, Boolean> original = new EnumMap<>(SkinLayer.class);
    private final Map<SkinLayer, Boolean> lastApplied = new EnumMap<>(SkinLayer.class);
    private long nextChangeTick = Long.MIN_VALUE;
    private boolean hidden;

    public SkinBlinker(SkinLayerAccess layers) {
        super("skin_blinker", "SkinBlinker", "Blinks local player skin layers without saving or broadcasting them.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        this.layers = Objects.requireNonNull(layers, "layers");
        halfCycleTicks = addSetting(new NumberSetting("half_cycle_ticks", "Half-cycle ticks",
                "Ticks to hide and then restore the local skin layers.", 8.0D, 2.0D, 40.0D));
    }

    /** Advances the local visual state only while a local player is available and no screen is open. */
    public void tick(long clientTick, boolean playerAvailable, boolean screenOpen) {
        if (clientTick < 0L) {
            throw new IllegalArgumentException("clientTick must not be negative");
        }
        if (!isEnabled()) {
            return;
        }
        if (!playerAvailable || screenOpen) {
            restore();
            return;
        }
        captureOriginalState();
        if (clientTick < nextChangeTick) {
            return;
        }

        hidden = !hidden;
        for (SkinLayer layer : SkinLayer.values()) {
            boolean enabled = !hidden && original.get(layer);
            layers.setEnabled(layer, enabled);
            lastApplied.put(layer, enabled);
        }
        long interval = halfCycleTicks();
        nextChangeTick = clientTick > Long.MAX_VALUE - interval ? Long.MAX_VALUE : clientTick + interval;
    }

    @Override
    protected void onEnable() {
        resetSchedule();
    }

    @Override
    protected void onDisable() {
        restore();
    }

    private void captureOriginalState() {
        if (!original.isEmpty()) {
            return;
        }
        for (SkinLayer layer : SkinLayer.values()) {
            original.put(layer, layers.isEnabled(layer));
        }
    }

    private void restore() {
        for (SkinLayer layer : SkinLayer.values()) {
            Boolean originalValue = original.get(layer);
            Boolean lastValue = lastApplied.get(layer);
            if (originalValue != null && lastValue != null && layers.isEnabled(layer) == lastValue) {
                layers.setEnabled(layer, originalValue);
            }
        }
        original.clear();
        lastApplied.clear();
        resetSchedule();
    }

    private long halfCycleTicks() {
        return Math.round(halfCycleTicks.value());
    }

    private void resetSchedule() {
        nextChangeTick = Long.MIN_VALUE;
        hidden = false;
    }
}
