package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiEntityPushTest {
    @Test
    void isDisabledByDefaultAndSuppressesCollisionPushOnlyWhileEnabled() {
        AntiEntityPush module = new AntiEntityPush();

        assertEquals("anti_entity_push", module.id());
        assertEquals(ModuleCategory.MOVEMENT, module.category());
        assertFalse(module.defaultEnabled());
        assertFalse(module.preventsCollisionPush(false));

        module.enable();
        assertTrue(module.preventsCollisionPush(false));
        module.disable();
        assertFalse(module.preventsCollisionPush(false));
    }

    @Test
    void optionalSneakingGateIsHonoredAndResetsToTheGeneralDefault() {
        AntiEntityPush module = new AntiEntityPush();
        BooleanSetting onlyWhileSneaking = (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("only_while_sneaking"))
                .findFirst()
                .orElseThrow();
        module.enable();

        onlyWhileSneaking.set(true);
        assertFalse(module.preventsCollisionPush(false));
        assertTrue(module.preventsCollisionPush(true));

        module.resetSettings();
        assertFalse(onlyWhileSneaking.value());
        assertTrue(module.preventsCollisionPush(false));
    }
}
