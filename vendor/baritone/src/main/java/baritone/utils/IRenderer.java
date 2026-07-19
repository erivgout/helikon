/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package baritone.utils;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.utils.accessor.IEntityRenderManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * Minecraft 26.2 moved ad-hoc world geometry to its gizmo render phase.
 *
 * The original Baritone renderer is intentionally inert in the embedded
 * component. Helikon renders Baritone goals and paths from its supported
 * Fabric gizmo callback instead, while these compatibility methods keep
 * upstream selection and pathing code source-compatible.
 */
public interface IRenderer {

    IEntityRenderManager renderManager = (IEntityRenderManager) Minecraft.getInstance().getEntityRenderDispatcher();
    Settings settings = BaritoneAPI.getSettings();
    float[] color = new float[]{1.0F, 1.0F, 1.0F, 1.0F};

    static void glColor(Color value, float alpha) {
        float[] components = value.getColorComponents(null);
        color[0] = components[0];
        color[1] = components[1];
        color[2] = components[2];
        color[3] = alpha;
    }

    static BufferBuilder startLines(Color value, float alpha) {
        glColor(value, alpha);
        return null;
    }

    static BufferBuilder startLines(Color value) {
        return startLines(value, 0.4F);
    }

    static void endLines(BufferBuilder ignored, boolean ignoredDepth) {
    }

    static BufferBuilder startBlockQuads() {
        return null;
    }

    static void endBuffer(BufferBuilder ignored, RenderType ignoredType) {
    }

    static void emitLine(BufferBuilder ignored, PoseStack stack,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2, float lineWidth) {
    }

    static void emitLine(BufferBuilder ignored, PoseStack stack,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         double nx, double ny, double nz, float lineWidth) {
    }

    static void emitLine(BufferBuilder ignored, PoseStack stack,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float nx, float ny, float nz, float lineWidth) {
    }

    static void emitLine(BufferBuilder ignored, PoseStack stack, Vec3 start, Vec3 end, float lineWidth) {
    }

    static void emitAABB(BufferBuilder ignored, PoseStack stack, AABB box, float lineWidth) {
    }

    static void emitAABB(BufferBuilder ignored, PoseStack stack, AABB box, double expand, float lineWidth) {
    }

    static void emitTexturedVertex(BufferBuilder ignored, PoseStack.Pose pose,
                                   float x, float y, float z, int vertexColor,
                                   float u, float v, float nx, float ny, float nz) {
    }

    static RenderType beaconBeam(Identifier identifier, boolean translucent) {
        return null;
    }

    static RenderType beaconBeam(Identifier identifier, boolean translucent, boolean ignoreDepth) {
        return null;
    }
}
