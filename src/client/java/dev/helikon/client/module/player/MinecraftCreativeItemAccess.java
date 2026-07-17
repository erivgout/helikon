package dev.helikon.client.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;
import java.util.Optional;

/** Narrow adapter for normal Creative inventory stack insertion with bounded vanilla components. */
public final class MinecraftCreativeItemAccess {
    private MinecraftCreativeItemAccess() {
    }

    public static void tick(CreativeItemModule module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) {
            module.onContextLost();
            return;
        }
        module.nextRequest(client.player.isCreative(), client.gui.screen() != null).ifPresent(request -> {
            ItemStack stack = create(request);
            if (stack.isEmpty()) {
                return;
            }
            int creativeSlot = 36 + client.player.getInventory().getSelectedSlot();
            client.gameMode.handleCreativeModeItemAdd(stack, creativeSlot);
            module.markDelivered();
        });
    }

    private static ItemStack create(CreativeItemModule.Request request) {
        ItemStack stack = switch (request.kind()) {
            case ITEM -> configuredItem(request);
            case KILL_POTION -> potion(Items.SPLASH_POTION, List.of(
                    new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 4),
                    new MobEffectInstance(MobEffects.WITHER, 200, 2)));
            case TROLL_POTION -> potion(Items.SPLASH_POTION, List.of(
                    new MobEffectInstance(MobEffects.NAUSEA, 600, 1),
                    new MobEffectInstance(MobEffects.SLOWNESS, 600, 2),
                    new MobEffectInstance(MobEffects.GLOWING, 600, 0)));
            case COMMAND_BLOCK -> new ItemStack(Items.COMMAND_BLOCK);
        };
        if (!request.customName().isBlank() && !stack.isEmpty()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(request.customName()));
        }
        return stack;
    }

    private static ItemStack configuredItem(CreativeItemModule.Request request) {
        Identifier id = Identifier.tryParse(request.itemId());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        return item.map(value -> new ItemStack(value, request.count())).orElse(ItemStack.EMPTY);
    }

    private static ItemStack potion(Item item, List<MobEffectInstance> effects) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.empty(), Optional.empty(), effects, Optional.empty()));
        return stack;
    }
}
