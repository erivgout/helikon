package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WindChargeTest {
    private static final WindCharge.Context FALLING = context(true, false, 5.0D, 75.0D, 2, 4);

    @Test
    void defaultsOffInTheUtilityCategoryWithBoundedCooldown() {
        WindCharge module = new WindCharge();

        assertEquals("wind_charge", module.id());
        assertEquals(ModuleCategory.MISCELLANEOUS, module.category());
        assertFalse(module.defaultEnabled());
        assertEquals(WindCharge.Action.none(), module.update(0L, FALLING));

        IntegerSetting cooldown = (IntegerSetting) setting(module, "cooldown_ticks");
        assertEquals(1, cooldown.minimum());
        assertEquals(40, cooldown.maximum());
        assertThrows(IllegalArgumentException.class, () -> cooldown.set(0));
        assertThrows(IllegalArgumentException.class, () -> cooldown.set(41));
    }

    @Test
    void fallingTriggerSelectsUsesAndRestoresOnlyTheOwnedSlot() {
        WindCharge module = enabled();

        assertEquals(new WindCharge.Action(WindCharge.ActionType.SELECT_AND_USE, 4),
                module.update(10L, FALLING));
        assertEquals(new WindCharge.Action(WindCharge.ActionType.RESTORE, 2),
                module.update(11L, context(false, false, 0.0D, 0.0D, 4, 4)));

        assertEquals(WindCharge.Action.none(), module.update(12L, FALLING));
        assertEquals(new WindCharge.Action(WindCharge.ActionType.USE, 4),
                module.update(20L, context(true, false, 5.0D, 75.0D, 4, 4)));
    }

    @Test
    void userSlotChangeRelinquishesRestorationAndUnsafeConditionsDoNothing() {
        WindCharge module = enabled();
        assertEquals(WindCharge.ActionType.SELECT_AND_USE, module.update(0L, FALLING).type());

        assertEquals(WindCharge.Action.none(), module.update(1L, context(false, false, 0.0D, 0.0D, 6, 4)));
        assertEquals(WindCharge.Action.none(),
                module.update(20L, context(true, false, 5.0D, 45.0D, 2, 4)));
        assertEquals(WindCharge.Action.none(),
                module.update(20L, context(false, true, 0.0D, 75.0D, 2, 4)));
    }

    @Test
    void jumpAndCombinedTriggersHonorTheHeldJumpCondition() {
        WindCharge module = enabled();
        @SuppressWarnings("unchecked")
        EnumSetting<WindCharge.Trigger> trigger =
                (EnumSetting<WindCharge.Trigger>) setting(module, "trigger");
        trigger.set(WindCharge.Trigger.JUMP_KEY);

        assertEquals(WindCharge.Action.none(),
                module.update(0L, context(false, false, 0.0D, 75.0D, 2, 4)));
        assertEquals(WindCharge.ActionType.SELECT_AND_USE,
                module.update(0L, context(false, true, 0.0D, 75.0D, 2, 4)).type());
    }

    private static WindCharge enabled() {
        WindCharge module = new WindCharge();
        module.enable();
        return module;
    }

    private static WindCharge.Context context(boolean falling, boolean jumpHeld, double fallDistance,
                                              double pitch, int currentSlot, int chargeSlot) {
        return new WindCharge.Context(true, false, false, falling, jumpHeld, fallDistance, pitch, false,
                currentSlot, chargeSlot);
    }

    private static dev.helikon.client.setting.Setting<?> setting(WindCharge module, String id) {
        return module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
