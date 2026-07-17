package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoWeatherTest {
    @Test
    void hidesNothingWhileDisabled() {
        NoWeather module = new NoWeather();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);

        assertFalse(module.hidesRain());
        assertFalse(module.hidesSnow());
    }

    @Test
    void hidesBothPrecipitationTypesByDefaultWhenEnabled() {
        NoWeather module = new NoWeather();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);

        registry.setEnabled(module, true);

        assertTrue(module.hidesRain());
        assertTrue(module.hidesSnow());
    }

    @Test
    void eachPrecipitationToggleIsIndependent() {
        NoWeather module = new NoWeather();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        booleanSetting(module, "snow").set(false);
        assertTrue(module.hidesRain());
        assertFalse(module.hidesSnow());

        booleanSetting(module, "rain").set(false);
        booleanSetting(module, "snow").set(true);
        assertFalse(module.hidesRain());
        assertTrue(module.hidesSnow());
    }

    @Test
    void disablingRestoresRenderingRegardlessOfSettings() {
        NoWeather module = new NoWeather();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        assertTrue(module.hidesRain());
        registry.setEnabled(module, false);

        assertFalse(module.hidesRain());
        assertFalse(module.hidesSnow());
    }

    private static BooleanSetting booleanSetting(NoWeather module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
