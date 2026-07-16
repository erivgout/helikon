package dev.helikon.client.module.render;

import java.util.Objects;

/**
 * Owns the reversible gamma override used by {@link Fullbright}. Keeping this
 * state separate from Minecraft makes the restoration behavior unit-testable.
 */
public final class FullbrightGammaController {
    private boolean applying;
    private double originalGamma;

    /** Applies or removes the gamma override for the currently selected mode. */
    public void reconcile(GammaAccess gamma, boolean gammaMode, double brightness) {
        GammaAccess nonNullGamma = Objects.requireNonNull(gamma, "gamma");
        if (gammaMode) {
            if (!applying) {
                originalGamma = nonNullGamma.gamma();
                applying = true;
            }
            nonNullGamma.setGamma(brightness);
            return;
        }

        restore(nonNullGamma);
    }

    /** Restores exactly the gamma value that was present before the override. */
    public void restore(GammaAccess gamma) {
        GammaAccess nonNullGamma = Objects.requireNonNull(gamma, "gamma");
        if (!applying) {
            return;
        }

        nonNullGamma.setGamma(originalGamma);
        applying = false;
    }

    public boolean isApplying() {
        return applying;
    }

    /** Thin platform port for Minecraft's client-side gamma option. */
    public interface GammaAccess {
        double gamma();

        void setGamma(double value);
    }
}
