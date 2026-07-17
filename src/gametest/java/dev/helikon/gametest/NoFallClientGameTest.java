package dev.helikon.gametest;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Verifies NoFall prevents damage during an ordinary survival drop. */
public final class NoFallClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        ModuleRegistry modules = HelikonClient.activeModuleRegistry();
        if (modules == null) {
            throw new AssertionError("Helikon module registry was not initialized");
        }
        Module noFall = modules.find("no_fall").orElseThrow(() -> new AssertionError("no_fall module is missing"));
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            singleplayer.getServer().runCommand("gamemode survival @a");
            context.runOnClient(client -> {
                if (!modules.setEnabled(noFall, true)) {
                    throw new AssertionError("no_fall could not be enabled");
                }
            });
            singleplayer.getServer().runCommand("tp @a 0 -30 0");
            context.waitTicks(80);
            context.runOnClient(client -> {
                if (client.player == null) {
                    throw new AssertionError("local player disappeared during NoFall test");
                }
                if (!client.player.onGround()) {
                    throw new AssertionError("local player did not finish the NoFall test drop");
                }
                if (client.player.getHealth() < client.player.getMaxHealth()) {
                    throw new AssertionError("NoFall allowed damage: " + client.player.getHealth()
                            + "/" + client.player.getMaxHealth());
                }
                modules.setEnabled(noFall, false);
            });
        }
    }
}
