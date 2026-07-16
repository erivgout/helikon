package dev.helikon.client.event;

/** A handle for unregistering an event listener. */
@FunctionalInterface
public interface Subscription {
    void unsubscribe();
}
