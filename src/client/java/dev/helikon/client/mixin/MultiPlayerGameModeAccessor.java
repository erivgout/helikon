package dev.helikon.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Accesses Minecraft's private, transient normal destroy-input cooldown. */
@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("destroyDelay")
    int helikon$getDestroyDelay();

    @Accessor("destroyDelay")
    void helikon$setDestroyDelay(int value);

    @Invoker("ensureHasSentCarriedItem")
    void helikon$ensureHasSentCarriedItem();
}
