package dev.helikon.client.module.player;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoLibrarianTest {
    @Test
    void matchesConfiguredBoundedTradeAndAdvancesRerollState() {
        AutoLibrarian module = new AutoLibrarian();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        assertEquals(AutoLibrarian.Decision.FOUND,
                module.inspect(List.of(new AutoLibrarian.Offer(List.of("minecraft:mending"), 20))));
        module.reset();
        assertEquals(AutoLibrarian.Decision.REROLL,
                module.inspect(List.of(new AutoLibrarian.Offer(List.of("minecraft:unbreaking"), 10))));
        assertEquals(AutoLibrarian.Phase.BREAK_LECTERN, module.phase());
        module.markBroken();
        assertEquals(AutoLibrarian.Phase.PLACE_LECTERN, module.phase());
        module.markPlaced();
        assertEquals(AutoLibrarian.Phase.WAITING_FOR_TRADE, module.phase());
    }
}
