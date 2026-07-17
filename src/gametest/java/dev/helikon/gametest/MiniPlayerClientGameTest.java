package dev.helikon.gametest;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Captures the standalone Mini Player model for transparent-background visual verification. */
public final class MiniPlayerClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        ModuleRegistry modules = HelikonClient.activeModuleRegistry();
        if (modules == null) {
            throw new AssertionError("Helikon module registry was not initialized");
        }
        Module miniPlayer = modules.find("mini_player").orElseThrow(
                () -> new AssertionError("mini_player module is missing"));
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            context.runOnClient(client -> {
                modules.disableAll();
                if (!modules.setEnabled(miniPlayer, true)) {
                    throw new AssertionError("mini_player could not be enabled");
                }
            });
            context.waitTicks(20);
            context.takeScreenshot("mini-player-transparent");
            context.runOnClient(client -> modules.setEnabled(miniPlayer, false));
        }
    }
}
