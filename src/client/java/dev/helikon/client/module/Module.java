package dev.helikon.client.module;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.setting.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base type for a local, client-only feature. The registry owns failure
 * isolation; modules should keep their callbacks focused and reversible.
 */
public abstract class Module {
    private final String id;
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final boolean defaultEnabled;
    private final List<Setting<?>> settings = new ArrayList<>();

    private Keybind keybind;
    private boolean enabled;

    protected Module(
            String id,
            String name,
            String description,
            ModuleCategory category,
            boolean defaultEnabled,
            Keybind keybind
    ) {
        this.id = requireId(id);
        this.name = requireText(name, "name");
        this.description = requireText(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.defaultEnabled = defaultEnabled;
        this.keybind = Objects.requireNonNullElse(keybind, Keybind.unbound());
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

    public final ModuleCategory category() {
        return category;
    }

    public final boolean defaultEnabled() {
        return defaultEnabled;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final Keybind keybind() {
        return keybind;
    }

    public final void setKeybind(Keybind keybind) {
        this.keybind = Objects.requireNonNullElse(keybind, Keybind.unbound());
    }

    public final List<Setting<?>> settings() {
        return List.copyOf(settings);
    }

    public final void enable() {
        if (enabled) {
            return;
        }

        enabled = true;
        onEnable();
    }

    public final void disable() {
        if (!enabled) {
            return;
        }

        try {
            onDisable();
        } finally {
            enabled = false;
        }
    }

    public final void toggle() {
        if (enabled) {
            disable();
        } else {
            enable();
        }
    }

    public final void resetSettings() {
        settings.forEach(Setting::reset);
    }

    protected final <T extends Setting<?>> T addSetting(T setting) {
        T nonNullSetting = Objects.requireNonNull(setting, "setting");
        boolean duplicate = settings.stream().anyMatch(existing -> existing.id().equals(nonNullSetting.id()));
        if (duplicate) {
            throw new IllegalArgumentException("Duplicate setting id '" + nonNullSetting.id() + "' in module '" + id + "'");
        }
        settings.add(nonNullSetting);
        return nonNullSetting;
    }

    protected void onEnable() {
        // Implemented by individual modules.
    }

    protected void onDisable() {
        // Implemented by individual modules.
    }

    private static String requireId(String id) {
        String checkedId = requireText(id, "id");
        if (!checkedId.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Module id must use lowercase letters, digits, and underscores: " + id);
        }
        return checkedId;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
