package dev.helikon.client.module.render;

import dev.helikon.client.render.EntityEspNativeOutlineTargets;
import dev.helikon.client.setting.EnumSetting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityEspRenderAccessTest {
    @AfterEach
    void clearSharedState() {
        EntityEspRenderAccess.clear();
    }

    @Test
    void exposesInstalledTargetsToTheRenderBridge() {
        EntityEspRenderAccess.install(new EntityEspNativeOutlineTargets(Set.of(4, 8), Map.of(8, 0xFFAA5500)));

        assertTrue(EntityEspRenderAccess.shouldAppearGlowing(4));
        assertTrue(EntityEspRenderAccess.shouldAppearGlowing(8));
        assertFalse(EntityEspRenderAccess.shouldAppearGlowing(2));
        assertTrue(EntityEspRenderAccess.shaderColorFor(4).isEmpty());
        assertEquals(0xFFAA5500, EntityEspRenderAccess.shaderColorFor(8).orElseThrow());
    }

    @Test
    void clearRemovesEveryTarget() {
        EntityEspRenderAccess.install(new EntityEspNativeOutlineTargets(Set.of(4), Map.of()));
        EntityEspRenderAccess.clear();

        assertFalse(EntityEspRenderAccess.shouldAppearGlowing(4));
    }

    @Test
    void disablingEntityEspClearsInstalledTargets() {
        EntityEsp entityEsp = new EntityEsp();
        entityEsp.enable();
        EntityEspRenderAccess.install(new EntityEspNativeOutlineTargets(Set.of(4), Map.of()));

        entityEsp.disable();

        assertFalse(EntityEspRenderAccess.shouldAppearGlowing(4));
    }

    @Test
    void leavingANativeModeClearsInstalledTargets() {
        EntityEsp entityEsp = new EntityEsp();
        @SuppressWarnings("unchecked")
        EnumSetting<EntityEspMode> mode = (EnumSetting<EntityEspMode>) entityEsp.settings().stream()
                .filter(setting -> setting.id().equals("mode"))
                .findFirst()
                .orElseThrow();
        mode.set(EntityEspMode.GLOW);
        EntityEspRenderAccess.install(new EntityEspNativeOutlineTargets(Set.of(4), Map.of()));

        mode.set(EntityEspMode.OUTLINE);

        assertFalse(EntityEspRenderAccess.shouldAppearGlowing(4));
    }
}
