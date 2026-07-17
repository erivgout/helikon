package dev.helikon.client.module.player;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyPlayerModulesTest {
    @Test
    void creativeModulesAreOneShotCreativeOnlyAndReset() {
        List<CreativeItemModule> modules = List.of(
                new ItemGenerator(), new KillPotion(), new TrollPotion(), new CommandBlock());
        for (CreativeItemModule module : modules) {
            enabled(module);
            assertTrue(module.nextRequest(false, false).isEmpty());
            assertTrue(module.nextRequest(true, false).isPresent());
            module.markDelivered();
            assertTrue(module.nextRequest(true, false).isEmpty());
            module.disable();
            module.enable();
            assertTrue(module.nextRequest(true, false).isPresent());
        }
    }

    @Test
    void defensiveUseModulesSelectOnlyEligibleItemsAndRestoreOwnedSlot() {
        AntiPotion antiPotion = enabled(new AntiPotion());
        List<HotbarUseModule.Candidate> candidates = List.of(
                new HotbarUseModule.Candidate(2, true, false, false, false),
                new HotbarUseModule.Candidate(3, false, true, false, false));
        HotbarUseModule.Action action = antiPotion.update(0L,
                new HotbarUseModule.Context(0, true, false, false), candidates);
        assertEquals(HotbarUseModule.ActionType.SELECT_AND_USE, action.type());
        assertEquals(3, action.slot());
        assertEquals(HotbarUseModule.ActionType.RESTORE, antiPotion.update(1L,
                new HotbarUseModule.Context(3, true, false, false), candidates).type());

        FastEat fastEat = enabled(new FastEat());
        assertTrue(fastEat.shouldRelease(12));
        assertFalse(fastEat.shouldRelease(11));
    }

    @Test
    void antiHungerOnlyStopsSprintAtThreshold() {
        AntiHunger antiHunger = enabled(new AntiHunger());
        assertTrue(antiHunger.shouldStopSprinting(18, true));
        assertFalse(antiHunger.shouldStopSprinting(20, true));
        assertFalse(antiHunger.shouldStopSprinting(18, false));
    }

    private static <T extends dev.helikon.client.module.Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
