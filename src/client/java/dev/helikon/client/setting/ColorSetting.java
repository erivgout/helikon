package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A validated ARGB color setting stored as a portable {@code #AARRGGBB} token. */
public final class ColorSetting extends Setting<Integer> {
    public ColorSetting(String id, String name, String description, int defaultValue) {
        super(id, name, description, defaultValue);
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
