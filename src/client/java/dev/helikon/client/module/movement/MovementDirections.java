package dev.helikon.client.module.movement;

/** Shared view-relative conversion for Minecraft's positive-left movement input. */
public final class MovementDirections {
    private MovementDirections() {
    }

    /**
     * Converts local positive-left/positive-forward input to world X/Z.
     * The supplied horizontal view vector is expected to point forward.
     */
    public static HorizontalVelocity fromView(double forwardX, double forwardZ,
                                              double sideInput, double forwardInput) {
        if (!Double.isFinite(forwardX) || !Double.isFinite(forwardZ)
                || !Double.isFinite(sideInput) || !Double.isFinite(forwardInput)) {
            throw new IllegalArgumentException("movement direction inputs must be finite");
        }
        return new HorizontalVelocity(
                forwardX * forwardInput + forwardZ * sideInput,
                forwardZ * forwardInput - forwardX * sideInput
        );
    }
}
