package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
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

    @Test
    void targetsMobsAndHonorsPerTypeToggles() {
        AnimeAura module = new AnimeAura();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        assertEquals(CombatEntityType.PASSIVE,
                module.next(0, true, List.of(target("horse", false, CombatEntityType.PASSIVE)))
                        .orElseThrow().target().type());
        module.reset();
        booleanSetting(module, "passive_mobs").set(false);
        assertTrue(module.next(1, true,
                List.of(target("horse", false, CombatEntityType.PASSIVE))).isEmpty());
        assertEquals(CombatEntityType.HOSTILE,
                module.next(2, true, List.of(target("zombie", false, CombatEntityType.HOSTILE)))
                        .orElseThrow().target().type());
    }

    private static CombatTarget target(String id, boolean friend) {
        return target(id, friend, CombatEntityType.PLAYER);
    }

    private static CombatTarget target(String id, boolean friend, CombatEntityType type) {
        return new CombatTarget(id, id, type, friend, false, true, true,
                true, 5.0, 10.0, 5, 0, 0, 0, 0, 0, 20, 0, "", List.of());
    }

    private static BooleanSetting booleanSetting(AnimeAura module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id))
                .findFirst().orElseThrow();
    }
}
