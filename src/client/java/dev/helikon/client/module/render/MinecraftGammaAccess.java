package dev.helikon.client.module.render;

import net.minecraft.client.Minecraft;

/** Minecraft-only adapter for the validated {@code Options.gamma()} API. */
public final class MinecraftGammaAccess implements FullbrightGammaController.GammaAccess {
    @Override
    public boolean isAvailable() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.options != null;
    }

    @Override
    public double gamma() {
        return Minecraft.getInstance().options.gamma().get();
    }

    @Override
    public void setGamma(double value) {
        Minecraft.getInstance().options.gamma().set(value);
    }
}
