package dev.helikon.client.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Delegates glint geometry while replacing only its per-vertex local color. */
final class RainbowEnchantVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int color;

    RainbowEnchantVertexConsumer(VertexConsumer delegate, int color) {
        this.delegate = delegate;
        this.color = color;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        delegate.setColor(color);
        return this;
    }

    @Override
    public VertexConsumer setColor(int ignoredColor) {
        delegate.setColor(color);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        delegate.setLineWidth(width);
        return this;
    }
}
