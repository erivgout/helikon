package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooManyHaxTest {
    @Test
    void returnsDeterministicLaterConflictsOnlyWhileEnabled() {
        TooManyHax module = new TooManyHax();
        assertTrue(module.conflicts(List.of("flight", "phase")).isEmpty());
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        assertEquals(List.of("phase", "tp_aura"),
                module.conflicts(List.of("flight", "phase", "kill_aura", "tp_aura")));
    }
}
