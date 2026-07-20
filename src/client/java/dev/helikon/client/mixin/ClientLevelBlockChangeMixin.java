package dev.helikon.client.mixin;

import dev.helikon.client.event.ClientEventAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes accepted block changes after the local client world has applied them. */
@Mixin(ClientLevel.class)
abstract class ClientLevelBlockChangeMixin {
    @Inject(method = "setBlock", at = @At("RETURN"))
    private void helikon$postBlockChange(BlockPos position, BlockState state, int flags, int recursionLeft,
                                         CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue()) {
            return;
        }
        BlockState appliedState = ((ClientLevel) (Object) this).getBlockState(position);
        ClientEventAccess.postBlockChange(position.getX(), position.getY(), position.getZ(),
                BuiltInRegistries.BLOCK.getKey(appliedState.getBlock()).toString());
    }
}
