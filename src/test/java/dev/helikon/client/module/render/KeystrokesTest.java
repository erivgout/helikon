package dev.helikon.client.module.render;

import dev.helikon.client.hud.ClickRateTracker;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeystrokesTest {
    @Test
    void exposesCpsSettingAndClearsCountsOnDisable() {
        Keystrokes keystrokes = new Keystrokes();
        assertTrue(keystrokes.showCps());
        assertEquals("show_cps", keystrokes.settings().get(2).id());
        assertTrue(((BooleanSetting) keystrokes.settings().get(2)).value());

        ModuleRegistry modules = new ModuleRegistry();
        modules.register(keystrokes);
        modules.setEnabled(keystrokes, true);
        keystrokes.recordClick(ClickRateTracker.Button.LEFT, 1L);
        assertEquals(1, keystrokes.clicksPerSecond(ClickRateTracker.Button.LEFT, 2L));

        modules.setEnabled(keystrokes, false);
        assertEquals(0, keystrokes.clicksPerSecond(ClickRateTracker.Button.LEFT, 2L));
    }
}
