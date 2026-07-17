package dev.helikon.client.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the verified 26.2 locally synchronized FishingHook bite flag without changing hook state. */
@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    @Accessor("biting")
    boolean helikon$isBiting();
}
