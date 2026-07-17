package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

import java.util.List;

/** Slides only the local player's requested movement away from known cactus collision boxes. */
public final class AntiCactus extends Module {
    public AntiCactus() {
        super("anti_cactus", "AntiCactus", "Locally slides movement away from cactus collision boxes.",
                ModuleCategory.WORLD, false, Keybind.unbound());
    }

    /** Declines every world-driven movement source, even while the module is enabled. */
    public boolean shouldAdjust(boolean selfMovement) {
        return isEnabled() && selfMovement;
    }

    /** Applies the tested collision policy only while the module is enabled. */
    public CactusCollisionPolicy.Movement adjust(CactusCollisionPolicy.Movement requested,
                                                  CactusCollisionPolicy.Bounds playerBounds,
                                                  List<CactusCollisionPolicy.Bounds> cactusBounds) {
        if (!shouldAdjust(true)) {
            return requested;
        }
        return CactusCollisionPolicy.avoid(requested, playerBounds, cactusBounds);
    }
}
