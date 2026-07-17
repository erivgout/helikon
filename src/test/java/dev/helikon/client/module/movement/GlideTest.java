package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlideTest {
    @Test
    void capsOnlyEnabledOrdinaryFallDescent() {
        Glide module = new Glide();
        assertFalse(module.verticalVelocity(false, false, false, false, false, false, false, false, -0.30D).isPresent());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        assertEquals(-0.08D, module.verticalVelocity(false, false, false, false, false, false, false, false, -0.30D)
                .orElseThrow(), 0.0001D);
        assertFalse(module.verticalVelocity(false, false, false, false, false, false, false, false, -0.05D).isPresent());
        assertFalse(module.verticalVelocity(true, false, false, false, false, false, false, false, -0.30D).isPresent());
        assertFalse(module.verticalVelocity(false, true, false, false, false, false, false, false, -0.30D).isPresent());
        assertFalse(module.verticalVelocity(false, false, true, false, false, false, false, false, -0.30D).isPresent());
        assertFalse(module.verticalVelocity(false, false, false, true, false, false, false, false, -0.30D).isPresent());
        assertFalse(module.verticalVelocity(false, false, false, false, true, false, false, false, -0.30D).isPresent());
        assertFalse(module.verticalVelocity(false, false, false, false, false, true, false, false, -0.30D).isPresent());
        assertFalse(module.verticalVelocity(false, false, false, false, false, false, true, false, -0.30D).isPresent());
        assertFalse(module.verticalVelocity(false, false, false, false, false, false, false, true, -0.30D).isPresent());

        numberSetting(module).set(0.20D);
        assertEquals(-0.20D, module.verticalVelocity(false, false, false, false, false, false, false, false, -0.30D)
                .orElseThrow(), 0.0001D);
    }

    @Test
    void rejectsNonFiniteVelocity() {
        Glide module = new Glide();
        assertThrows(IllegalArgumentException.class, () -> module.verticalVelocity(false, false, false, false,
                false, false, false, false, Double.NaN));
    }

    private static NumberSetting numberSetting(Glide module) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals("descent_speed"))
                .findFirst().orElseThrow();
    }
}
