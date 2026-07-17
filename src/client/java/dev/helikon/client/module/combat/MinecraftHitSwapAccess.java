package dev.helikon.client.module.combat;

import dev.helikon.client.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

/** Narrow 26.2 adapter around the verified ordinary attack and carried-item paths. */
public final class MinecraftHitSwapAccess {
    private static volatile HitSwap module;

    private MinecraftHitSwapAccess() {
    }

    public static void install(HitSwap hitSwap) {
        module = Objects.requireNonNull(hitSwap, "hitSwap");
    }

    /** Called at the head of {@code MultiPlayerGameMode.attack}, before vanilla synchronizes the held slot. */
    public static void beforeAttack(Player attackingPlayer) {
        HitSwap current = module;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer localPlayer = client.player;
        if (current == null || localPlayer == null || attackingPlayer != localPlayer) {
            return;
        }

        int currentSlot = localPlayer.getInventory().getSelectedSlot();
        int configuredSlot = current.weaponSlot() - 1;
        HitSwap.Action action = current.beforeAttack(
                currentSlot,
                !localPlayer.getInventory().getItem(configuredSlot).isEmpty()
        );
        if (action.type() == HitSwap.ActionType.SELECT) {
            localPlayer.getInventory().setSelectedSlot(action.slot());
        }
    }

    /** Restores a module-owned slot and asks vanilla to synchronize its own carried-item cache. */
    public static void tickRestore() {
        HitSwap current = module;
        if (current == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            current.onPlayerUnavailable();
            return;
        }

        HitSwap.Action action = current.restore(player.getInventory().getSelectedSlot());
        if (action.type() != HitSwap.ActionType.RESTORE) {
            return;
        }
        player.getInventory().setSelectedSlot(action.slot());
        if (client.gameMode != null && player.connection != null) {
            ((MultiPlayerGameModeAccessor) client.gameMode).helikon$ensureHasSentCarriedItem();
        }
    }
}
