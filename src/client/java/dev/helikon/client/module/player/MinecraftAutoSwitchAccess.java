package dev.helikon.client.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Narrow 26.2 adapter for local hotbar facts and vanilla selected-slot state. */
public final class MinecraftAutoSwitchAccess {
    private MinecraftAutoSwitchAccess() {
    }

    public static void tick(AutoSwitch module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            module.onPlayerUnavailable();
            return;
        }

        List<AutoSwitch.HotbarItem> items = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                items.add(new AutoSwitch.HotbarItem(slot,
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
            }
        }
        AutoSwitch.Context context = new AutoSwitch.Context(
                player.getInventory().getSelectedSlot(),
                client.gui.screen() != null,
                client.options.keyAttack.isDown(),
                client.options.keyUse.isDown(),
                player.isShiftKeyDown(),
                player.getHealth() + player.getAbsorptionAmount(),
                items
        );
        AutoSwitch.Action action = module.update(context);
        if (action.type() == AutoSwitch.ActionType.SELECT || action.type() == AutoSwitch.ActionType.RESTORE) {
            player.getInventory().setSelectedSlot(action.slot());
        }
    }
}
