package dev.helikon.client.module.miscellaneous;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Narrow 26.2 adapter that performs only normal hotbar selection and item use. */
public final class MinecraftWindChargeAccess {
    private MinecraftWindChargeAccess() {
    }

    public static void tick(WindCharge module, long clientTick) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            module.onPlayerUnavailable();
            return;
        }

        int currentSlot = player.getInventory().getSelectedSlot();
        int chargeSlot = windChargeSlot(player, currentSlot);
        ItemStack charge = chargeSlot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(chargeSlot);
        WindCharge.Action action = module.update(clientTick, new WindCharge.Context(
                true,
                client.gui.screen() != null,
                player.isUsingItem(),
                !player.onGround() && player.getDeltaMovement().y < -0.01D,
                player.input.keyPresses.jump(),
                player.fallDistance,
                player.getXRot(),
                !charge.isEmpty() && player.getCooldowns().isOnCooldown(charge),
                currentSlot,
                chargeSlot
        ));
        switch (action.type()) {
            case NONE -> {
            }
            case RESTORE -> player.getInventory().setSelectedSlot(action.slot());
            case USE -> use(client, player);
            case SELECT_AND_USE -> {
                player.getInventory().setSelectedSlot(action.slot());
                use(client, player);
            }
        }
    }

    private static int windChargeSlot(LocalPlayer player, int currentSlot) {
        if (player.getInventory().getItem(currentSlot).is(Items.WIND_CHARGE)) {
            return currentSlot;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(Items.WIND_CHARGE)) {
                return slot;
            }
        }
        return -1;
    }

    private static void use(Minecraft client, LocalPlayer player) {
        if (client.gameMode != null) {
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }
    }
}
