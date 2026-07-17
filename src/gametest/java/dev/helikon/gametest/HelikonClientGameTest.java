package dev.helikon.gametest;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.render.EntityEspMode;
import dev.helikon.client.setting.EnumSetting;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * In-engine smoke test: every registered module must survive real client
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
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            context.takeScreenshot("helikon-world-loaded");

            context.runOnClient(client -> modules.all().forEach(module -> modules.setEnabled(module, true)));
            context.waitTicks(SOAK_TICKS);
            assertAllStillEnabled(context, modules, "after enabling every module");
            context.takeScreenshot("helikon-all-modules-enabled");

            cycleEntityEspModes(context, modules);
            assertAllStillEnabled(context, modules, "after cycling EntityESP modes");

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

    /** Any module the failure handler auto-disabled indicates a real tick/render crash. */
    private static void assertAllStillEnabled(ClientGameTestContext context, ModuleRegistry modules, String stage) {
        List<String> failed = new ArrayList<>();
        context.runOnClient(client -> modules.all().stream()
                .filter(module -> !module.isEnabled())
                .forEach(module -> failed.add(module.id())));
        if (!failed.isEmpty()) {
            throw new AssertionError("Modules were auto-disabled by a failure " + stage + ": " + failed);
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
