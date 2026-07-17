package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.BooleanSupplier;

/** A finite decimal setting constrained to an inclusive range. */
public final class NumberSetting extends Setting<Double> {
    private final double minimum;
    private final double maximum;

    public NumberSetting(
            String id,
            String name,
            String description,
            double defaultValue,
            double minimum,
            double maximum
    ) {
        this(id, name, description, defaultValue, minimum, maximum, () -> true);
    }

    public NumberSetting(
            String id,
            String name,
            String description,
            double defaultValue,
            double minimum,
            double maximum,
            BooleanSupplier visibilityPredicate
    ) {
        super(id, name, description, defaultValue, visibilityPredicate);
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("Number setting bounds must be finite and ordered");
        }
        if (!Double.isFinite(defaultValue)) {
            throw new IllegalArgumentException("Default value must be finite");
        }
        if (defaultValue < minimum || defaultValue > maximum) {
            throw new IllegalArgumentException("Default value must be inside the configured range");
        }
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public double minimum() {
        return minimum;
    }

    public double maximum() {
        return maximum;
    }

    @Override
    protected boolean isValid(Double value) {
        return value != null
                && Double.isFinite(value)
                && value >= minimum
                && value <= maximum;
    }

    @Override
    protected JsonElement serialize(Double value) {
        return new JsonPrimitive(value);
    }

    @Override
    protected Double deserialize(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Expected a number");
        }

        double parsed = element.getAsDouble();
        if (!Double.isFinite(parsed)) {
            throw new IllegalArgumentException("Number must be finite");
        }
        return parsed;
    }
}
