package dev.helikon.client.module.world;

import dev.helikon.client.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;

/** Minecraft-only port for the private, transient ordinary item-use cooldown. */
public final class MinecraftUseCooldownAccess implements FastPlace.CooldownAccess {
    @Override
    public int delay() {
        return ((MinecraftAccessor) Minecraft.getInstance()).helikon$getRightClickDelay();
    }

    @Override
    public void setDelay(int value) {
        ((MinecraftAccessor) Minecraft.getInstance()).helikon$setRightClickDelay(value);
    }
}
