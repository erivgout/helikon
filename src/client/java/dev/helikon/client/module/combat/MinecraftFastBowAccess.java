package dev.helikon.client.module.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BowItem;

/** Minecraft-only ordinary bow-release adapter. */
public final class MinecraftFastBowAccess {
    private MinecraftFastBowAccess() {
    }

    public static void tick(FastBow module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        boolean bow = client.player.getUseItem().getItem() instanceof BowItem;
        if (module.shouldRelease(bow, client.player.isUsingItem(), client.player.getTicksUsingItem(),
                dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen()))) {
            client.gameMode.releaseUsingItem(client.player);
        }
    }
}
