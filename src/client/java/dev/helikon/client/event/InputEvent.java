package dev.helikon.client.event;

import java.util.Objects;

/** Normalized local key, mouse-button, or scroll input without a platform event object. */
public record InputEvent(Kind kind, Action action, int code, int scanCode, int modifiers,
                         double scrollX, double scrollY) implements ClientEvent {
    public enum Kind {
        KEY,
        MOUSE_BUTTON,
        MOUSE_SCROLL
    }

    public enum Action {
        PRESS,
        RELEASE,
        REPEAT,
        SCROLL
    }

    public InputEvent {
        kind = Objects.requireNonNull(kind, "kind");
        action = Objects.requireNonNull(action, "action");
        if (!Double.isFinite(scrollX) || !Double.isFinite(scrollY)) {
            throw new IllegalArgumentException("Scroll values must be finite");
        }
        if (kind == Kind.MOUSE_SCROLL && action != Action.SCROLL) {
            throw new IllegalArgumentException("Mouse scroll input must use the scroll action");
        }
        if (kind != Kind.MOUSE_SCROLL && action == Action.SCROLL) {
            throw new IllegalArgumentException("Only mouse scroll input may use the scroll action");
        }
    }
}
