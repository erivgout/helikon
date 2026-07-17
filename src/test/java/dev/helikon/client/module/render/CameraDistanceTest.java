package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CameraDistanceTest {
    @Test
    void distanceIsDisabledAtVanillaDefaultAndUsesTheBoundedConfiguredValueWhenEnabled() {
        CameraDistance module = new CameraDistance();
        assertEquals(4.0F, module.distance());
        NumberSetting distance = (NumberSetting) module.settings().getFirst();
        distance.set(12.0D);
        new ModuleRegistry().register(module);
        module.enable();
        assertEquals(12.0F, module.distance());
        module.disable();
        assertEquals(4.0F, module.distance());
    }
}
