package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FakeLagTest {
    @Test
    void identityMatchesTheModuleContract() {
        FakeLag fakeLag = new FakeLag();

        assertEquals("fakelag", fakeLag.id());
        assertEquals(ModuleCategory.MOVEMENT, fakeLag.category());
        // Advantage modules default off.
        assertFalse(fakeLag.defaultEnabled());
        assertEquals(200L, fakeLag.delayMillis());
        assertEquals(20, fakeLag.maxHeldPackets());
    }

    @Test
    void accessorsRoundConfiguredValues() {
        FakeLag fakeLag = new FakeLag();

        delay(fakeLag).set(349.6D);
        maxHeld(fakeLag).set(7.4D);

        assertEquals(350L, fakeLag.delayMillis());
        assertEquals(7, fakeLag.maxHeldPackets());
    }

    @Test
    void settingsRejectOutOfRangeValues() {
        FakeLag fakeLag = new FakeLag();

        assertThrows(IllegalArgumentException.class, () -> delay(fakeLag).set(0.0D));
        assertThrows(IllegalArgumentException.class, () -> maxHeld(fakeLag).set(0.0D));
    }

    private static NumberSetting delay(FakeLag fakeLag) {
        return (NumberSetting) fakeLag.settings().stream()
                .filter(setting -> setting.id().equals("delay_ms"))
                .findFirst()
                .orElseThrow();
    }

    private static NumberSetting maxHeld(FakeLag fakeLag) {
        return (NumberSetting) fakeLag.settings().stream()
                .filter(setting -> setting.id().equals("max_held"))
                .findFirst()
                .orElseThrow();
    }
}
