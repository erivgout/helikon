package dev.helikon.client.module.player;

import dev.helikon.client.module.combat.MinecraftCombatAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.List;

/** Narrow 26.2 adapter for splash-potion inspection, local aim, and ordinary held-item use. */
public final class MinecraftThrowDebuffAccess {
    private MinecraftThrowDebuffAccess() {
    }

    public static void tick(long clientTick, ThrowDebuff module, MinecraftCombatAccess.Snapshot snapshot) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (!snapshot.available() || player == null || client.level == null || client.gameMode == null) {
            module.onPlayerUnavailable();
            return;
        }

        ThrowDebuff.Action action = module.update(clientTick, new ThrowDebuff.Context(
                player.getInventory().getSelectedSlot(),
                client.gui.screen() != null,
                player.isUsingItem(),
                potionCandidates(player),
                snapshot.targets()
        ));
        switch (action.type()) {
            case NONE -> {
            }
            case RESTORE_SLOT -> player.getInventory().setSelectedSlot(action.slot());
            case THROW_SELECTED -> throwPotion(client, player, action);
            case SELECT_AND_THROW -> {
                player.getInventory().setSelectedSlot(action.slot());
                throwPotion(client, player, action);
            }
        }
    }

    private static void throwPotion(Minecraft client, LocalPlayer player, ThrowDebuff.Action action) {
        if (action.rotate()) {
            player.setYRot(action.yaw());
            player.setXRot(action.pitch());
        }
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
    }

    private static List<ThrowDebuff.PotionCandidate> potionCandidates(LocalPlayer player) {
        List<ThrowDebuff.PotionCandidate> candidates = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof SplashPotionItem) || player.getCooldowns().isOnCooldown(stack)) {
                continue;
            }
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) {
                continue;
            }
            boolean hasEffect = false;
            boolean harmfulOnly = true;
            for (MobEffectInstance effect : contents.getAllEffects()) {
                hasEffect = true;
                if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
                    harmfulOnly = false;
                    break;
                }
            }
            String potionId = contents.potion().map(holder -> holder.value().name()).orElse("custom");
            if (hasEffect && !potionId.isBlank()) {
                candidates.add(new ThrowDebuff.PotionCandidate(slot, potionId, harmfulOnly));
            }
        }
        return List.copyOf(candidates);
    }
}
