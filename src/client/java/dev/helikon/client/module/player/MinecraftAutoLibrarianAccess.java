package dev.helikon.client.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Ordinary break/place adapter for one nearby loaded lectern. */
public final class MinecraftAutoLibrarianAccess {
    private BlockPos lectern;
    private Integer restoreSlot;

    public void tick(AutoLibrarian module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            restore(player);
            module.reset();
            lectern = null;
            return;
        }
        if (!module.isEnabled()) {
            restore(player);
            lectern = null;
            return;
        }
        if (client.gui.screen() instanceof MerchantScreen screen) {
            List<AutoLibrarian.Offer> offers = screen.getMenu().getOffers().stream()
                    .map(MinecraftAutoLibrarianAccess::facts).toList();
            if (module.inspect(offers) == AutoLibrarian.Decision.REROLL) {
                lectern = nearestLectern(client, player, module.radius());
                if (lectern != null) {
                    player.closeContainer();
                } else {
                    module.reset();
                }
            }
            return;
        }
        if (lectern == null) {
            return;
        }
        if (module.phase() == AutoLibrarian.Phase.BREAK_LECTERN) {
            if (client.level.getBlockState(lectern).isAir()) {
                module.markBroken();
                return;
            }
            client.gameMode.continueDestroyBlock(lectern, Direction.UP);
            player.swing(InteractionHand.MAIN_HAND);
        } else if (module.phase() == AutoLibrarian.Phase.PLACE_LECTERN) {
            int slot = lecternSlot(player);
            if (slot < 0 || !client.level.getBlockState(lectern).canBeReplaced()) {
                restore(player);
                lectern = null;
                module.reset();
                return;
            }
            if (restoreSlot == null && slot != player.getInventory().getSelectedSlot()) {
                restoreSlot = player.getInventory().getSelectedSlot();
                player.getInventory().setSelectedSlot(slot);
            }
            BlockPos support = lectern.below();
            client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false));
            player.swing(InteractionHand.MAIN_HAND);
            restore(player);
            lectern = null;
            module.markPlaced();
        }
    }

    private static AutoLibrarian.Offer facts(MerchantOffer offer) {
        ItemEnchantments enchantments = offer.getResult().get(DataComponents.STORED_ENCHANTMENTS);
        List<String> ids = enchantments == null ? List.of() : enchantments.keySet().stream()
                .map(holder -> holder.getRegisteredName()).toList();
        int emeralds = offer.getCostA().is(Items.EMERALD) ? offer.getCostA().getCount() : 64;
        return new AutoLibrarian.Offer(ids, emeralds);
    }

    private static BlockPos nearestLectern(Minecraft client, LocalPlayer player, int radius) {
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos center = player.blockPosition();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos position = center.offset(x, y, z);
                    if (client.level.hasChunk(position.getX() >> 4, position.getZ() >> 4)
                            && client.level.getBlockState(position).is(Blocks.LECTERN)) {
                        candidates.add(position.immutable());
                    }
                }
            }
        }
        return candidates.stream().min(Comparator.comparingDouble(position ->
                position.distToCenterSqr(player.getX(), player.getY(), player.getZ()))).orElse(null);
    }

    private static int lecternSlot(LocalPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(Items.LECTERN)) {
                return slot;
            }
        }
        return -1;
    }

    private void restore(LocalPlayer player) {
        if (restoreSlot != null && player != null) {
            player.getInventory().setSelectedSlot(restoreSlot);
        }
        restoreSlot = null;
    }
}
