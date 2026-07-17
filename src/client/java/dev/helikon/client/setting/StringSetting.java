package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Objects;

/** A bounded local text setting, persisted without interpretation. */
public final class StringSetting extends Setting<String> {
    private final int maximumLength;
    private final boolean allowBlank;

    public StringSetting(String id, String name, String description, String defaultValue,
                         int maximumLength, boolean allowBlank) {
        super(id, name, description, Objects.requireNonNull(defaultValue, "defaultValue"));
        if (maximumLength < 1) {
            throw new IllegalArgumentException("maximumLength must be positive");
        }
        if (defaultValue.length() > maximumLength || (!allowBlank && defaultValue.isBlank())) {
            throw new IllegalArgumentException("Default value is outside the configured text constraints");
        }
        this.maximumLength = maximumLength;
        this.allowBlank = allowBlank;
    }

    public int maximumLength() {
        return maximumLength;
    }

    public boolean allowBlank() {
        return allowBlank;
    }

    @Override
    protected boolean isValid(String value) {
        return value != null && value.length() <= maximumLength && (allowBlank || !value.isBlank());
    }

    @Override
    protected JsonElement serialize(String value) {
        return new JsonPrimitive(value);
    }

    @Override
    protected String deserialize(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected text");
        }
        return element.getAsString();
    }
}
