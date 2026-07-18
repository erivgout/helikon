package dev.helikon.client.module.movement;

import net.minecraft.world.entity.player.Input;

import java.util.Objects;

/** Narrow input bridge for normal BunnyHop and Scaffold jump/sneak requests. */
public final class AdvancedMovementInputAccess {
    private static volatile BunnyHop bunnyHop;
    private static volatile Scaffold scaffold;

    private AdvancedMovementInputAccess() {
    }

    public static void install(BunnyHop bunnyHopModule, Scaffold scaffoldModule) {
        bunnyHop = Objects.requireNonNull(bunnyHopModule, "bunnyHopModule");
        scaffold = Objects.requireNonNull(scaffoldModule, "scaffoldModule");
    }

    /** Applies only tested local input requests; callers retain Minecraft's physical input fields otherwise. */
    public static Input apply(Input input, boolean screenOpen, boolean onGround, boolean moving, boolean openBelow) {
        Input current = Objects.requireNonNull(input, "input");
        BunnyHop hop = bunnyHop;
        Scaffold scaffoldModule = scaffold;
        boolean scaffoldInputAllowed = !screenOpen && scaffoldModule != null;
        boolean jump = current.jump() || (hop != null && hop.shouldJump(onGround, moving, screenOpen))
                || (scaffoldInputAllowed && scaffoldModule.shouldRequestTowerJump(onGround, openBelow));
        boolean shift = current.shift() || (scaffoldInputAllowed && scaffoldModule.shouldRequestEdgeSafety(openBelow));
        if (jump == current.jump() && shift == current.shift()) {
            return current;
        }
        return new Input(current.forward(), current.backward(), current.left(), current.right(), jump, shift, current.sprint());
    }
}
