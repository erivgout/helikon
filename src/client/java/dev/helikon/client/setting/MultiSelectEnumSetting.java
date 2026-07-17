package dev.helikon.client.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** A finite, immutable multi-selection of enum values persisted as stable tokens. */
public final class MultiSelectEnumSetting<E extends Enum<E>> extends Setting<Set<E>> {
    private final Class<E> enumType;
    private final Set<E> allowedValues;

    public MultiSelectEnumSetting(
            String id,
            String name,
            String description,
            Class<E> enumType,
            Set<E> defaultValue
    ) {
        this(id, name, description, enumType, defaultValue, () -> true);
    }

    public MultiSelectEnumSetting(
            String id,
            String name,
            String description,
            Class<E> enumType,
            Set<E> defaultValue,
            BooleanSupplier visibilityPredicate
    ) {
        super(id, name, description, immutable(enumType, defaultValue), visibilityPredicate);
        this.enumType = Objects.requireNonNull(enumType, "enumType");
        E[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0) {
            throw new IllegalArgumentException("enumType must declare at least one value");
        }
        this.allowedValues = Set.copyOf(Arrays.asList(constants));
        if (!isValid(defaultValue())) {
            throw new IllegalArgumentException("Default enum selection contains an unsupported value");
        }
    }

    public Set<E> allowedValues() {
        return allowedValues;
    }

    public boolean contains(E value) {
        return value().contains(Objects.requireNonNull(value, "value"));
    }

    @Override
    protected boolean isValid(Set<E> value) {
        return value != null && allowedValues.containsAll(value);
    }

    @Override
    protected Set<E> normalize(Set<E> value) {
        return immutable(enumType, value);
    }

    @Override
    protected JsonElement serialize(Set<E> value) {
        JsonArray array = new JsonArray();
        value.stream().sorted().forEach(option -> array.add(option.name().toLowerCase(Locale.ROOT)));
        return array;
    }

    @Override
    protected Set<E> deserialize(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("Expected an enum token array");
        }
        Set<E> values = EnumSet.noneOf(enumType);
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Expected enum token array entries");
            }
            String token = entry.getAsString();
            E match = Arrays.stream(enumType.getEnumConstants())
                    .filter(value -> value.name().equalsIgnoreCase(token))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown enum token: " + token));
            if (!values.add(match)) {
                throw new IllegalArgumentException("Duplicate enum token: " + token);
            }
        }
        return immutable(enumType, values);
    }

    private static <E extends Enum<E>> Set<E> immutable(Class<E> enumType, Set<E> value) {
        Objects.requireNonNull(enumType, "enumType");
        Objects.requireNonNull(value, "value");
        Set<E> copy = EnumSet.noneOf(enumType);
        copy.addAll(value);
        return Set.copyOf(copy);
    }
}
