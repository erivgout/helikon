package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RainbowUiAccessTest {
    @Test
    void suppliesAnimatedAccentOnlyWhileRgbModuleIsEnabled() {
        int fallback = 0xFF123456;
        RainbowUi rgb = new RainbowUi();
        RainbowUiAccess.install(rgb);
        assertEquals(fallback, RainbowUiAccess.accent(10_000L, fallback));

        ModuleRegistry modules = new ModuleRegistry();
        modules.register(rgb);
        modules.setEnabled(rgb, true);
        assertNotEquals(fallback, RainbowUiAccess.accent(10_000L, fallback));

        RainbowUiAccess.install(new RainbowUi());
    }
}
