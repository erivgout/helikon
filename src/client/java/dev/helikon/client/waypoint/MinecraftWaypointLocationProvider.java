package dev.helikon.client.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/** Thin 26.2 client adapter for deriving a local waypoint context and position. */
public final class MinecraftWaypointLocationProvider implements WaypointLocationProvider {
    @Override
    public Optional<WaypointLocation> currentLocation() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return Optional.empty();
        }

        return resolveContext(client).map(context -> {
            var position = client.player.blockPosition();
            return new WaypointLocation(position.getX(), position.getY(), position.getZ(), context);
        });
    }

    private static Optional<WaypointContext> resolveContext(Minecraft client) {
        if (client.isMultiplayerServer()) {
            ServerData server = client.getCurrentServer();
            if (server == null || server.ip == null || server.ip.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new WaypointContext("server:" + server.ip.trim().toLowerCase(Locale.ROOT),
                    client.level.dimension().identifier().toString()));
        }
        if (!client.isLocalServer() || !client.hasSingleplayerServer()) {
            return Optional.empty();
        }

        Path directory = client.getSingleplayerServer().getServerDirectory();
        Path worldName = directory.getFileName();
        if (worldName == null || worldName.toString().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new WaypointContext("world:" + worldName,
                client.level.dimension().identifier().toString()));
    }
}
