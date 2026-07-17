package dev.helikon.gametest;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Reproduces the live XRay report: buried target blocks must render all faces
 * while XRay hides their neighbors, and disabling XRay must restore normal
 * chunk geometry. Screenshots capture each stage for visual verification.
 */
public final class XRayClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        ModuleRegistry modules = HelikonClient.activeModuleRegistry();
        if (modules == null) {
            throw new AssertionError("Helikon module registry was not initialized");
        }
        Module xray = modules.find("xray").orElseThrow(() -> new AssertionError("xray module is missing"));
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();

            // A buried cluster (culling regression case) and a floating control cluster.
            singleplayer.getServer().runCommand("fill 6 -63 -2 10 -62 2 minecraft:diamond_ore");
            singleplayer.getServer().runCommand("fill 6 -56 -2 8 -54 0 minecraft:diamond_ore");
            singleplayer.getServer().runCommand("tp @a -6 -60 0 -90 10");
            context.waitTicks(20);
            singleplayer.getClientLevel().waitForChunksRender();
            context.takeScreenshot("xray-0-before");

            context.runOnClient(client -> modules.setEnabled(xray, true));
            context.waitTicks(60);
            singleplayer.getClientLevel().waitForChunksRender();
            context.takeScreenshot("xray-1-enabled");
            context.runOnClient(client -> modules.setEnabled(xray, false));
            context.waitTicks(60);
            singleplayer.getClientLevel().waitForChunksRender();
            context.takeScreenshot("xray-2-disabled");
        }
    }
}
