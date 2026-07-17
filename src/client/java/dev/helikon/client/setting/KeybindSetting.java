package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.helikon.client.input.Keybind;

import java.util.Locale;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** A validated local keyboard or mouse bind value for settings that need their own input. */
public final class KeybindSetting extends Setting<Keybind> {
    public KeybindSetting(String id, String name, String description, Keybind defaultValue) {
        this(id, name, description, defaultValue, () -> true);
    }

    public KeybindSetting(
            String id,
            String name,
            String description,
            Keybind defaultValue,
            BooleanSupplier visibilityPredicate
    ) {
        super(id, name, description, defaultValue, visibilityPredicate);
    }

    @Override
    protected boolean isValid(Keybind value) {
        return value != null && Keybind.isValidCode(value.inputType(), value.keyCode());
    }

    @Override
    protected JsonElement serialize(Keybind value) {
        JsonObject object = new JsonObject();
        object.addProperty("inputType", value.inputType().name().toLowerCase(Locale.ROOT));
        object.addProperty("key", value.keyCode());
        object.addProperty("activation", value.activation().name().toLowerCase(Locale.ROOT));
        com.google.gson.JsonArray modifiers = new com.google.gson.JsonArray();
        value.modifiers().stream().sorted().forEach(modifier ->
                modifiers.add(modifier.name().toLowerCase(Locale.ROOT)));
        object.add("modifiers", modifiers);
        return object;
    }

    @Override
    protected Keybind deserialize(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("Expected a keybind object");
        }
        JsonObject object = element.getAsJsonObject();
        if (!object.has("key") || !object.has("activation") || !object.get("key").isJsonPrimitive()
                || !object.get("key").getAsJsonPrimitive().isNumber() || !object.get("activation").isJsonPrimitive()
                || !object.get("activation").getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Invalid keybind object");
        }
        double rawKey = object.get("key").getAsDouble();
        if (!Double.isFinite(rawKey) || rawKey < Integer.MIN_VALUE || rawKey > Integer.MAX_VALUE
                || Math.rint(rawKey) != rawKey) {
            throw new IllegalArgumentException("Invalid key code");
        }
        try {
            Keybind.InputType inputType = object.has("inputType")
                    ? Keybind.InputType.valueOf(object.get("inputType").getAsString().toUpperCase(Locale.ROOT))
                    : Keybind.InputType.KEYBOARD;
            Set<Keybind.Modifier> modifiers = parseModifiers(object.get("modifiers"));
            return new Keybind(inputType, (int) rawKey, modifiers,
                    Keybind.Activation.valueOf(object.get("activation").getAsString().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid keybind object", exception);
        }
    }

    private static Set<Keybind.Modifier> parseModifiers(JsonElement element) {
        if (element == null) {
            return Set.of();
        }
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("Invalid keybind modifiers");
        }
        EnumSet<Keybind.Modifier> modifiers = EnumSet.noneOf(Keybind.Modifier.class);
        for (JsonElement modifier : element.getAsJsonArray()) {
            if (!modifier.isJsonPrimitive() || !modifier.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Invalid keybind modifier");
            }
            modifiers.add(Keybind.Modifier.valueOf(modifier.getAsString().toUpperCase(Locale.ROOT)));
        }
        return Set.copyOf(modifiers);
    }
}
