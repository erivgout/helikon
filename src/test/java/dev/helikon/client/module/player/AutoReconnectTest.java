package dev.helikon.client.module.player;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoReconnectTest {
    @Test
    void reconnectsOnlyAfterTheVisibleCountdownAndBoundsAttempts() {
        AutoReconnect module = enabled();
        module.onDisconnected(0L, "example.test:25565");

        assertEquals(5, module.remainingSeconds(0L));
        assertTrue(module.nextReconnect(99L, true).isEmpty());
        assertEquals("example.test:25565", module.nextReconnect(100L, true).orElseThrow());
        assertEquals(1, module.attempts());

        module.onReconnectFailed(101L);
        assertEquals("example.test:25565", module.nextReconnect(201L, true).orElseThrow());
        module.onReconnectFailed(202L);
        assertEquals("example.test:25565", module.nextReconnect(302L, true).orElseThrow());
        module.onReconnectFailed(303L);

        assertFalse(module.isAwaitingDisconnectScreen());
    }

    @Test
    void cancelsOnUserChoiceOrWhenNoDisconnectScreenAppears() {
        AutoReconnect module = enabled();
        module.onDisconnected(0L, "example.test");
        module.cancel();
        assertTrue(module.nextReconnect(100L, true).isEmpty());

        module.onDisconnected(0L, "example.test");
        assertTrue(module.nextReconnect(21L, false).isEmpty());
        assertFalse(module.isAwaitingDisconnectScreen());
    }

    private static AutoReconnect enabled() {
        AutoReconnect module = new AutoReconnect();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
