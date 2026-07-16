package dev.helikon.client.command;

import java.util.OptionalInt;

/**
 * Resolves a user-typed key name (for example {@code r}, {@code f6}, or
 * {@code right.shift}) to a key code. Kept as an interface so the command
 * layer stays independent of Minecraft input classes and unit-testable.
 */
@FunctionalInterface
public interface KeyNameResolver {
    OptionalInt resolve(String keyName);
}
