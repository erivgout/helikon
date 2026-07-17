package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplosionsTest {
    @Test
    void hasStableRenderIdentityAndDefaultsOff() {
        Explosions explosions = new Explosions();

        assertEquals("explosions", explosions.id());
        assertEquals(ModuleCategory.RENDER, explosions.category());
        assertFalse(explosions.defaultEnabled());
    }

    @Test
    void damageRadiusMatchesVanillaPowerTimesTwo() {
        assertEquals(8.0D, Explosions.Source.TNT.damageRadius());
        assertEquals(6.0D, Explosions.Source.CREEPER.damageRadius());
        assertEquals(12.0D, Explosions.Source.CHARGED_CREEPER.damageRadius());
        assertEquals(8.0D, Explosions.Source.TNT_MINECART.damageRadius());
        assertEquals(12.0D, Explosions.Source.END_CRYSTAL.damageRadius());
    }

    @Test
    void includesReflectsPerCategorySettingsOnlyWhenEnabled() {
        Explosions explosions = new Explosions();

        // Disabled: nothing is included regardless of category defaults.
        for (Explosions.Source source : Explosions.Source.values()) {
            assertFalse(explosions.includes(source), "disabled module must include nothing: " + source);
        }

        explosions.enable();
        // Every category defaults on, so both creeper variants share the creepers toggle.
        for (Explosions.Source source : Explosions.Source.values()) {
            assertTrue(explosions.includes(source), "enabled default should include " + source);
        }
    }

    @Test
    void boundedAccessorsExposeValidatedDefaults() {
        Explosions explosions = new Explosions();

        assertEquals(24, explosions.segments());
        assertEquals(32, explosions.maximumSources());
        assertEquals(1.5F, explosions.lineWidth());
        assertTrue(explosions.armedCreepersOnly());
        assertTrue(explosions.showRadiusLabel());
    }
}
