package dev.helikon.client.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Optional;

/** Thin 26.2 adapter for server-scoped macro matching. */
public final class MinecraftMacroServerContextProvider implements MacroServerContextProvider {
    @Override
    public Optional<String> currentServerAddress() {
        Minecraft client = Minecraft.getInstance();
        if (!client.isMultiplayerServer()) {
            return Optional.empty();
        }
        ServerData server = client.getCurrentServer();
        if (server == null || server.ip == null || server.ip.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Macro.normalizeServerAddress(server.ip));
    }
}
