package dev.helikon.gametest;

import baritone.api.BaritoneAPI;
import baritone.api.cache.IWaypoint;
import baritone.api.utils.BetterBlockPos;
import dev.helikon.client.HelikonClient;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.render.EntityEspMode;
import dev.helikon.client.module.world.BaritoneNavigation;
import dev.helikon.client.module.world.SeedCracker;
import dev.helikon.client.render.MinecraftBaritoneVisualizationRenderer;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-engine smoke test: every non-network module must survive real client
 * tick/render traffic without tripping the failure handler, and EntityESP must
 * cycle all four modes cleanly. A module that throws anywhere in its guarded
 * paths is auto-disabled by ModuleRegistry, which this test detects by name.
 */
public final class HelikonClientGameTest implements FabricClientGameTest {
    private static final int SOAK_TICKS = 100;

    @Override
    public void runTest(ClientGameTestContext context) {
        ModuleRegistry modules = HelikonClient.activeModuleRegistry();
        if (modules == null) {
            throw new AssertionError("Helikon module registry was not initialized");
        }
        List<String> failures = new CopyOnWriteArrayList<>();
        modules.addFailureHandler((module, operation, exception) ->
                failures.add(module.id() + " (" + operation + ": " + exception.getClass().getSimpleName() + ")"));
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            context.takeScreenshot("helikon-world-loaded");

            exerciseBaritone(context, modules);
            assertNoFailures(failures, "while exercising embedded Baritone");

            exerciseEmptyBaritoneHomeWaypoint(context, modules);
            assertNoFailures(failures, "while rendering an unnamed Baritone HOME waypoint");

            exerciseRadarMinimap(context, modules);
            assertNoFailures(failures, "while rendering the cached Radar minimap texture");

            exerciseSeedCracker(context, modules);
            assertNoFailures(failures, "while solving and rendering SeedCracker evidence");

            context.runOnClient(client -> modules.all().stream()
                    // External integrations use deterministic unit/fake-transport and explicit manual tests;
                    // the in-engine suite must never contact a live third-party service.
                    .filter(module -> !module.id().equals("update_checker"))
                    .forEach(module -> modules.setEnabled(module, true)));
            context.waitTicks(SOAK_TICKS);
            assertNoFailures(failures, "after enabling every non-network module");
            context.takeScreenshot("helikon-all-modules-enabled");

            cycleEntityEspModes(context, modules);
            assertNoFailures(failures, "after cycling EntityESP modes");

            context.runOnClient(client -> modules.disableAll());
            context.waitTicks(20);
            List<String> stillEnabled = new ArrayList<>();
            context.runOnClient(client -> modules.all().stream()
                    .filter(Module::isEnabled)
                    .forEach(module -> stillEnabled.add(module.id())));
            if (!stillEnabled.isEmpty()) {
                throw new AssertionError("Modules failed to disable cleanly: " + stillEnabled);
            }
        }
    }

    private static void exerciseRadarMinimap(ClientGameTestContext context, ModuleRegistry modules) {
        Module radar = modules.find("radar")
                .orElseThrow(() -> new AssertionError("radar module is missing"));
        BooleanSetting minimap = (BooleanSetting) radar.settings().stream()
                .filter(setting -> setting.id().equals("minimap"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("radar minimap setting is missing"));
        context.runOnClient(client -> {
            minimap.set(true);
            modules.setEnabled(radar, true);
            client.player.setYRot(client.player.getYRot() + 20.0F);
        });
        context.waitTicks(25);
        context.takeScreenshot("helikon-radar-texture-minimap");
        context.runOnClient(client -> {
            modules.setEnabled(radar, false);
            minimap.set(false);
        });
    }

    private static void exerciseBaritone(ClientGameTestContext context, ModuleRegistry modules) {
        BaritoneNavigation baritone = (BaritoneNavigation) modules.find("baritone")
                .orElseThrow(() -> new AssertionError("baritone module is missing"));
        context.runOnClient(client -> {
            modules.setEnabled(baritone, true);
            ((dev.helikon.client.setting.BooleanSetting) baritone.settings().stream()
                    .filter(setting -> setting.id().equals("render_through_walls"))
                    .findFirst()
                    .orElseThrow()).set(false);
            if (!baritone.execute("goto 48 -60 0")) {
                throw new AssertionError("embedded Baritone rejected goto");
            }
            // Keep the route at an angle to the camera so the screenshot
            // verifies visible geometry instead of collapsing a straight path
            // into a single point beneath the crosshair.
            client.player.setYRot(-70.0F);
            client.player.setYHeadRot(-70.0F);
            client.player.setXRot(24.0F);
        });
        context.waitTicks(20);
        if (baritone.status().endsWith("goal: none")) {
            throw new AssertionError("embedded Baritone did not retain a goto goal");
        }
        if (!baritone.status().startsWith("pathing")) {
            throw new AssertionError("embedded Baritone retained the goal but did not begin pathing: "
                    + baritone.status());
        }
        MinecraftBaritoneVisualizationRenderer.VisualizationState visualization =
                MinecraftBaritoneVisualizationRenderer.inspect();
        if (visualization.pathPositions() < 2 || visualization.goalMarkers() < 1
                || visualization.renderInvocations() < 1
                || visualization.lastSubmittedPathPositions() < 2
                || visualization.lastSubmittedGoalMarkers() < 1) {
            throw new AssertionError("embedded Baritone has no renderable path/goal state: " + visualization);
        }
        context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
        context.waitTicks(10);
        if (baritone.status().endsWith("goal: none")) {
            throw new AssertionError("embedded Baritone stopped when an inventory screen opened");
        }
        context.takeScreenshot("helikon-baritone-inventory-open");
        context.runOnClient(client -> {
            client.gui.setScreen(null);
            baritone.execute("pause");
            client.player.setYRot(-70.0F);
            client.player.setYHeadRot(-70.0F);
            client.player.setXRot(24.0F);
        });
        context.waitTicks(1);
        context.takeScreenshot("helikon-baritone-pathing-world");
        // Screenshot capture is queued onto a render frame. Keep the route
        // alive long enough for that frame instead of clearing its state in
        // the same test turn.
        context.waitTicks(2);
        context.runOnClient(client -> baritone.cancel());
        context.waitTicks(5);
        if (!baritone.status().startsWith("idle")) {
            throw new AssertionError("embedded Baritone did not cancel cleanly: " + baritone.status());
        }
        context.runOnClient(client -> modules.setEnabled(baritone, false));
    }

    private static void exerciseSeedCracker(ClientGameTestContext context, ModuleRegistry modules) {
        SeedCracker seedCracker = (SeedCracker) modules.find("seed_cracker")
                .orElseThrow(() -> new AssertionError("seed_cracker module is missing"));
        context.runOnClient(client -> {
            if (client.getSingleplayerServer() == null || client.player == null || client.level == null) {
                throw new AssertionError("SeedCracker GameTest requires an integrated world");
            }
            modules.setEnabled(seedCracker, true);
            long worldSeed = client.getSingleplayerServer().overworld().getSeed();
            seedCracker.revealLocalWorldSeed(worldSeed);
            if (!seedCracker.addManualObservation(
                    client.player.getBlockX() >> 4, client.player.getBlockZ() >> 4)) {
                throw new AssertionError("SeedCracker rejected fresh manual evidence");
            }
        });
        context.waitTicks(5);
        SeedCracker.Snapshot snapshot = seedCracker.snapshot();
        if (snapshot.state() != SeedCracker.State.SOLVED || snapshot.fullWorldSeed() == null) {
            throw new AssertionError("SeedCracker did not expose the integrated-world seed: " + snapshot);
        }
        if (snapshot.observations() != 1) {
            throw new AssertionError("SeedCracker did not retain its rendered evidence: " + snapshot);
        }
        context.takeScreenshot("helikon-seed-cracker-solved");
        context.runOnClient(client -> modules.setEnabled(seedCracker, false));
    }

    private static void exerciseEmptyBaritoneHomeWaypoint(ClientGameTestContext context, ModuleRegistry modules) {
        Module waypoints = modules.find("waypoints")
                .orElseThrow(() -> new AssertionError("waypoints module is missing"));
        context.runOnClient(client -> {
            var world = BaritoneAPI.getProvider().getPrimaryBaritone().getWorldProvider().getCurrentWorld();
            if (world == null || client.player == null) {
                throw new AssertionError("Baritone waypoint GameTest requires a loaded world");
            }
            world.getWaypoints().addWaypoint(new baritone.api.cache.Waypoint(
                    "", IWaypoint.Tag.HOME,
                    new BetterBlockPos(client.player.blockPosition()), 15L));
            modules.setEnabled(waypoints, true);
        });
        context.waitTicks(5);
        context.takeScreenshot("helikon-unnamed-home-waypoint");
        context.waitTicks(2);
        context.runOnClient(client -> {
            var world = BaritoneAPI.getProvider().getPrimaryBaritone().getWorldProvider().getCurrentWorld();
            if (world != null) {
                world.getWaypoints().getAllWaypoints().stream()
                        .filter(waypoint -> waypoint.getTag() == IWaypoint.Tag.HOME
                                && waypoint.getName().isEmpty()
                                && waypoint.getCreationTimestamp() == 15L)
                        .toList()
                        .forEach(world.getWaypoints()::removeWaypoint);
            }
            modules.setEnabled(waypoints, false);
        });
    }

    /** Intentional one-shot and mutually exclusive modules may disable; actual guarded failures may not occur. */
    private static void assertNoFailures(List<String> failures, String stage) {
        if (!failures.isEmpty()) {
            throw new AssertionError("Modules failed " + stage + ": " + failures);
        }
    }

    private static void cycleEntityEspModes(ClientGameTestContext context, ModuleRegistry modules) {
        for (EntityEspMode mode : EntityEspMode.values()) {
            context.runOnClient(client -> {
                Module entityEsp = modules.find("entity_esp").orElseThrow(
                        () -> new AssertionError("entity_esp module is missing"));
                @SuppressWarnings("unchecked")
                EnumSetting<EntityEspMode> setting = (EnumSetting<EntityEspMode>) entityEsp.settings().stream()
                        .filter(candidate -> candidate.id().equals("mode"))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("entity_esp mode setting is missing"));
                setting.set(mode);
            });
            context.waitTicks(10);
        }
    }
}
