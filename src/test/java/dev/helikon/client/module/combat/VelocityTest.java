package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VelocityTest {
    @Test
    void defaultsOffAndPassesMotionThroughUntilEnabled() {
        Velocity velocity = new Velocity();

        assertEquals("velocity", velocity.id());
        assertFalse(velocity.defaultEnabled());
        assertEquals(0.0D, velocity.horizontalPercent());
        assertEquals(0.0D, velocity.verticalPercent());
        assertEquals(new Velocity.Motion(1.25D, -0.5D, -2.0D), velocity.adjust(1.25D, -0.5D, -2.0D));
    }

    @Test
    void independentlyScalesHorizontalAndVerticalMotion() {
        Velocity velocity = new Velocity();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(velocity);
        numberSetting(velocity, "horizontal_percent").set(25.0D);
        numberSetting(velocity, "vertical_percent").set(50.0D);
        registry.setEnabled(velocity, true);

        assertEquals(new Velocity.Motion(0.5D, -0.5D, -1.0D), velocity.adjust(2.0D, -1.0D, -4.0D));

        registry.setEnabled(velocity, false);
        assertEquals(new Velocity.Motion(2.0D, -1.0D, -4.0D), velocity.adjust(2.0D, -1.0D, -4.0D));
    }

    @Test
    void settingsStayWithinHonestBoundedPercentages() {
        Velocity velocity = new Velocity();
        NumberSetting horizontal = numberSetting(velocity, "horizontal_percent");
        NumberSetting vertical = numberSetting(velocity, "vertical_percent");

        assertEquals(0.0D, horizontal.minimum());
        assertEquals(200.0D, horizontal.maximum());
        assertEquals(0.0D, vertical.minimum());
        assertEquals(200.0D, vertical.maximum());
        assertThrows(IllegalArgumentException.class, () -> horizontal.set(-0.01D));
        assertThrows(IllegalArgumentException.class, () -> vertical.set(200.01D));
    }

    private static NumberSetting numberSetting(Velocity module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
