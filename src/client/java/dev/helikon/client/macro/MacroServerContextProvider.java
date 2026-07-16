package dev.helikon.client.macro;

import java.util.Optional;

/** Minecraft-free boundary for the currently connected multiplayer address. */
@FunctionalInterface
public interface MacroServerContextProvider {
    Optional<String> currentServerAddress();
}
