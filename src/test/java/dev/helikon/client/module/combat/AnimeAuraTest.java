package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimeAuraTest {
    @Test
    void progressesThroughBoundedStateMachineAndExcludesFriends() {
        AnimeAura module = new AnimeAura();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        assertTrue(module.next(0, true, List.of(target("friend", true))).isEmpty());
        registry.setEnabled(module, true);
        assertTrue(module.next(0, true, List.of(target("friend", true))).isEmpty());
        AnimeAura.Action approach = module.next(1, true, List.of(target("enemy", false))).orElseThrow();
        assertEquals(AnimeAura.Stage.APPROACH, approach.stage());
        assertFalse(approach.attack());
        module.markSuccessful(1, approach.stage());
        AnimeAura.Action launcher = module.next(10, true, List.of(target("enemy", false))).orElseThrow();
        assertEquals(AnimeAura.Stage.LAUNCHER, launcher.stage());
        assertTrue(launcher.attack());
    }

    private static CombatTarget target(String id, boolean friend) {
        return new CombatTarget(id, id, CombatEntityType.PLAYER, friend, false, true, true,
                true, 5.0, 10.0, 5, 0, 0, 0, 0, 0, 20, 0, "", List.of());
    }
}
