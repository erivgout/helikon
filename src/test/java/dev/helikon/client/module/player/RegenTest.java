package dev.helikon.client.module.player;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegenTest {
    @Test
    void requestsOnlyBoundedFedGroundedHurtBursts() {
        Regen regen = enabled(new Regen());
        Regen.Context eligible = context(10.0F, 20, true);

        assertEquals(5, regen.packetCount(0L, eligible));
        assertEquals(0, regen.packetCount(0L, eligible));
        assertEquals(5, regen.packetCount(1L, eligible));
        assertEquals(0, regen.packetCount(2L, context(20.0F, 20, true)));
        assertEquals(0, regen.packetCount(2L, context(10.0F, 10, true)));
        assertEquals(0, regen.packetCount(2L, context(10.0F, 20, false)));
    }

    @Test
    void packetAndCadenceSettingsStayBounded() {
        Regen regen = enabled(new Regen());
        integerSetting(regen, "packets_per_burst").set(10);
        integerSetting(regen, "delay_ticks").set(4);

        assertEquals(10, regen.packetCount(0L, context(10.0F, 20, true)));
        assertEquals(0, regen.packetCount(3L, context(10.0F, 20, true)));
        assertEquals(10, regen.packetCount(4L, context(10.0F, 20, true)));
    }

    private static Regen.Context context(float health, int food, boolean onGround) {
        return new Regen.Context(health, 20.0F, food, false, onGround, false, false, false);
    }

    private static Regen enabled(Regen module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static IntegerSetting integerSetting(Regen module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
