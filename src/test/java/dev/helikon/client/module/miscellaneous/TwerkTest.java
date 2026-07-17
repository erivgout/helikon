package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.movement.MovementInput;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TwerkTest {
    private static final MovementInput STANDING = new MovementInput(false, false, false, false, false, false, false);

    @Test
    void pulsesLocalSneakAtTheConfiguredHalfCycleWithoutOverridingPhysicalSneak() {
        Twerk twerk = enabledModule();
        halfCycle(twerk).set(2.0D);

        assertEquals(withSneak(STANDING), twerk.apply(STANDING, false));
        assertEquals(withSneak(STANDING), twerk.apply(STANDING, false));
        assertEquals(STANDING, twerk.apply(STANDING, false));
        assertEquals(STANDING, twerk.apply(STANDING, false));
        assertEquals(withSneak(STANDING), twerk.apply(STANDING, false));

        MovementInput physicalSneak = withSneak(STANDING);
        assertEquals(physicalSneak, twerk.apply(physicalSneak, false));
        assertEquals(physicalSneak, twerk.apply(physicalSneak, false));
    }

    @Test
    void screensAndDisableReturnFreshInputAndResetThePulse() {
        Twerk twerk = enabledModule();
        twerk.apply(STANDING, false);

        assertEquals(STANDING, twerk.apply(STANDING, true));
        assertEquals(withSneak(STANDING), twerk.apply(STANDING, false));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(twerk);
        registry.setEnabled(twerk, false);
        assertEquals(STANDING, twerk.apply(STANDING, false));
    }

    private static Twerk enabledModule() {
        Twerk twerk = new Twerk();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(twerk);
        registry.setEnabled(twerk, true);
        return twerk;
    }

    @SuppressWarnings("unchecked")
    private static NumberSetting halfCycle(Twerk twerk) {
        return (NumberSetting) twerk.settings().stream()
                .filter(setting -> setting.id().equals("half_cycle_ticks"))
                .findFirst()
                .orElseThrow();
    }

    private static MovementInput withSneak(MovementInput input) {
        return new MovementInput(input.forward(), input.backward(), input.left(), input.right(),
                input.jump(), true, input.sprint());
    }
}
