package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoatFlightTest {
    @Test
    void isAnIndependentMovementModule() {
        BoatFlight boatFlight = enabled();

        assertEquals("boat_flight", boatFlight.id());
        assertEquals("Boat Flight", boatFlight.name());
        assertTrue(boatFlight.isEnabled());
    }

    @Test
    void normalizesDirectionAndAppliesVerticalInputAtItsOwnSpeed() {
        BoatFlight boatFlight = enabled();

        BoatFlight.Velocity moving = boatFlight.velocity(new HorizontalVelocity(3.0D, 4.0D), true, false);
        assertEquals(0.6D * 3.0D / 5.0D, moving.x(), 1.0E-9D);
        assertEquals(0.6D, moving.y(), 1.0E-9D);
        assertEquals(0.6D * 4.0D / 5.0D, moving.z(), 1.0E-9D);

        numberSetting(boatFlight, "speed").set(1.25D);
        BoatFlight.Velocity sinking = boatFlight.velocity(new HorizontalVelocity(0.0D, 0.0D), false, true);
        assertEquals(-1.25D, sinking.y(), 1.0E-9D);
    }

    private static BoatFlight enabled() {
        BoatFlight boatFlight = new BoatFlight();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(boatFlight);
        registry.setEnabled(boatFlight, true);
        return boatFlight;
    }

    private static NumberSetting numberSetting(BoatFlight module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
