package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GojoAbilitiesTest {
    @Test
    void choosesNearestNonFriendAndRespectsCooldown() {
        GojoAbilities module = new GojoAbilities();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        GojoAbilities.Action action = module.next(10, false,
                List.of(target("friend", 2, true), target("enemy", 4, false))).orElseThrow();
        assertEquals("enemy", action.target().id());
        module.markUsed(10);
        assertTrue(module.next(20, false, List.of(target("enemy", 4, false))).isEmpty());
    }

    private static CombatTarget target(String id, double distance, boolean friend) {
        return new CombatTarget(id, id, CombatEntityType.PLAYER, friend, false, true, true,
                true, distance, 10.0, distance, 0, 0, 0, 0, 0, 20, 0, "", List.of());
    }
}
