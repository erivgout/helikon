package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiBlindTest {
    @Test
    void onlyEnabledConfiguredEffectsAreSuppressed() {
        AntiBlind antiBlind = new AntiBlind();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(antiBlind);

        assertFalse(antiBlind.hidesBlindness());
        registry.setEnabled(antiBlind, true);
        assertTrue(antiBlind.hidesBlindness());
        assertTrue(antiBlind.hidesDarkness());
        assertTrue(antiBlind.hidesNausea());
        assertTrue(antiBlind.hidesPumpkinOverlay());
        assertTrue(antiBlind.hidesPowderSnowOverlay());

        booleanSetting(antiBlind, "nausea").set(false);
        booleanSetting(antiBlind, "pumpkin_overlay").set(false);
        assertFalse(antiBlind.hidesNausea());
        assertFalse(antiBlind.hidesPumpkinOverlay());
        assertTrue(antiBlind.hidesBlindness());
    }

    private static BooleanSetting booleanSetting(AntiBlind module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
