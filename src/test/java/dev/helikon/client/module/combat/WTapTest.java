package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.movement.MovementInput;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WTapTest {
    private static final MovementInput SPRINT_FORWARD =
            new MovementInput(true, false, false, false, false, false, true);

    @Test
    void disabledModuleNeverReleasesForward() {
        WTap wtap = new WTap();
        wtap.onAttack(eligibleAttack());
        assertSame(SPRINT_FORWARD, wtap.apply(SPRINT_FORWARD, false));
    }

    @Test
    void eligibleAttackReleasesForwardForConfiguredTicksThenResumes() {
        WTap wtap = enabled(new WTap());
        numberSetting(wtap, "release_ticks").set(2.0D);
        wtap.onAttack(eligibleAttack());

        MovementInput first = wtap.apply(SPRINT_FORWARD, false);
        assertFalse(first.forward());
        assertFalse(first.sprint());
        MovementInput second = wtap.apply(SPRINT_FORWARD, false);
        assertFalse(second.forward());
        // Pulse exhausted: the third tick leaves the held forward input untouched.
        assertSame(SPRINT_FORWARD, wtap.apply(SPRINT_FORWARD, false));
    }

    @Test
    void openScreenPausesThePulseWithoutConsumingIt() {
        WTap wtap = enabled(new WTap());
        wtap.onAttack(eligibleAttack());
        assertSame(SPRINT_FORWARD, wtap.apply(SPRINT_FORWARD, true));
        assertFalse(wtap.apply(SPRINT_FORWARD, false).forward());
    }

    @Test
    void friendPassiveAndNonSprintAttacksAreIgnoredByDefault() {
        WTap wtap = enabled(new WTap());

        wtap.onAttack(new WTap.AttackContext(CombatEntityType.PLAYER, true, true, true));
        assertSame(SPRINT_FORWARD, wtap.apply(SPRINT_FORWARD, false));

        wtap.onAttack(new WTap.AttackContext(CombatEntityType.PASSIVE, false, true, true));
        assertSame(SPRINT_FORWARD, wtap.apply(SPRINT_FORWARD, false));

        wtap.onAttack(new WTap.AttackContext(CombatEntityType.PLAYER, false, false, true));
        assertSame(SPRINT_FORWARD, wtap.apply(SPRINT_FORWARD, false));
    }

    @Test
    void hostileAttackTriggersWhenAllowed() {
        WTap wtap = enabled(new WTap());
        wtap.onAttack(new WTap.AttackContext(CombatEntityType.HOSTILE, false, true, true));
        assertFalse(wtap.apply(SPRINT_FORWARD, false).forward());
    }

    @Test
    void requireForwardCanBeRelaxed() {
        WTap wtap = enabled(new WTap());
        booleanSetting(wtap, "require_forward").set(false);
        wtap.onAttack(new WTap.AttackContext(CombatEntityType.PLAYER, false, true, false));
        // The player is not holding forward, so there is nothing to release, but the pulse is armed.
        MovementInput heldNow = wtap.apply(SPRINT_FORWARD, false);
        assertFalse(heldNow.forward());
    }

    @Test
    void disableAndPlayerLossClearAnyActivePulse() {
        WTap wtap = enabled(new WTap());
        wtap.onAttack(eligibleAttack());
        wtap.disable();
        wtap.enable();
        assertSame(SPRINT_FORWARD, wtap.apply(SPRINT_FORWARD, false));

        wtap.onAttack(eligibleAttack());
        wtap.onPlayerUnavailable();
        assertSame(SPRINT_FORWARD, wtap.apply(SPRINT_FORWARD, false));
    }

    private static WTap.AttackContext eligibleAttack() {
        return new WTap.AttackContext(CombatEntityType.PLAYER, false, true, true);
    }

    private static <T extends Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static NumberSetting numberSetting(Module module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static BooleanSetting booleanSetting(Module module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
