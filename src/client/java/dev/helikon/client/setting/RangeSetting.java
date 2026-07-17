package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.function.BooleanSupplier;

/** A validated inclusive numeric subrange constrained within configured global bounds. */
public final class RangeSetting extends Setting<NumberRange> {
    private final double minimum;
    private final double maximum;

    public RangeSetting(
            String id,
            String name,
            String description,
            NumberRange defaultValue,
            double minimum,
            double maximum
    ) {
        this(id, name, description, defaultValue, minimum, maximum, () -> true);
    }

    public RangeSetting(
            String id,
            String name,
            String description,
            NumberRange defaultValue,
            double minimum,
            double maximum,
            BooleanSupplier visibilityPredicate
    ) {
        super(id, name, description, defaultValue, visibilityPredicate);
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("Range-setting bounds must be finite and ordered");
        }
        this.minimum = minimum;
        this.maximum = maximum;
        if (!isValid(defaultValue())) {
            throw new IllegalArgumentException("Default range must be inside the configured bounds");
        }
    }

    public double minimum() {
        return minimum;
    }

    public double maximum() {
        return maximum;
    }

    @Override
    protected boolean isValid(NumberRange value) {
        return value != null && value.minimum() >= minimum && value.maximum() <= maximum;
    }

    @Override
    protected JsonElement serialize(NumberRange value) {
        JsonObject object = new JsonObject();
        object.addProperty("minimum", value.minimum());
        object.addProperty("maximum", value.maximum());
        return object;
    }

    @Override
    protected NumberRange deserialize(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("Expected a range object");
        }
        JsonObject object = element.getAsJsonObject();
        if (!object.has("minimum") || !object.has("maximum") || !object.get("minimum").isJsonPrimitive()
                || !object.get("minimum").getAsJsonPrimitive().isNumber() || !object.get("maximum").isJsonPrimitive()
                || !object.get("maximum").getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Invalid range object");
        }
        return new NumberRange(object.get("minimum").getAsDouble(), object.get("maximum").getAsDouble());
    }
}
