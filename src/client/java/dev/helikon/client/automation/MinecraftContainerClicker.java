package dev.helikon.client.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;

import java.util.List;
import java.util.Objects;

/** Narrow 26.2 adapter that routes automation through Minecraft's normal container-input method. */
public final class MinecraftContainerClicker {
    private MinecraftContainerClicker() {
    }

    /**
     * Applies a prevalidated sequence only when the normal menu cursor is empty.
     * Minecraft updates local menu state and emits its regular container clicks; Helikon
     * never creates packets or edits inventory storage directly.
     */
    public static boolean apply(Minecraft client, List<ContainerClick> clicks) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(clicks, "clicks");
        if (clicks.isEmpty()) {
            return true;
        }
        if (client.player == null || client.gameMode == null || !client.player.containerMenu.getCarried().isEmpty()) {
            return false;
        }
        int containerId = client.player.containerMenu.containerId;
        for (ContainerClick click : clicks) {
            client.gameMode.handleContainerInput(containerId, click.slot(), click.button(), map(click.type()), client.player);
        }
        return true;
    }

    private static ContainerInput map(ContainerClick.Type type) {
        return switch (type) {
            case PICKUP -> ContainerInput.PICKUP;
            case QUICK_MOVE -> ContainerInput.QUICK_MOVE;
            case THROW -> ContainerInput.THROW;
        };
    }
}
