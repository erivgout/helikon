package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

import java.util.Objects;
import java.util.Optional;

/** Retains and displays one session-local coordinate snapshot when the client disconnects. */
public final class LogoutCoordinates extends Module {
    public LogoutCoordinates() {
        super("logout_coordinates", "Logout Coordinates", "Records the last local position when disconnecting.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
    }

    public Optional<CoordinateEntry> record(CoordinateTracker tracker) {
        return isEnabled() ? Objects.requireNonNull(tracker, "tracker").record(CoordinateKind.LOGOUT) : Optional.empty();
    }
}
