package dev.helikon.client.input;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Polls module keybinds once per client tick and applies their activation
 * mode through the registry's failure-isolated path. The key source is
 * injected so the edge/hold logic is unit-testable without Minecraft.
 */
public final class KeybindManager {
    /** Reads whether a raw key code is currently held down. */
    @FunctionalInterface
    public interface KeyStateReader {
        boolean isDown(int keyCode);
    }

    private final ModuleRegistry modules;
    private final Map<String, Boolean> previouslyDown = new HashMap<>();

    public KeybindManager(ModuleRegistry modules) {
        this.modules = Objects.requireNonNull(modules, "modules");
    }

    /**
     * Processes all bound module keys. While {@code inputSuppressed} is true
     * (any screen is open, so the user may be typing into a text field) no
     * keybind action fires and HOLD modules disable. Physical key state keeps
     * being tracked so a key held across the end of suppression is not
     * treated as a fresh press.
     */
    public void tick(KeyStateReader keys, boolean inputSuppressed) {
        Objects.requireNonNull(keys, "keys");
        for (Module module : modules.all()) {
            Keybind keybind = module.keybind();
            if (!keybind.isBound()) {
                previouslyDown.remove(module.id());
                continue;
            }

            boolean down = keys.isDown(keybind.keyCode());
            boolean wasDown = Boolean.TRUE.equals(previouslyDown.put(module.id(), down));
            boolean pressed = down && !wasDown;

            if (module instanceof KeybindInputConsumer consumer && consumer.consumesKeybindInput()) {
                continue;
            }

            if (inputSuppressed) {
                if (keybind.activation() == Keybind.Activation.HOLD && module.isEnabled()) {
                    modules.setEnabled(module, false);
                }
                continue;
            }

            switch (keybind.activation()) {
                case TOGGLE -> {
                    if (pressed) {
                        modules.toggle(module);
                    }
                }
                case HOLD -> {
                    if (down && !module.isEnabled()) {
                        modules.setEnabled(module, true);
                    } else if (!down && wasDown && module.isEnabled()) {
                        modules.setEnabled(module, false);
                    }
                }
                case PRESS_ONCE -> {
                    if (pressed && !module.isEnabled()) {
                        modules.setEnabled(module, true);
                    }
                }
            }
        }
    }
}
