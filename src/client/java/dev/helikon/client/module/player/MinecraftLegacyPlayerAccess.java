package dev.helikon.client.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Narrow adapter for legacy defensive hotbar use and sprint conservation. */
public final class MinecraftLegacyPlayerAccess {
    private MinecraftLegacyPlayerAccess() {
    }

    public static void tickAntiHunger(AntiHunger module) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && module.shouldStopSprinting(player.getFoodData().getFoodLevel(), player.isSprinting())) {
            player.setSprinting(false);
        }
    }

    public static void tickAntiFire(long tick, AntiFire module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            module.onContextLost();
            return;
        }
        apply(client, player, module, tick, player.isOnFire(), false);
    }

    public static void tickAntiPotion(long tick, AntiPotion module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            module.onContextLost();
            return;
        }
        boolean harmful = player.getActiveEffects().stream()
                .anyMatch(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
        apply(client, player, module, tick, harmful, false);
    }

    public static void tickFastEat(long tick, FastEat module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            module.onContextLost();
            return;
        }
        if (player.isUsingItem() && module.shouldRelease(player.getTicksUsingItem())) {
            client.gameMode.releaseUsingItem(player);
        }
        apply(client, player, module, tick, player.getFoodData().needsFood(), true);
    }

    private static void apply(Minecraft client, LocalPlayer player, HotbarUseModule module,
                              long tick, boolean triggered, boolean foodUse) {
        HotbarUseModule.Action action = module.update(tick,
                new HotbarUseModule.Context(player.getInventory().getSelectedSlot(), triggered,
                        client.gui.screen() != null, player.isUsingItem()), candidates(player));
        switch (action.type()) {
            case NONE -> {
            }
            case RESTORE -> player.getInventory().setSelectedSlot(action.slot());
            case USE_SELECTED -> useSelected(client, player, foodUse);
            case SELECT_AND_USE -> {
                player.getInventory().setSelectedSlot(action.slot());
                useSelected(client, player, foodUse);
            }
        }
    }

    private static void useSelected(Minecraft client, LocalPlayer player, boolean foodUse) {
        ItemStack selected = player.getMainHandItem();
        if (selected.is(Items.WATER_BUCKET) && !foodUse) {
            var position = player.blockPosition().below();
            client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false));
        } else {
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }
    }

    private static List<HotbarUseModule.Candidate> candidates(LocalPlayer player) {
        List<HotbarUseModule.Candidate> result = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || player.getCooldowns().isOnCooldown(stack)) {
                continue;
            }
            boolean food = stack.has(DataComponents.FOOD);
            boolean fireResistance = false;
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                for (var effect : contents.getAllEffects()) {
                    if (effect.is(MobEffects.FIRE_RESISTANCE)) {
                        fireResistance = true;
                        break;
                    }
                }
            }
            result.add(new HotbarUseModule.Candidate(slot, food, stack.is(Items.MILK_BUCKET),
                    stack.is(Items.WATER_BUCKET), fireResistance));
        }
        return List.copyOf(result);
    }
}
