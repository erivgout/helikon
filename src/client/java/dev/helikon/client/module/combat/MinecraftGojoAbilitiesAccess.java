package dev.helikon.client.module.combat;

import dev.helikon.client.module.chat.MinecraftChatSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Uses only ordinary hotbar selection, aim, item-use, and optional chat paths. */
public final class MinecraftGojoAbilitiesAccess {
    private final MinecraftChatSender chat = new MinecraftChatSender();
    private Integer restoreSlot;

    public void tick(long tick, GojoAbilities module, MinecraftCombatAccess.Snapshot snapshot) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (restoreSlot != null && player != null) {
            player.getInventory().setSelectedSlot(restoreSlot);
            restoreSlot = null;
        }
        if (player == null || client.level == null || client.gameMode == null || !snapshot.available()) {
            module.reset();
            return;
        }
        Optional<GojoAbilities.Action> next = module.next(tick, client.gui.screen() != null, snapshot.targets());
        if (next.isEmpty()) {
            return;
        }
        GojoAbilities.Action action = next.orElseThrow();
        LivingEntity target = snapshot.entities().get(action.target().id());
        if (target == null || !target.isAlive()) {
            return;
        }
        Item required = switch (action.ability()) {
            case RED -> Items.WIND_CHARGE;
            case BLUE -> Items.FISHING_ROD;
            case PURPLE -> Items.CROSSBOW;
        };
        int slot = find(player, required);
        if (slot < 0) {
            return;
        }
        int original = player.getInventory().getSelectedSlot();
        if (slot != original) {
            player.getInventory().setSelectedSlot(slot);
            restoreSlot = original;
        }
        aim(player, target);
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        if (action.sendIncantation()) {
            chat.send(action.incantation());
        }
        module.markUsed(tick);
    }

    private static int find(LocalPlayer player, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static void aim(LocalPlayer player, LivingEntity target) {
        Vec3 delta = target.getEyePosition().subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        player.setYRot((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0));
        player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, horizontal)));
    }
}
