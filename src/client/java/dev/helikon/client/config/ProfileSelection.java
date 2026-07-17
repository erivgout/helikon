package dev.helikon.client.config;

import java.util.Objects;
import java.util.Optional;

/** Minecraft-free precedence rule for automatic local profile selection. */
public final class ProfileSelection {
    private ProfileSelection() {
    }

    /** Chooses the one optional profile applied during client startup. */
    public static Optional<String> atStartup(Optional<String> defaultProfile) {
        Objects.requireNonNull(defaultProfile, "defaultProfile");
        return defaultProfile;
    }

    /**
     * Chooses a profile for a newly joined world. An absent association deliberately means no
     * application: the startup default must not overwrite session changes on every join.
     */
    public static Optional<String> atConnection(Optional<String> scopedProfile) {
        Objects.requireNonNull(scopedProfile, "scopedProfile");
        return scopedProfile;
    }
}
