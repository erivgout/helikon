package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;

/**
 * Reduces the local, purely visual clutter that negative potion effects add.
 * Where AntiBlind targets a fixed set of impairment overlays (blindness and
 * darkness fog, nausea, pumpkin and powder-snow overlays), AntiDebuff removes
 * the swirling effect particles the local player emits while harmful effects
 * are active. The decision logic is Minecraft-free so it can be unit tested; a
 * thin render adapter supplies the observed effect facts.
 */
public final class AntiDebuff extends Module {
    private final BooleanSetting effectParticles;
    private final BooleanSetting harmfulOnly;

    public AntiDebuff() {
        super("anti_debuff", "AntiDebuff",
                "Hides the local swirling particles emitted by negative potion effects.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        effectParticles = addSetting(new BooleanSetting("effect_particles", "Effect particles",
                "Hide the local player's swirling potion-effect particles.", true));
        harmfulOnly = addSetting(new BooleanSetting("harmful_only", "Harmful only",
                "Only hide particles while every active effect is harmful, so beneficial effects still show theirs.",
                true));
    }

    /**
     * Decides whether the local player's combined effect-particle stream should
     * be suppressed this tick.
     *
     * <p>Minecraft renders one merged particle stream for all of an entity's
     * active effects, so a single beneficial effect cannot be separated from a
     * harmful one. When {@code harmfulOnly} is set, particles are hidden only
     * while every active effect is harmful; otherwise they are hidden whenever
     * any effect is active.
     *
     * @param anyEffectActive whether the local player has at least one active effect
     * @param allActiveEffectsHarmful whether every active effect is in the harmful category
     */
    public boolean hidesEffectParticles(boolean anyEffectActive, boolean allActiveEffectsHarmful) {
        if (!isEnabled() || !effectParticles.value() || !anyEffectActive) {
            return false;
        }
        return !harmfulOnly.value() || allActiveEffectsHarmful;
    }
}
