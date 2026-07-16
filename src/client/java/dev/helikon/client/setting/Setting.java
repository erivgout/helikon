package dev.helikon.client.setting;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A validated, serializable module setting. Concrete setting types own their
 * value parsing so configuration code does not need type-specific branches.
 */
public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final String description;
    private final T defaultValue;
    private final List<Consumer<T>> changeListeners = new ArrayList<>();

    private T value;

    protected Setting(String id, String name, String description, T defaultValue) {
        this.id = requireIdentifier(id, "id");
        this.name = requireText(name, "name");
        this.description = requireText(description, "description");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");

        this.value = defaultValue;
    }

    public final String id() {
        return id;
    }

    public final String name() {
        return name;
    }

    public final String description() {
        return description;
    }

    public final T defaultValue() {
        return defaultValue;
    }

    public final T value() {
        return value;
    }

    public final void set(T value) {
        Objects.requireNonNull(value, "value");

        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid value for setting '" + id + "': " + value);
        }

        this.value = value;
        notifyListeners(value);
    }

    public final void reset() {
        set(defaultValue);
    }

    public final void addChangeListener(Consumer<T> listener) {
        changeListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public final JsonElement toJson() {
        return serialize(value);
    }

    /**
     * Applies a JSON value if it is valid. Invalid or malformed values reset to
     * the setting default and return {@code false}.
     */
    public final boolean applyJson(JsonElement element) {
        try {
            set(deserialize(element));
            return true;
        } catch (RuntimeException exception) {
            reset();
            return false;
        }
    }

    protected abstract boolean isValid(T value);

    protected abstract JsonElement serialize(T value);

    protected abstract T deserialize(JsonElement element);

    private void notifyListeners(T newValue) {
        for (Consumer<T> listener : List.copyOf(changeListeners)) {
            listener.accept(newValue);
        }
    }

    private static String requireIdentifier(String value, String field) {
        String identifier = requireText(value, field);
        if (!identifier.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException(field + " must use lowercase letters, digits, and underscores: " + value);
        }
        return identifier;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
