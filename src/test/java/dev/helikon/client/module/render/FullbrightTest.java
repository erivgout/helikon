package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FullbrightTest {
    @Test
    void appliesSettingsWhileEnabledAndRestoresAllStateOnDisable() {
        RecordingGamma gamma = new RecordingGamma(0.4);
        RecordingNightVision nightVision = new RecordingNightVision();
        Fullbright fullbright = new Fullbright(gamma, nightVision);
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(fullbright);

        registry.setEnabled(fullbright, true);
        assertEquals(1.0, gamma.value);
        assertEquals(0, nightVision.applyCalls);

        fullbright.brightness().set(0.8);
        assertEquals(0.8, gamma.value);

        fullbright.nightVisionMode().set(true);
        assertEquals(1, nightVision.applyCalls);
        fullbright.tick();
        assertEquals(2, nightVision.applyCalls);

        fullbright.nightVisionMode().set(false);
        assertEquals(1, nightVision.restoreCalls);

        registry.setEnabled(fullbright, false);
        assertEquals(0.4, gamma.value);
        assertEquals(1, nightVision.restoreCalls);
        assertFalse(fullbright.isEnabled());
    }

    @Test
    void changesWhileDisabledDoNotTouchClientState() {
        RecordingGamma gamma = new RecordingGamma(0.25);
        RecordingNightVision nightVision = new RecordingNightVision();
        Fullbright fullbright = new Fullbright(gamma, nightVision);

        fullbright.brightness().set(0.7);
        fullbright.nightVisionMode().set(true);

        assertEquals(0.25, gamma.value);
        assertEquals(0, nightVision.applyCalls);
        assertEquals(0, nightVision.restoreCalls);
    }

    private static final class RecordingGamma implements FullbrightGammaController.GammaAccess {
        private double value;

        private RecordingGamma(double value) {
            this.value = value;
        }

        @Override
        public double gamma() {
            return value;
        }

        @Override
        public void setGamma(double value) {
            this.value = value;
        }
    }

    private static final class RecordingNightVision implements Fullbright.NightVisionAccess {
        private int applyCalls;
        private int restoreCalls;

        @Override
        public void apply() {
            applyCalls++;
        }

        @Override
        public void restore() {
            restoreCalls++;
        }
    }
}
