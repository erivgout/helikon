package dev.helikon.client.hud;

/** Small client-thread estimate of observed local tick cadence; it is not a server TPS claim. */
public final class ClientTpsEstimate {
    private static final double TARGET_TPS = 20.0D;
    private static final double SMOOTHING = 0.20D;
    private long previousTickNanos = -1L;
    private double tps = TARGET_TPS;

    public void observeTick(long nowNanos) {
        if (nowNanos < 0L) {
            throw new IllegalArgumentException("Tick timestamp must not be negative");
        }
        if (previousTickNanos < 0L) {
            previousTickNanos = nowNanos;
            return;
        }
        long elapsed = nowNanos - previousTickNanos;
        previousTickNanos = nowNanos;
        if (elapsed <= 0L || elapsed > 1_000_000_000L) {
            return;
        }
        double observed = Math.clamp(1_000_000_000.0D / elapsed, 0.0D, TARGET_TPS);
        tps += (observed - tps) * SMOOTHING;
    }

    public double tps() {
        return tps;
    }

    public void reset() {
        previousTickNanos = -1L;
        tps = TARGET_TPS;
    }
}
