package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoFogTest {
    @Test
    void onlyEnabledModuleExtendsFogPlanesToTheBoundedConfiguredDistance() {
        NoFog module = new NoFog();
        NoFog.FogPlanes vanilla = new NoFog.FogPlanes(8.0F, 16.0F, 32.0F, 48.0F, 64.0F, 80.0F);

        assertEquals("no_fog", module.id());
        assertEquals(1024.0D, module.distance());
        assertSame(vanilla, module.extend(vanilla));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        NumberSetting distance = (NumberSetting) module.settings().getFirst();
        distance.set(128.0D);

        assertEquals(new NoFog.FogPlanes(128.0F, 128.0F, 129.0F, 129.0F, 129.0F, 129.0F), module.extend(vanilla));
        assertThrows(IllegalArgumentException.class, () -> distance.set(5000.0D));
        assertEquals(128.0D, module.distance());
    }
}
