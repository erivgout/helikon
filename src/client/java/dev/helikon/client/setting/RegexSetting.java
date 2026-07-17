package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** A bounded, conservatively validated regular-expression setting. */
public final class RegexSetting extends Setting<String> {
    private final int maximumLength;
    private final boolean allowBlank;

    public RegexSetting(String id, String name, String description, String defaultValue, int maximumLength, boolean allowBlank) {
        this(id, name, description, defaultValue, maximumLength, allowBlank, () -> true);
    }

    public RegexSetting(
            String id,
            String name,
            String description,
            String defaultValue,
            int maximumLength,
            boolean allowBlank,
            BooleanSupplier visibilityPredicate
    ) {
        super(id, name, description, Objects.requireNonNull(defaultValue, "defaultValue"), visibilityPredicate);
        if (maximumLength < 1) {
            throw new IllegalArgumentException("maximumLength must be positive");
        }
        this.maximumLength = maximumLength;
        this.allowBlank = allowBlank;
        if (!isValid(defaultValue())) {
            throw new IllegalArgumentException("Default regular expression is invalid");
        }
    }

    public int maximumLength() {
        return maximumLength;
    }

    public boolean allowBlank() {
        return allowBlank;
    }

    @Override
    protected boolean isValid(String value) {
        if (value == null || value.length() > maximumLength || (!allowBlank && value.isBlank())) {
            return false;
        }
        if (value.isBlank()) {
            return true;
        }
        if (value.matches(".*\\\\[1-9].*") || value.contains("\\k<") || value.contains("(?=") || value.contains("(?!")
                || value.contains("(?<=") || value.contains("(?<!")
                || value.matches(".*\\([^)]*\\)[+*{].*")) {
            return false;
        }
        try {
            Pattern.compile(value);
            return true;
        } catch (PatternSyntaxException exception) {
            return false;
        }
    }

    @Override
    protected JsonElement serialize(String value) {
        return new JsonPrimitive(value);
    }

    @Override
    protected String deserialize(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected a regular-expression string");
        }
        return element.getAsString();
    }
}
