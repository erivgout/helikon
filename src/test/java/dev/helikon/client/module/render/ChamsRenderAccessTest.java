package dev.helikon.client.module.render;

import dev.helikon.client.render.ChamsTargets;
import dev.helikon.client.render.ChamsTargetsBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChamsRenderAccessTest {
    @AfterEach
    void clearSharedState() {
        ChamsRenderAccess.clear();
    }

    @Test
    void exposesInstalledTargetsAndColorsToTheRenderBridge() {
        ChamsRenderAccess.install(new ChamsTargets(Map.of(4, 0xFFAA5500, 8, 0xFF00FF00)));

        assertTrue(ChamsRenderAccess.shouldRender(4));
        assertTrue(ChamsRenderAccess.shouldRender(8));
        assertFalse(ChamsRenderAccess.shouldRender(2));
        assertEquals(0xFFAA5500, ChamsRenderAccess.colorFor(4).orElseThrow());
        assertTrue(ChamsRenderAccess.colorFor(2).isEmpty());
    }

    @Test
    void clearRemovesEveryTarget() {
        ChamsRenderAccess.install(new ChamsTargets(Map.of(4, 0xFFFFFFFF)));
        ChamsRenderAccess.clear();

        assertFalse(ChamsRenderAccess.shouldRender(4));
    }

    @Test
    void disablingChamsClearsInstalledTargets() {
        Chams chams = new Chams();
        chams.enable();
        ChamsRenderAccess.install(new ChamsTargets(Map.of(4, 0xFFFFFFFF)));

        chams.disable();

        assertFalse(ChamsRenderAccess.shouldRender(4));
    }

    @Test
    void builderForcesOpaqueColorsAndHonorsTheCap() {
        ChamsTargetsBuilder builder = new ChamsTargetsBuilder(2);

        assertTrue(builder.offer(1, 0x00112233));
        assertTrue(builder.offer(2, 0x0044AABB));
        assertFalse(builder.offer(3, 0xFFFFFFFF));
        assertTrue(builder.offer(1, 0x00998877));

        ChamsTargets targets = builder.build();
        assertEquals(0xFF998877, targets.colorFor(1).orElseThrow());
        assertEquals(0xFF44AABB, targets.colorFor(2).orElseThrow());
        assertFalse(targets.contains(3));
    }
}
