package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.ColorSettingText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterCrosshairTest {
    @Test
    void hideVanillaOptionOnlyAppliesWhileTheModuleIsEnabled() {
        BetterCrosshair crosshair = new BetterCrosshair();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(crosshair);

        assertFalse(crosshair.hidesVanillaCrosshair());
        registry.setEnabled(crosshair, true);
        assertTrue(crosshair.hidesVanillaCrosshair());

        ColorSetting color = (ColorSetting) crosshair.settings().stream()
                .filter(setting -> setting.id().equals("color"))
                .findFirst()
                .orElseThrow();
        assertTrue(ColorSettingText.tryApply(color, "#80FF6600"));
        assertEquals(0x80FF6600, crosshair.color());
    }
}
