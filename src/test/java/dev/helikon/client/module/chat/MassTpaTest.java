package dev.helikon.client.module.chat;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MassTpaTest {
    @Test
    void sendsBoundedUniqueNonFriendCommandsAtSafeCadence() {
        MassTpa module = new MassTpa();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        List<MassTpa.Candidate> candidates = List.of(
                new MassTpa.Candidate("Friend", true),
                new MassTpa.Candidate("Alex", false),
                new MassTpa.Candidate("Steve", false));

        assertEquals("tpa Alex", module.nextCommand(0L, candidates).orElseThrow());
        assertTrue(module.nextCommand(39L, candidates).isEmpty());
        assertEquals("tpa Steve", module.nextCommand(60L, candidates).orElseThrow());
        assertTrue(module.nextCommand(120L, candidates).isEmpty());
        module.disable();
        module.enable();
        assertEquals("tpa Alex", module.nextCommand(0L, candidates).orElseThrow());
    }
}
