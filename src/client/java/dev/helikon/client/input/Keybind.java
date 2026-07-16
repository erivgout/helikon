package dev.helikon.client.input;

import java.util.Objects;

/**
 * A serializable description of a module keybind. Input polling is deliberately
 * kept outside this value type so modules remain independent of Minecraft APIs.
 */
public record Keybind(int keyCode, Activation activation) {
    public static final int UNBOUND_KEY = -1;

    public Keybind {
        Objects.requireNonNull(activation, "activation");
    }

    public static Keybind unbound() {
        return new Keybind(UNBOUND_KEY, Activation.TOGGLE);
    }

    public boolean isBound() {
        return keyCode != UNBOUND_KEY;
    }

    public enum Activation {
        TOGGLE,
        HOLD,
        PRESS_ONCE
    }
}
