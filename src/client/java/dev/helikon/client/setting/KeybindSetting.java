package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.helikon.client.input.Keybind;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/** A validated local keyboard bind value for settings that need their own input. */
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
        return value != null && Keybind.isValidKeyCode(value.keyCode());
    }

    @Override
    protected JsonElement serialize(Keybind value) {
        JsonObject object = new JsonObject();
        object.addProperty("key", value.keyCode());
        object.addProperty("activation", value.activation().name().toLowerCase(Locale.ROOT));
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
            return new Keybind((int) rawKey,
                    Keybind.Activation.valueOf(object.get("activation").getAsString().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid keybind object", exception);
        }
    }
}
