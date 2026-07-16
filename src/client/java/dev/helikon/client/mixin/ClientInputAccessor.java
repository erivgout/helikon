package dev.helikon.client.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives the keyboard-input tail hook access to the already-normalized 26.2 movement vector. */
@Mixin(ClientInput.class)
interface ClientInputAccessor {
    @Accessor("moveVector")
    void helikon$setMoveVector(Vec2 value);
}
