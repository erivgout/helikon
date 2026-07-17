package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.BooleanSupplier;

/** A finite integral setting constrained to an inclusive range. */
public final class IntegerSetting extends Setting<Integer> {
    private final int minimum;
    private final int maximum;

    public IntegerSetting(String id, String name, String description, int defaultValue, int minimum, int maximum) {
        this(id, name, description, defaultValue, minimum, maximum, () -> true);
    }

    public IntegerSetting(
            String id,
            String name,
            String description,
            int defaultValue,
            int minimum,
            int maximum,
            BooleanSupplier visibilityPredicate
    ) {
        super(id, name, description, defaultValue, visibilityPredicate);
        if (minimum > maximum) {
            throw new IllegalArgumentException("Integer setting bounds must be ordered");
        }
        if (defaultValue < minimum || defaultValue > maximum) {
            throw new IllegalArgumentException("Default value must be inside the configured range");
        }
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public int minimum() {
        return minimum;
    }

    public int maximum() {
        return maximum;
    }

    @Override
    protected boolean isValid(Integer value) {
        return value != null && value >= minimum && value <= maximum;
    }

    @Override
    protected JsonElement serialize(Integer value) {
        return new JsonPrimitive(value);
    }

    @Override
    protected Integer deserialize(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Expected an integer");
        }
        double parsed = element.getAsDouble();
        if (!Double.isFinite(parsed) || parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE
                || Math.rint(parsed) != parsed) {
            throw new IllegalArgumentException("Expected an integer");
        }
        return (int) parsed;
    }
}
