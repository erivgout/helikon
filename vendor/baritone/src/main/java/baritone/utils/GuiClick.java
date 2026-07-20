/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.Collections;

import static baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class GuiClick extends Screen implements Helper {

    private BlockPos clickStart;
    private BlockPos currentMouseOver;

    public GuiClick() {
        super(Component.literal("CLICK"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateHoveredBlock(mc.gameRenderer.mainCamera());
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * Updates the click ray from Minecraft's current camera basis. Using the
     * camera's own near plane avoids projection/view convention mismatches
     * across renderer versions.
     */
    public void updateHoveredBlock(Camera camera) {
        if (camera == null || !camera.isInitialized() || mc.level == null
                || mc.getWindow().getScreenWidth() <= 0 || mc.getWindow().getScreenHeight() <= 0) {
            currentMouseOver = null;
            return;
        }
        float normalizedX = (float) (mc.mouseHandler.xpos() * 2.0D
                / mc.getWindow().getScreenWidth() - 1.0D);
        float normalizedY = (float) (1.0D - mc.mouseHandler.ypos() * 2.0D
                / mc.getWindow().getScreenHeight());
        Vec3 direction = camera.getNearPlane(camera.getFov())
                .getPointOnPlane(normalizedX, normalizedY)
                .normalize();
        Vec3 start = camera.position();
        Vec3 end = start.add(direction.scale(512.0D));
        LocalPlayer player = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().player();
        HitResult result = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            currentMouseOver = ((BlockHitResult) result).getBlockPos();
            return;
        }
        currentMouseOver = null;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Prevent default background rendering
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (currentMouseOver != null) { //Catch this, or else a click into void will result in a crash
            if (event.button() == 0) {
                if (clickStart != null && !clickStart.equals(currentMouseOver)) {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().removeAllSelections();
                    BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().addSelection(BetterBlockPos.from(clickStart), BetterBlockPos.from(currentMouseOver));
                    MutableComponent component = Component.literal("Selection made! For usage: " + Baritone.settings().prefix.value + "help sel");
                    component.setStyle(component.getStyle()
                            .withColor(ChatFormatting.WHITE)
                            .withClickEvent(new ClickEvent.RunCommand(
                                    FORCE_COMMAND_PREFIX + "help sel"
                            )));
                    Helper.HELPER.logDirect(component);
                    clickStart = null;
                } else {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentMouseOver));
                }
            } else if (event.button() == 1) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentMouseOver.above()));
            }
        }
        clickStart = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        clickStart = currentMouseOver;
        return super.mouseClicked(event, doubleClick);
    }

    public void onRender(PoseStack modelViewStack, Matrix4f projectionMatrix) {
        if (currentMouseOver != null) {
            Entity e = mc.getCameraEntity();
            // drawSingleSelectionBox WHEN?
            PathRenderer.drawManySelectionBoxes(modelViewStack, e, Collections.singletonList(currentMouseOver), Color.CYAN);
            if (clickStart != null && !clickStart.equals(currentMouseOver)) {
                BufferBuilder bufferBuilder = IRenderer.startLines(Color.RED);
                BetterBlockPos a = new BetterBlockPos(currentMouseOver);
                BetterBlockPos b = new BetterBlockPos(clickStart);
                IRenderer.emitAABB(bufferBuilder, modelViewStack, new AABB(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z), Math.max(a.x, b.x) + 1, Math.max(a.y, b.y) + 1, Math.max(a.z, b.z) + 1), Baritone.settings().pathRenderLineWidthPixels.value);
                IRenderer.endLines(bufferBuilder, true);
            }
        }
    }

    /**
     * Exposes the click target to Helikon's Minecraft 26.2 gizmo renderer.
     * Baritone's legacy direct-buffer renderer is intentionally inert on this
     * port, so the supported renderer needs this read-only preview state.
     */
    public BlockPos hoveredBlock() {
        return currentMouseOver;
    }

    /**
     * Returns the inclusive drag selection as block-aligned world bounds.
     */
    public AABB selectionPreviewBounds() {
        if (clickStart == null || currentMouseOver == null || clickStart.equals(currentMouseOver)) {
            return null;
        }
        return new AABB(
                Math.min(clickStart.getX(), currentMouseOver.getX()),
                Math.min(clickStart.getY(), currentMouseOver.getY()),
                Math.min(clickStart.getZ(), currentMouseOver.getZ()),
                Math.max(clickStart.getX(), currentMouseOver.getX()) + 1,
                Math.max(clickStart.getY(), currentMouseOver.getY()) + 1,
                Math.max(clickStart.getZ(), currentMouseOver.getZ()) + 1
        );
    }

}
