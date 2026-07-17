package dev.helikon.client.module.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestStealTest {
    @Test
    void choosesHighestConfiguredPriorityBeforeClosing() {
        ChestSteal module = new ChestSteal();
        module.enable();

        var action = module.nextAction(0L, 4, List.of(
                new ChestItem(0, "minecraft:stone", 1, 0),
                new ChestItem(1, "minecraft:diamond", 1, 3)));

        assertTrue(action.isPresent());
        assertEquals(ChestSteal.ActionType.QUICK_MOVE, action.get().type());
        assertEquals(1, action.get().clicks().getFirst().slot());
    }
}
