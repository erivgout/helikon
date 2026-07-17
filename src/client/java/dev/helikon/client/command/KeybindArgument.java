package dev.helikon.client.command;

import dev.helikon.client.input.Keybind;
import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/** Parses a local command bind token such as {@code ctrl+r} or {@code alt+mouse1}. */
public record KeybindArgument(Keybind.InputType inputType, int code, Set<Keybind.Modifier> modifiers) {
    public KeybindArgument {
        modifiers = modifiers == null || modifiers.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(modifiers));
        if (!Keybind.isValidCode(inputType, code) || code == Keybind.UNBOUND_KEY) {
            throw new IllegalArgumentException("Invalid bind input");
        }
    }

    public static Optional<KeybindArgument> parse(String raw, KeyNameResolver keyNames) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String[] parts = raw.toLowerCase(Locale.ROOT).split("\\+", -1);
        if (parts.length == 0 || parts.length > Keybind.Modifier.values().length + 1) {
            return Optional.empty();
        }
        EnumSet<Keybind.Modifier> modifiers = EnumSet.noneOf(Keybind.Modifier.class);
        for (int index = 0; index < parts.length - 1; index++) {
            Optional<Keybind.Modifier> modifier = modifier(parts[index]);
            if (modifier.isEmpty() || !modifiers.add(modifier.get())) {
                return Optional.empty();
            }
        }
        String primary = parts[parts.length - 1];
        OptionalInt mouseButton = mouseButton(primary);
        if (mouseButton.isPresent()) {
            return Optional.of(new KeybindArgument(Keybind.InputType.MOUSE_BUTTON, mouseButton.getAsInt(), modifiers));
        }
        OptionalInt keyCode = keyNames.resolve(primary);
        return keyCode.isPresent()
                ? Optional.of(new KeybindArgument(Keybind.InputType.KEYBOARD, keyCode.getAsInt(), modifiers))
                : Optional.empty();
    }

    private static Optional<Keybind.Modifier> modifier(String token) {
        return switch (token) {
            case "shift" -> Optional.of(Keybind.Modifier.SHIFT);
            case "ctrl", "control" -> Optional.of(Keybind.Modifier.CONTROL);
            case "alt" -> Optional.of(Keybind.Modifier.ALT);
            case "super", "win", "cmd", "command" -> Optional.of(Keybind.Modifier.SUPER);
            default -> Optional.empty();
        };
    }

    private static OptionalInt mouseButton(String token) {
        if (!token.startsWith("mouse")) {
            return OptionalInt.empty();
        }
        try {
            int userNumber = Integer.parseInt(token.substring("mouse".length()));
            int button = GLFW.GLFW_MOUSE_BUTTON_1 + userNumber - 1;
            return userNumber >= 1 && Keybind.isValidMouseButton(button)
                    ? OptionalInt.of(button)
                    : OptionalInt.empty();
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }
}
