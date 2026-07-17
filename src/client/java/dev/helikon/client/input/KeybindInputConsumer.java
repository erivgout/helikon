package dev.helikon.client.input;

/**
 * A module that reserves its configured key as an input value instead of a
 * normal enable/disable trigger for its current mode.
 */
public interface KeybindInputConsumer {
    boolean consumesKeybindInput();
}
