package dev.helikon.client.setting;

/** An immutable, finite inclusive decimal range. */
public record NumberRange(double minimum, double maximum) {
    public NumberRange {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("Range bounds must be finite and ordered");
        }
    }
}
