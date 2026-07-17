package dev.helikon.client.module.world;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiCactusTest {
    private static final CactusCollisionPolicy.Bounds PLAYER = new CactusCollisionPolicy.Bounds(0.0D, 0.0D, 0.0D,
            0.6D, 1.8D, 0.6D);
    private static final CactusCollisionPolicy.Bounds CACTUS = new CactusCollisionPolicy.Bounds(0.7D, 0.0D, -1.0D,
            1.7D, 2.0D, 0.7D);

    @Test
    void isDisabledByDefaultAndSlidesAlongTheOnlySafeHorizontalAxisWhenEnabled() {
        AntiCactus module = new AntiCactus();
        CactusCollisionPolicy.Movement requested = new CactusCollisionPolicy.Movement(0.2D, 0.0D, 0.2D);
        assertEquals(requested, module.adjust(requested, PLAYER, List.of(CACTUS)));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        assertFalse(module.shouldAdjust(false));
        assertTrue(module.shouldAdjust(true));
        assertEquals(new CactusCollisionPolicy.Movement(0.0D, 0.0D, 0.2D),
                module.adjust(requested, PLAYER, List.of(CACTUS)));
    }

    @Test
    void preservesNonCollidingAndVerticalOnlyMovement() {
        CactusCollisionPolicy.Movement safe = new CactusCollisionPolicy.Movement(-0.2D, 0.0D, 0.0D);
        assertEquals(safe, CactusCollisionPolicy.avoid(safe, PLAYER, List.of(CACTUS)));
        CactusCollisionPolicy.Movement vertical = new CactusCollisionPolicy.Movement(0.0D, 0.1D, 0.0D);
        assertEquals(vertical, CactusCollisionPolicy.avoid(vertical, PLAYER, List.of(CACTUS)));
    }
}
