package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

import java.util.Objects;
import java.util.Optional;

/** Retains and displays one session-local coordinate snapshot when the local player dies. */
public final class DeathCoordinates extends Module {
    public DeathCoordinates() {
        super("death_coordinates", "Death Coordinates", "Records the last local position when the player dies.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
    }

    public Optional<CoordinateEntry> record(CoordinateTracker tracker) {
        return isEnabled() ? Objects.requireNonNull(tracker, "tracker").record(CoordinateKind.DEATH) : Optional.empty();
    }
}
