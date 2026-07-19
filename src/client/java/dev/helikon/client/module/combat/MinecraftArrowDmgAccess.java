package dev.helikon.client.module.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Narrow 26.2 bridge that inserts only vanilla-shaped command and movement packets
 * immediately before Minecraft sends the ordinary release-use-item action.
 */
public final class MinecraftArrowDmgAccess {
    private static volatile ArrowDmg module;

    private MinecraftArrowDmgAccess() {
    }

    public static void install(ArrowDmg arrowDmg) {
        module = Objects.requireNonNull(arrowDmg, "arrowDmg");
    }

    public static void beforeRelease(Player releasingPlayer) {
        ArrowDmg current = module;
        Minecraft client = Minecraft.getInstance();
        if (current == null || !(releasingPlayer instanceof LocalPlayer player) || client.player != player) {
            return;
        }
        Vec3 view = player.getViewVector(1.0F);
        ArrowDmg.ReleaseContext context = new ArrowDmg.ReleaseContext(
                player.getUseItem().getItem() instanceof BowItem,
                player.getTicksUsingItem(),
                dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen()),
                player.connection != null,
                view.x,
                view.z
        );
        current.planRelease(context).ifPresent(plan -> sendPlan(player, plan));
    }

    private static void sendPlan(LocalPlayer player, ArrowDmg.MovementPlan plan) {
        if (plan.sprintSignal()) {
            player.connection.send(new ServerboundPlayerCommandPacket(player,
                    ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        }
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        for (int index = 0; index < plan.stationaryPackets(); index++) {
            sendPosition(player, x, y, z, true);
        }
        sendPosition(player, x + plan.offsetX(), y, z + plan.offsetZ(), true);
        sendPosition(player, x, y, z, false);
    }

    private static void sendPosition(LocalPlayer player, double x, double y, double z, boolean onGround) {
        player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround,
                player.horizontalCollision));
    }
}
