package dev.helikon.client.module.world;

import dev.helikon.client.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

/** Minecraft-only port for the ordinary client destroy cooldown. */
public final class MinecraftBreakCooldownAccess implements FastBreak.CooldownAccess {
    @Override
    public int delay() {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        return gameMode == null ? 0 : ((MultiPlayerGameModeAccessor) gameMode).helikon$getDestroyDelay();
    }

    @Override
    public void setDelay(int value) {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode != null) {
            ((MultiPlayerGameModeAccessor) gameMode).helikon$setDestroyDelay(value);
        }
    }
}
