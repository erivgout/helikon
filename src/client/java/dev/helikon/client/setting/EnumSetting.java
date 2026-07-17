package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** A closed set of named enum values, persisted as stable lowercase tokens. */
public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final List<E> values;

    public EnumSetting(
            String id,
            String name,
            String description,
            Class<E> enumType,
            E defaultValue
    ) {
        super(id, name, description, requireDefault(enumType, defaultValue));
        E[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0) {
            throw new IllegalArgumentException("enumType must declare at least one value");
        }
        values = List.copyOf(Arrays.asList(constants));
    }

    public List<E> values() {
        return values;
    }

    /** Stable lowercase token suitable for JSON, commands, and the ClickGUI. */
    public String valueId() {
        return value().name().toLowerCase(Locale.ROOT);
    }

    /** Finds an option case-insensitively without changing the current value. */
    public Optional<E> find(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return values.stream().filter(value -> value.name().equalsIgnoreCase(token.trim())).findFirst();
    }

    /** Applies a named option, returning false without mutation when it is unknown. */
    public boolean trySet(String token) {
        Optional<E> found = find(token);
        if (found.isEmpty()) {
            return false;
        }
        set(found.get());
        return true;
    }

    /** Cycles forward through the declared enum order. */
    public void cycle() {
        int currentIndex = values.indexOf(value());
        set(values.get((currentIndex + 1) % values.size()));
    }

    @Override
    protected boolean isValid(E value) {
        return value != null && values.contains(value);
    }

    @Override
    protected JsonElement serialize(E value) {
        return new JsonPrimitive(value.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected E deserialize(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected an enum token");
        }
        return find(element.getAsString()).orElseThrow(() -> new IllegalArgumentException("Unknown enum token"));
    }

    private static <E extends Enum<E>> E requireDefault(Class<E> enumType, E defaultValue) {
        Objects.requireNonNull(enumType, "enumType");
        return Objects.requireNonNull(defaultValue, "defaultValue");
    }
}
