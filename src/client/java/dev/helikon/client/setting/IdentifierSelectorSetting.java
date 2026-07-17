package dev.helikon.client.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Shared, Minecraft-free validation for bounded block/item/entity identifier selections. */
abstract class IdentifierSelectorSetting extends Setting<List<String>> {
    private static final String IDENTIFIER_PATTERN = "(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+";

    private final int maximumEntries;

    IdentifierSelectorSetting(
            String id,
            String name,
            String description,
            List<String> defaultValue,
            int maximumEntries,
            BooleanSupplier visibilityPredicate
    ) {
        super(id, name, description, normalizeIdentifiers(defaultValue), visibilityPredicate);
        if (maximumEntries < 0) {
            throw new IllegalArgumentException("maximumEntries must not be negative");
        }
        this.maximumEntries = maximumEntries;
        if (!isValid(defaultValue())) {
            throw new IllegalArgumentException("Default identifiers are outside the configured constraints");
        }
    }

    public final int maximumEntries() {
        return maximumEntries;
    }

    @Override
    protected final boolean isValid(List<String> value) {
        return value != null
                && value.size() <= maximumEntries
                && value.stream().allMatch(IdentifierSelectorSetting::isIdentifier)
                && new LinkedHashSet<>(value).size() == value.size();
    }

    @Override
    protected final List<String> normalize(List<String> value) {
        return normalizeIdentifiers(value);
    }

    @Override
    protected final JsonElement serialize(List<String> value) {
        JsonArray array = new JsonArray();
        value.forEach(array::add);
        return array;
    }

    @Override
    protected final List<String> deserialize(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("Expected an identifier array");
        }
        List<String> identifiers = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Expected identifier array entries");
            }
            identifiers.add(entry.getAsString());
        }
        return normalizeIdentifiers(identifiers);
    }

    private static List<String> normalizeIdentifiers(List<String> identifiers) {
        Objects.requireNonNull(identifiers, "identifiers");
        return identifiers.stream()
                .map(identifier -> Objects.requireNonNull(identifier, "identifier").toLowerCase(Locale.ROOT))
                .toList();
    }

    private static boolean isIdentifier(String value) {
        return value != null && value.matches(IDENTIFIER_PATTERN);
    }
}
