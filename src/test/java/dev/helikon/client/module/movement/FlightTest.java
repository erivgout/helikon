package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightTest {
    @Test
    void velocityFlightAppliesOnlyWithoutAbilityFlight() {
        Flight flight = enabled();

        assertTrue(flight.usesVelocityFlight(false));
        assertFalse(flight.usesVelocityFlight(true));

        booleanSetting(flight, "survival_velocity").set(false);
        assertFalse(flight.usesVelocityFlight(false));
    }

    @Test
    void freecamViewSuppressesVelocityAndBoatFlight() {
        Flight flight = enabled();
        booleanSetting(flight, "freecam_view").set(true);

        assertFalse(flight.usesVelocityFlight(false));
        assertFalse(flight.usesBoatFlight());
    }

    @Test
    void flightVelocityNormalizesDirectionAndAppliesVerticalInput() {
        Flight flight = enabled();

        Flight.FlightVelocity hover = flight.flightVelocity(new HorizontalVelocity(0.0D, 0.0D), false, false, false);
        assertEquals(0.0D, hover.x());
        assertEquals(0.0D, hover.y());
        assertEquals(0.0D, hover.z());

        Flight.FlightVelocity moving = flight.flightVelocity(new HorizontalVelocity(3.0D, 4.0D), true, false, false);
        assertEquals(0.5D * 3.0D / 5.0D, moving.x(), 1.0E-9D);
        assertEquals(0.5D, moving.y(), 1.0E-9D);
        assertEquals(0.5D * 4.0D / 5.0D, moving.z(), 1.0E-9D);

        Flight.FlightVelocity sinking = flight.flightVelocity(new HorizontalVelocity(0.0D, 0.0D), false, true, true);
        assertEquals(-0.6D, sinking.y(), 1.0E-9D);
    }

    private static Flight enabled() {
        Flight flight = new Flight();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(flight);
        registry.setEnabled(flight, true);
        return flight;
    }

    private static BooleanSetting booleanSetting(Flight module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
