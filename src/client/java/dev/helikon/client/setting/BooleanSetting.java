package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A boolean module setting. */
public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String id, String name, String description, boolean defaultValue) {
        super(id, name, description, defaultValue);
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
