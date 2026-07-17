package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.BooleanSupplier;

/** A boolean module setting. */
public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String id, String name, String description, boolean defaultValue) {
        this(id, name, description, defaultValue, () -> true);
    }

    public BooleanSetting(
            String id,
            String name,
            String description,
            boolean defaultValue,
            BooleanSupplier visibilityPredicate
    ) {
        super(id, name, description, defaultValue, visibilityPredicate);
    }

    @Override
    protected boolean isValid(Boolean value) {
        return value != null;
    }

    @Override
    protected JsonElement serialize(Boolean value) {
        return new JsonPrimitive(value);
    }

    @Override
    protected Boolean deserialize(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Expected a boolean");
        }
        return element.getAsBoolean();
    }
}
