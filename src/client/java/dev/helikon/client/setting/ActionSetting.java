package dev.helikon.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Objects;

/**
 * A ClickGUI action presented beside ordinary persisted settings.
 *
 * Its stored value is always false; invoking the action changes runtime state
 * through the supplied callback rather than representing configuration.
 */
public final class ActionSetting extends Setting<Boolean> {
    private final Runnable action;

    public ActionSetting(String id, String name, String description, Runnable action) {
        super(id, name, description, false);
        this.action = Objects.requireNonNull(action, "action");
    }

    public void run() {
        action.run();
    }

    @Override
    protected boolean isValid(Boolean value) {
        return value != null && !value;
    }

    @Override
    protected JsonElement serialize(Boolean value) {
        return new JsonPrimitive(false);
    }

    @Override
    protected Boolean deserialize(JsonElement element) {
        return false;
    }
}
