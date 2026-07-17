package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.BooleanSupplier;

/** A validated ARGB color setting stored as a portable {@code #AARRGGBB} token. */
public final class ColorSetting extends Setting<Integer> {
    public ColorSetting(String id, String name, String description, int defaultValue) {
        this(id, name, description, defaultValue, () -> true);
    }

    public ColorSetting(
            String id,
            String name,
            String description,
            int defaultValue,
            BooleanSupplier visibilityPredicate
    ) {
        super(id, name, description, defaultValue, visibilityPredicate);
    }

    @Override
    protected boolean isValid(Integer value) {
        return value != null;
    }

    @Override
    protected JsonElement serialize(Integer value) {
        return new JsonPrimitive(ColorSettingText.format(value));
    }

    @Override
    protected Integer deserialize(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected an ARGB color token");
        }
        return ColorSettingText.parse(element.getAsString());
    }
}
