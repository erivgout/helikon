package dev.helikon.client.automation;

import java.util.List;

/** Safe, deterministic sequences made from ordinary vanilla container clicks. */
public final class ContainerClickSequence {
    private ContainerClickSequence() {
    }

    /** Swaps two menu slots using the normal cursor path and leaves the cursor empty. */
    public static List<ContainerClick> swap(int sourceSlot, int destinationSlot) {
        if (sourceSlot == destinationSlot) {
            return List.of();
        }
        return List.of(
                new ContainerClick(sourceSlot, 0, ContainerClick.Type.PICKUP),
                new ContainerClick(destinationSlot, 0, ContainerClick.Type.PICKUP),
                new ContainerClick(sourceSlot, 0, ContainerClick.Type.PICKUP)
        );
    }

    public static List<ContainerClick> quickMove(int sourceSlot) {
        return List.of(new ContainerClick(sourceSlot, 0, ContainerClick.Type.QUICK_MOVE));
    }

    /** Button one is vanilla's whole-stack throw action. */
    public static List<ContainerClick> throwStack(int sourceSlot) {
        return List.of(new ContainerClick(sourceSlot, 1, ContainerClick.Type.THROW));
    }

    /** Button zero is vanilla's single-item throw action. */
    public static List<ContainerClick> throwOne(int sourceSlot) {
        return List.of(new ContainerClick(sourceSlot, 0, ContainerClick.Type.THROW));
    }
}
