package dev.helikon.gametest;

import baritone.api.BaritoneAPI;
import baritone.api.selection.ISelection;
import baritone.utils.GuiClick;
import dev.helikon.client.HelikonClient;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.world.BaritoneNavigation;
import dev.helikon.client.render.MinecraftBaritoneVisualizationRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * In-engine regression for #click: a real client screen, mouse drag, world
 * raycast, Gizmo submission, and saved Baritone selection must all survive.
 */
public final class BaritoneClickClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        ModuleRegistry modules = HelikonClient.activeModuleRegistry();
        if (modules == null) {
            throw new AssertionError("Helikon module registry was not initialized");
        }
        BaritoneNavigation baritone = (BaritoneNavigation) modules.find("baritone")
                .orElseThrow(() -> new AssertionError("baritone module is missing"));

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getServer().runCommand("fill -8 -62 6 8 -46 6 minecraft:stone");
            singleplayer.getServer().runCommand("tp @a 0 -54 -6 0 0");
            context.waitTicks(20);
            singleplayer.getClientLevel().waitForChunksRender();

            context.runOnClient(client -> {
                modules.disableAll();
                modules.setEnabled(baritone, true);
                BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().removeAllSelections();
                baritone.execute("click");
            });
            context.waitForScreen(GuiClick.class);
            context.waitTicks(5);

            int centerX = context.computeOnClient(client -> client.getWindow().getScreenWidth() / 2);
            int centerY = context.computeOnClient(client -> client.getWindow().getScreenHeight() / 2);
            context.getInput().setCursorPos(centerX - 80, centerY);
            context.waitTicks(3);
            assertHoverAndGizmo(context);
            assertHoveredTestWall(context, "first");

            context.getInput().holdMouse(0);
            context.waitTicks(2);
            context.getInput().setCursorPos(centerX + 80, centerY);
            context.waitTicks(5);
            assertHoveredTestWall(context, "second");
            boolean hasDragPreview = context.computeOnClient(client ->
                    client.gui.screen() instanceof GuiClick click && click.selectionPreviewBounds() != null);
            if (!hasDragPreview) {
                throw new AssertionError("#click did not retain a drag selection preview");
            }
            if (!MinecraftBaritoneVisualizationRenderer.inspectInteractive().clickDrag()) {
                throw new AssertionError("#click drag bounds were not submitted to the live Gizmo phase");
            }
            Path dragScreenshot = context.takeScreenshot("baritone-click-drag-preview");
            assertVisibleOutlinePixels(dragScreenshot, true);

            context.getInput().releaseMouse(0);
            context.waitTicks(5);
            ISelection[] selections = BaritoneAPI.getProvider().getPrimaryBaritone()
                    .getSelectionManager().getSelections();
            if (selections.length != 1) {
                throw new AssertionError("#click drag did not create exactly one Baritone selection: "
                        + selections.length);
            }
            if (selections[0].pos1().z != 6 || selections[0].pos2().z != 6) {
                throw new AssertionError("#click selected blocks beyond the near test wall: "
                        + selections[0].pos1() + " -> " + selections[0].pos2());
            }
            if (MinecraftBaritoneVisualizationRenderer.inspectInteractive().selections() != 1) {
                throw new AssertionError("saved #click selection was not submitted to the live Gizmo phase");
            }
            Path selectionScreenshot = context.takeScreenshot("baritone-click-saved-selection");
            assertVisibleOutlinePixels(selectionScreenshot, false);

            context.runOnClient(client -> {
                client.gui.setScreen(null);
                BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().removeAllSelections();
                modules.setEnabled(baritone, false);
            });
        }
    }

    private static void assertHoverAndGizmo(ClientGameTestContext context) {
        boolean hasHover = context.computeOnClient(client ->
                client.gui.screen() instanceof GuiClick click && click.hoveredBlock() != null);
        if (!hasHover) {
            throw new AssertionError("#click did not raycast a hovered block in the real client");
        }
        if (!MinecraftBaritoneVisualizationRenderer.inspectInteractive().clickHover()) {
            throw new AssertionError("#click hover was not submitted to the live Gizmo phase");
        }
    }

    private static void assertHoveredTestWall(ClientGameTestContext context, String point) {
        int hoveredZ = context.computeOnClient(client ->
                client.gui.screen() instanceof GuiClick click && click.hoveredBlock() != null
                        ? click.hoveredBlock().getZ()
                        : Integer.MIN_VALUE);
        if (hoveredZ != 6) {
            throw new AssertionError("#click " + point + " cursor point missed the near test wall (z="
                    + hoveredZ + ")");
        }
    }

    private static void assertVisibleOutlinePixels(Path screenshot, boolean requireDragRed) {
        BufferedImage image;
        try {
            image = ImageIO.read(screenshot.toFile());
        } catch (IOException exception) {
            throw new AssertionError("Could not read #click GameTest screenshot", exception);
        }
        int cyan = 0;
        int red = 0;
        int worldBottom = Math.max(0, image.getHeight() - 100);
        for (int y = 0; y < worldBottom; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y);
                int r = color >> 16 & 0xFF;
                int g = color >> 8 & 0xFF;
                int b = color & 0xFF;
                if (g > 140 && b > 140 && r < 140) {
                    cyan++;
                }
                if (r > 170 && r > g * 1.35D && r > b * 1.35D) {
                    red++;
                }
            }
        }
        if (cyan < 8) {
            throw new AssertionError("#click screenshot contains no visible cyan hover outline: " + screenshot);
        }
        if (requireDragRed && red < 8) {
            throw new AssertionError("#click screenshot contains no visible red drag outline: " + screenshot);
        }
    }
}
