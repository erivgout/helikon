package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RainbowEnchantTest {
    @Test
    void usesTheConfiguredColorOrDeterministicRainbowMode() {
        RainbowEnchant module = new RainbowEnchant();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);

        colorSetting(module, "color").set(0xAA123456);
        assertEquals(0xAA123456, module.glintColor(0L));

        booleanSetting(module, "rainbow").set(true);
        numberSetting(module, "rainbow_speed").set(1.0D);
        assertEquals(0xFFFF0000, module.glintColor(0L));
        assertEquals(0xFF01FF00, module.glintColor(333L));
        assertEquals(0xFF0100FF, module.glintColor(667L));
    }

    private static BooleanSetting booleanSetting(RainbowEnchant module, String id) {
        return (BooleanSetting) setting(module, id);
    }

    private static ColorSetting colorSetting(RainbowEnchant module, String id) {
        return (ColorSetting) setting(module, id);
    }

    private static NumberSetting numberSetting(RainbowEnchant module, String id) {
        return (NumberSetting) setting(module, id);
    }

    private static Object setting(RainbowEnchant module, String id) {
        return module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
