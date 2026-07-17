package dev.helikon.client.module.player;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThrowDebuffTest {
    private static final ThrowDebuff.PotionCandidate POISON =
            new ThrowDebuff.PotionCandidate(4, "poison", true);
    private static final CombatTarget OPPONENT = target("opponent", false, 4.0D);

    @Test
    void defaultsOffAsAPlayerModuleAndUsesBoundedSettings() {
        ThrowDebuff module = new ThrowDebuff();

        assertEquals("throw_debuff", module.id());
        assertEquals(ModuleCategory.PLAYER, module.category());
        assertFalse(module.defaultEnabled());
        assertEquals(ThrowDebuff.Action.none(), module.update(0L, context(2, List.of(POISON), List.of(OPPONENT))));

        NumberSetting range = (NumberSetting) setting(module, "range");
        NumberSetting delay = (NumberSetting) setting(module, "delay_ticks");
        assertThrows(IllegalArgumentException.class, () -> range.set(12.01D));
        assertThrows(IllegalArgumentException.class, () -> delay.set(4.99D));
    }

    @Test
    void selectsAValidPotionThrowsAtNearestOpponentAndRestoresOwnedSlot() {
        ThrowDebuff module = enabled();

        ThrowDebuff.Action throwAction = module.update(10L, context(2, List.of(POISON), List.of(OPPONENT)));
        assertEquals(ThrowDebuff.ActionType.SELECT_AND_THROW, throwAction.type());
        assertEquals(4, throwAction.slot());
        assertEquals("opponent", throwAction.targetId());
        assertEquals(new ThrowDebuff.Action(ThrowDebuff.ActionType.RESTORE_SLOT, 2, "", 0.0F, 0.0F, false),
                module.update(11L, context(4, List.of(), List.of())));
    }

    @Test
    void excludesFriendsByDefaultAndNeverSelectsMixedOrBeneficialPotions() {
        ThrowDebuff module = enabled();
        CombatTarget friend = target("friend", true, 2.0D);
        ThrowDebuff.PotionCandidate mixed = new ThrowDebuff.PotionCandidate(3, "turtle_master", false);

        assertEquals(ThrowDebuff.Action.none(),
                module.update(0L, context(2, List.of(POISON), List.of(friend))));
        assertEquals(ThrowDebuff.Action.none(),
                module.update(0L, context(2, List.of(mixed), List.of(OPPONENT))));

        ((BooleanSetting) setting(module, "exclude_friends")).set(false);
        assertEquals(ThrowDebuff.ActionType.SELECT_AND_THROW,
                module.update(0L, context(2, List.of(POISON), List.of(friend))).type());
    }

    @Test
    void optionalWhitelistAndCooldownAreHonored() {
        ThrowDebuff module = enabled();
        ((StringSetting) setting(module, "potion_whitelist")).set("weakness");

        assertEquals(ThrowDebuff.Action.none(),
                module.update(0L, context(2, List.of(POISON), List.of(OPPONENT))));
        ThrowDebuff.PotionCandidate weakness = new ThrowDebuff.PotionCandidate(2, "weakness", true);
        assertEquals(ThrowDebuff.ActionType.THROW_SELECTED,
                module.update(0L, context(2, List.of(weakness), List.of(OPPONENT))).type());
        module.update(1L, context(2, List.of(), List.of()));
        assertEquals(ThrowDebuff.Action.none(),
                module.update(10L, context(2, List.of(weakness), List.of(OPPONENT))));
    }

    private static ThrowDebuff enabled() {
        ThrowDebuff module = new ThrowDebuff();
        module.enable();
        return module;
    }

    private static ThrowDebuff.Context context(int slot, List<ThrowDebuff.PotionCandidate> potions,
                                               List<CombatTarget> targets) {
        return new ThrowDebuff.Context(slot, false, false, potions, targets);
    }

    private static CombatTarget target(String id, boolean friend, double distance) {
        return new CombatTarget(id, id, CombatEntityType.PLAYER, friend, false, true, true, true,
                distance, 20.0D, 1.0D, 0.0D, distance, 0.0D, 0.0D, 0.0D,
                20.0D, 0, "", List.of());
    }

    private static dev.helikon.client.setting.Setting<?> setting(ThrowDebuff module, String id) {
        return module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
