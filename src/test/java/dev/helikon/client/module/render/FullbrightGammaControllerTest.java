package dev.helikon.client.module.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullbrightGammaControllerTest {
    @Test
    void restoresOriginalGammaAfterDisable() {
        RecordingGamma gamma = new RecordingGamma(0.35);
        FullbrightGammaController controller = new FullbrightGammaController();

        controller.reconcile(gamma, true, 1.0);
        assertEquals(1.0, gamma.value);
        assertTrue(controller.isApplying());

        controller.reconcile(gamma, true, 0.75);
        assertEquals(0.75, gamma.value);

        controller.restore(gamma);
        assertEquals(0.35, gamma.value);
        assertFalse(controller.isApplying());
    }

    @Test
    void recapturesGammaWhenTheModeIsTurnedBackOn() {
        RecordingGamma gamma = new RecordingGamma(0.2);
        FullbrightGammaController controller = new FullbrightGammaController();

        controller.reconcile(gamma, true, 1.0);
        controller.reconcile(gamma, false, 1.0);
        assertEquals(0.2, gamma.value);

        gamma.value = 0.6;
        controller.reconcile(gamma, true, 0.9);
        controller.restore(gamma);

        assertEquals(0.6, gamma.value);
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
}
