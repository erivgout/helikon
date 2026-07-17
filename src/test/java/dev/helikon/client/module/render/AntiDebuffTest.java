package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiDebuffTest {
    @Test
    void disabledModuleNeverHidesParticles() {
        AntiDebuff antiDebuff = new AntiDebuff();
        assertFalse(antiDebuff.hidesEffectParticles(true, true));
    }

    @Test
    void harmfulOnlyIsDefaultAndHidesOnlyAllHarmfulEffects() {
        AntiDebuff antiDebuff = new AntiDebuff();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(antiDebuff);
        registry.setEnabled(antiDebuff, true);

        assertFalse(antiDebuff.hidesEffectParticles(false, false), "no active effects means nothing to hide");
        assertTrue(antiDebuff.hidesEffectParticles(true, true), "all-harmful effects should be hidden");
        assertFalse(antiDebuff.hidesEffectParticles(true, false), "a beneficial effect present keeps the stream visible");
    }

    @Test
    void disablingHarmfulOnlyHidesWheneverAnyEffectActive() {
        AntiDebuff antiDebuff = new AntiDebuff();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(antiDebuff);
        registry.setEnabled(antiDebuff, true);

        booleanSetting(antiDebuff, "harmful_only").set(false);
        assertTrue(antiDebuff.hidesEffectParticles(true, false), "any active effect is hidden when harmful_only is off");
        assertFalse(antiDebuff.hidesEffectParticles(false, false), "still nothing to hide without active effects");
    }

    @Test
    void effectParticlesToggleDisablesSuppression() {
        AntiDebuff antiDebuff = new AntiDebuff();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(antiDebuff);
        registry.setEnabled(antiDebuff, true);

        booleanSetting(antiDebuff, "effect_particles").set(false);
        assertFalse(antiDebuff.hidesEffectParticles(true, true));
    }

    private static BooleanSetting booleanSetting(AntiDebuff module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
