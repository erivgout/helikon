package dev.helikon.client.module.chat;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoChatReportsTest {
    @Test
    void blocksOnlyOrdinaryChatWhileEnabled() {
        NoChatReports module = new NoChatReports();
        assertTrue(module.allowsOrdinaryChat("hello"));
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        assertFalse(module.allowsOrdinaryChat("hello"));
        assertTrue(module.allowsOrdinaryChat(".taco"));
        registry.setEnabled(module, false);
        assertTrue(module.allowsOrdinaryChat("hello"));
    }
}
