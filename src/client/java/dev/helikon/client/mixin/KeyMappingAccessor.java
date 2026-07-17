package dev.helikon.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the currently configured key so AutoEat can preserve physical Use input. */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("key")
    InputConstants.Key helikon$getKey();
}
