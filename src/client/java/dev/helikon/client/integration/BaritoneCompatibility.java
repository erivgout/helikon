package dev.helikon.client.integration;

import java.util.Objects;
import java.util.function.Predicate;

/** Detects a user-installed Baritone without loading, bundling, or invoking its classes. */
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
                ? "User-installed Baritone detected; Helikon keeps its integration optional and inactive."
                : "Baritone is not installed; Helikon needs no optional integration.");
    }
}
