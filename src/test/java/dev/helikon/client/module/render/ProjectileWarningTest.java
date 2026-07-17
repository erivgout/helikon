package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileWarningTest {
    @Test
    void usesAStableIdentityAndAdvantageSafeDefaults() {
        ProjectileWarning module = new ProjectileWarning();

        assertEquals("projectile_warning", module.id());
        assertEquals(ModuleCategory.RENDER, module.category());
        assertFalse(module.defaultEnabled());
        assertTrue(module.excludeFriendProjectiles());
        assertEquals(40.0D, module.warningTicks(), 1.0E-9D);
    }

    @Test
    void reportsFamilyInclusionOnlyWhileEnabled() {
        ProjectileWarning module = new ProjectileWarning();

        assertFalse(module.includes(Trajectories.ProjectileType.ARROW));

        module.enable();
        assertTrue(module.includes(Trajectories.ProjectileType.ARROW));
        assertTrue(module.includes(Trajectories.ProjectileType.SPLASH_POTION));
        assertFalse(module.includes(Trajectories.ProjectileType.ENDER_PEARL));

        module.disable();
        assertFalse(module.includes(Trajectories.ProjectileType.ARROW));
    }
}
