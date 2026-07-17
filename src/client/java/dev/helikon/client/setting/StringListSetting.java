package dev.helikon.client.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** A bounded immutable list of local text values. */
public final class StringListSetting extends Setting<List<String>> {
    private final int maximumEntries;
    private final int maximumEntryLength;
    private final boolean allowBlankEntries;

    public StringListSetting(
            String id,
            String name,
            String description,
            List<String> defaultValue,
            int maximumEntries,
            int maximumEntryLength,
            boolean allowBlankEntries
    ) {
        this(id, name, description, defaultValue, maximumEntries, maximumEntryLength, allowBlankEntries, () -> true);
    }

    public StringListSetting(
            String id,
            String name,
            String description,
            List<String> defaultValue,
            int maximumEntries,
            int maximumEntryLength,
            boolean allowBlankEntries,
            BooleanSupplier visibilityPredicate
    ) {
        super(id, name, description, immutable(defaultValue), visibilityPredicate);
        if (maximumEntries < 0 || maximumEntryLength < 1) {
            throw new IllegalArgumentException("String-list limits must be non-negative entries and positive entry length");
        }
        this.maximumEntries = maximumEntries;
        this.maximumEntryLength = maximumEntryLength;
        this.allowBlankEntries = allowBlankEntries;
        if (!isValid(defaultValue())) {
            throw new IllegalArgumentException("Default string list is outside the configured constraints");
        }
    }

    public int maximumEntries() {
        return maximumEntries;
    }

    public int maximumEntryLength() {
        return maximumEntryLength;
    }

    public boolean allowBlankEntries() {
        return allowBlankEntries;
    }

    @Override
    protected boolean isValid(List<String> value) {
        return value != null
                && value.size() <= maximumEntries
                && value.stream().allMatch(entry -> entry != null && entry.length() <= maximumEntryLength
                && (allowBlankEntries || !entry.isBlank()));
    }

    @Override
    protected List<String> normalize(List<String> value) {
        return immutable(value);
    }

    @Override
    protected JsonElement serialize(List<String> value) {
        JsonArray array = new JsonArray();
        value.forEach(array::add);
        return array;
    }

    @Override
    protected List<String> deserialize(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("Expected a text array");
        }
        List<String> entries = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Expected text array entries");
            }
            entries.add(entry.getAsString());
        }
        return immutable(entries);
    }

    private static List<String> immutable(List<String> value) {
        return List.copyOf(Objects.requireNonNull(value, "value"));
    }
}
