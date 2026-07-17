package dev.helikon.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses Minecraft's private, transient ordinary item-use cooldown. */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("rightClickDelay")
    int helikon$getRightClickDelay();

    @Accessor("rightClickDelay")
    void helikon$setRightClickDelay(int value);
}
