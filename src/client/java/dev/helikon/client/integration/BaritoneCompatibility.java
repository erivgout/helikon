package dev.helikon.client.integration;

import java.util.Objects;
import java.util.function.Predicate;

/** Confirms that Fabric Loader discovered Helikon's embedded Baritone mod. */
public final class BaritoneCompatibility {
    public static final String MOD_ID = "baritone";

    public record Status(boolean detected, String detail) {
        public Status {
            if (detail == null || detail.isBlank()) {
                throw new IllegalArgumentException("detail must not be blank");
            }
        }
    }

    private BaritoneCompatibility() {
    }

    public static Status detect(Predicate<String> installedMod) {
        boolean detected = Objects.requireNonNull(installedMod, "installedMod").test(MOD_ID);
        return new Status(detected, detected
                ? "Embedded Baritone loaded; Helikon pathfinding integration is available."
                : "Embedded Baritone was not discovered; pathfinding controls are unavailable.");
    }
}
