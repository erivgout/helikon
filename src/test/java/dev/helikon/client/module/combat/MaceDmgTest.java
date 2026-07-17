package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaceDmgTest {
    @Test
    void defaultsOffAndIdentifiesAsCombatModule() {
        MaceDmg maceDmg = new MaceDmg();

        assertEquals("mace_dmg", maceDmg.id());
        assertEquals(ModuleCategory.COMBAT, maceDmg.category());
        assertFalse(maceDmg.defaultEnabled());
        assertFalse(maceDmg.shouldAttack(0L, target(false), smashContext(5.0D, 1.0D)));
    }

    @Test
    void attacksOnceDuringRealChargedMaceFallAndRespectsDelay() {
        MaceDmg maceDmg = enabled(new MaceDmg());

        assertTrue(maceDmg.shouldAttack(0L, target(false), smashContext(3.0D, 0.9D)));
        assertFalse(maceDmg.shouldAttack(1L, target(false), smashContext(4.0D, 1.0D)));
        assertTrue(maceDmg.shouldAttack(5L, target(false), smashContext(5.0D, 1.0D)));
    }

    @Test
    void rejectsFabricatedOrUnsafeSmashConditionsAndFriends() {
        MaceDmg maceDmg = enabled(new MaceDmg());

        assertFalse(maceDmg.shouldAttack(0L, target(false), smashContext(1.5D, 1.0D)));
        assertFalse(maceDmg.shouldAttack(0L, target(false),
                new MaceDmg.Context(true, true, 1.0D, false, false, false,
                        true, false, 10.0D, -1.0D)));
        assertFalse(maceDmg.shouldAttack(0L, target(false),
                new MaceDmg.Context(true, true, 1.0D, false, false, false,
                        false, false, 10.0D, 0.1D)));
        assertFalse(maceDmg.shouldAttack(0L, target(true), smashContext(10.0D, 1.0D)));
    }

    @Test
    void configurableKeyGateAndSettingBoundsAreEnforced() {
        MaceDmg maceDmg = enabled(new MaceDmg());
        MaceDmg.Context noAttackKey = new MaceDmg.Context(true, false, 1.0D, false,
                false, false, false, false, 5.0D, -0.5D);
        assertFalse(maceDmg.shouldAttack(0L, target(false), noAttackKey));

        ((BooleanSetting) setting(maceDmg, "require_attack_key")).set(false);
        assertTrue(maceDmg.shouldAttack(0L, target(false), noAttackKey));

        NumberSetting fallDistance = (NumberSetting) setting(maceDmg, "minimum_fall_distance");
        assertThrows(IllegalArgumentException.class, () -> fallDistance.set(1.5D));
        assertThrows(IllegalArgumentException.class,
                () -> new MaceDmg.Context(true, true, Double.NaN, false,
                        false, false, false, false, 5.0D, -0.5D));
    }

    @Test
    void contextLossClearsTransientDelay() {
        MaceDmg maceDmg = enabled(new MaceDmg());
        assertTrue(maceDmg.shouldAttack(10L, target(false), smashContext(5.0D, 1.0D)));
        maceDmg.onContextLost();
        assertTrue(maceDmg.shouldAttack(0L, target(false), smashContext(5.0D, 1.0D)));
    }

    private static MaceDmg.Context smashContext(double fallDistance, double charge) {
        return new MaceDmg.Context(true, true, charge, false, false,
                false, false, false, fallDistance, -0.5D);
    }

    private static CombatTarget target(boolean friend) {
        return new CombatTarget("target", "target", CombatEntityType.PLAYER, friend, false,
                true, true, true, 3.0D, 0.0D, 0.0D, -1.0D, 3.0D,
                0.0D, 0.0D, 0.0D, 20.0D, 0, "minecraft:mace", List.of());
    }

    private static Setting<?> setting(MaceDmg module, String id) {
        return module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static MaceDmg enabled(MaceDmg module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
