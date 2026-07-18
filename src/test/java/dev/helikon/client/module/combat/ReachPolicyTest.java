package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReachPolicyTest {
    @Test
    void requestsBeyondVanillaTargetOnlyWhileAttackingAndDefersToVanillaCrosshair() {
        Reach reach = enabled(new Reach());
        CombatTarget beyond = target("beyond", CombatEntityType.HOSTILE, false, true, 4.0D, 2.0D);

        // Aiming at a target past vanilla 3.0 range with Attack held selects it.
        assertEquals("beyond", reach.reachAttack(0L, List.of(beyond), true, true, false).orElseThrow().id());
        // One ordinary attack per cooldown window.
        assertTrue(reach.reachAttack(1L, List.of(beyond), true, true, false).isEmpty());
        assertEquals("beyond", reach.reachAttack(4L, List.of(beyond), true, true, false).orElseThrow().id());

        // Never acts unless the user is actually attacking and the swing is ready.
        assertTrue(reach.reachAttack(8L, List.of(beyond), false, true, false).isEmpty());
        assertTrue(reach.reachAttack(8L, List.of(beyond), true, false, false).isEmpty());
        // Defers to Minecraft when it already has its own crosshair target.
        assertTrue(reach.reachAttack(8L, List.of(beyond), true, true, true).isEmpty());
    }

    @Test
    void ignoresWithinVanillaRangeFriendsBlockedAndOffAngleTargets() {
        Reach reach = enabled(new Reach());
        CombatTarget withinVanilla = target("near", CombatEntityType.HOSTILE, false, true, 2.5D, 1.0D);
        CombatTarget friend = target("friend", CombatEntityType.PLAYER, true, true, 4.0D, 1.0D);
        CombatTarget blocked = target("blocked", CombatEntityType.HOSTILE, false, false, 4.0D, 1.0D);
        CombatTarget offAngle = target("wide", CombatEntityType.HOSTILE, false, true, 4.0D, 20.0D);

        assertTrue(reach.reachAttack(0L, List.of(withinVanilla, friend, blocked, offAngle), true, true, false).isEmpty());
    }

    @Test
    void disabledOrInvalidInputsAreRejected() {
        Reach disabled = new Reach();
        CombatTarget beyond = target("beyond", CombatEntityType.HOSTILE, false, true, 4.0D, 2.0D);
        assertTrue(disabled.reachAttack(0L, List.of(beyond), true, true, false).isEmpty());

        Reach reach = enabled(new Reach());
        assertThrows(IllegalArgumentException.class, () -> reach.reachAttack(-1L, List.of(), true, true, false));
        assertThrows(IllegalArgumentException.class, () -> reach.reachAttack(0L, null, true, true, false));
    }

    @Test
    void disableResetsCooldownState() {
        Reach reach = enabled(new Reach());
        CombatTarget beyond = target("beyond", CombatEntityType.HOSTILE, false, true, 4.0D, 2.0D);
        assertFalse(reach.reachAttack(0L, List.of(beyond), true, true, false).isEmpty());
        reach.disable();
        reach.enable();
        // After a clean disable/enable the cooldown no longer blocks the next request.
        Optional<CombatTarget> next = reach.reachAttack(1L, List.of(beyond), true, true, false);
        assertEquals("beyond", next.orElseThrow().id());
    }

    @Test
    void blockInteractionRangeUsesTheSharedSettingAndNeverShortensVanilla() {
        Reach reach = new Reach();
        NumberSetting distance = (NumberSetting) reach.settings().stream()
                .filter(setting -> setting.id().equals("reach"))
                .findFirst()
                .orElseThrow();

        assertEquals(9.0D, distance.maximum());
        assertEquals(4.5D, reach.blockInteractionRange(4.5D));

        enabled(reach);
        assertEquals(4.0D, reach.blockInteractionRange(3.0D));
        distance.set(9.0D);
        assertEquals(9.0D, reach.blockInteractionRange(4.5D));
        assertEquals(12.0D, reach.blockInteractionRange(12.0D));
        assertThrows(IllegalArgumentException.class, () -> reach.blockInteractionRange(Double.NaN));
    }

    private static CombatTarget target(String id, CombatEntityType type, boolean friend, boolean lineOfSight,
                                       double distance, double angle) {
        return new CombatTarget(id, id, type, friend, false, true, true, lineOfSight, distance, angle,
                0.0D, 1.0D, distance, 0.1D, 0.0D, 0.0D, 20.0D, 0, "minecraft:air", List.of());
    }

    private static <T extends Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
