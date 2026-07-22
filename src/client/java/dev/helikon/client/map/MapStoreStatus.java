package dev.helikon.client.map;

import java.util.Objects;

/** Non-sensitive status exposed by persistent map storage. */
public record MapStoreStatus(State state, String detail) {
    public MapStoreStatus {
        state = Objects.requireNonNull(state, "state");
        detail = Objects.requireNonNullElse(detail, "").trim();
    }

    public static MapStoreStatus ready() {
        return new MapStoreStatus(State.READY, "Map recording ready");
    }

    public enum State {
        READY,
        LOADING,
        FLUSHING,
        QUOTA_REACHED,
        UNSUPPORTED_VERSION,
        IO_ERROR,
        CLOSED
    }
}

