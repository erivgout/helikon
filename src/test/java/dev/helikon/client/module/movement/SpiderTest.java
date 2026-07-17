package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiderTest {
    @Test
    void climbsOnlyAnOrdinaryMovingPlayerWhoIsCollidingWithAWall() {
        Spider spider = new Spider();
        assertTrue(spider.verticalVelocity(false, true, true, false, false,
                false, false, false, -0.1D).isEmpty());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(spider);
        registry.setEnabled(spider, true);

        assertEquals(0.20D, spider.verticalVelocity(false, true, true, false, false,
                false, false, false, -0.1D).orElseThrow());
        assertEquals(0.35D, spider.verticalVelocity(false, true, true, false, false,
                false, false, false, 0.35D).orElseThrow());
        assertTrue(spider.verticalVelocity(true, true, true, false, false,
                false, false, false, -0.1D).isEmpty());
        assertTrue(spider.verticalVelocity(false, false, true, false, false,
                false, false, false, -0.1D).isEmpty());
        assertTrue(spider.verticalVelocity(false, true, false, false, false,
                false, false, false, -0.1D).isEmpty());
        assertTrue(spider.verticalVelocity(false, true, true, true, false,
                false, false, false, -0.1D).isEmpty());
        assertTrue(spider.verticalVelocity(false, true, true, false, true,
                false, false, false, -0.1D).isEmpty());
        assertTrue(spider.verticalVelocity(false, true, true, false, false,
                true, false, false, -0.1D).isEmpty());
        assertTrue(spider.verticalVelocity(false, true, true, false, false,
                false, true, false, -0.1D).isEmpty());
        assertTrue(spider.verticalVelocity(false, true, true, false, false,
                false, false, true, -0.1D).isEmpty());
    }
}
