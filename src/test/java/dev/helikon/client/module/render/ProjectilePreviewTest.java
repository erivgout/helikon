package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.HeldProjectilePreview;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectilePreviewTest {
    @Test
    void hasAStableIdentityAndDefaultsOffAsARenderAdvantageAid() {
        ProjectilePreview module = new ProjectilePreview();

        assertEquals("projectilepreview", module.id());
        assertEquals(ModuleCategory.RENDER, module.category());
        assertFalse(module.defaultEnabled());
        assertFalse(module.isEnabled());
    }

    @Test
    void includesFamiliesOnlyWhileEnabledAndSelected() {
        ProjectilePreview module = new ProjectilePreview();
        // Disabled: nothing is previewed regardless of the per-family toggles.
        for (HeldProjectilePreview.Kind kind : HeldProjectilePreview.Kind.values()) {
            assertFalse(module.includes(kind), "disabled module should not preview " + kind);
        }

        module.enable();
        for (HeldProjectilePreview.Kind kind : HeldProjectilePreview.Kind.values()) {
            assertTrue(module.includes(kind), "default-on family should be previewed: " + kind);
        }
        assertTrue(module.previewsOffhand());
    }

    @Test
    void perFamilyToggleGatesTheMatchingKinds() {
        ProjectilePreview module = new ProjectilePreview();
        module.enable();
        module.settings().stream()
                .filter(setting -> setting.id().equals("throwables"))
                .forEach(setting -> ((dev.helikon.client.setting.BooleanSetting) setting).set(false));

        assertFalse(module.includes(HeldProjectilePreview.Kind.THROWABLE));
        assertFalse(module.includes(HeldProjectilePreview.Kind.SPLASH_POTION));
        assertTrue(module.includes(HeldProjectilePreview.Kind.BOW));
    }

    @Test
    void exposesValidatedBoundedSettings() {
        ProjectilePreview module = new ProjectilePreview();

        assertTrue(module.maximumSteps() >= 8 && module.maximumSteps() <= 200);
        assertTrue(module.lineWidth() >= 0.5F && module.lineWidth() <= 4.0F);
    }
}
