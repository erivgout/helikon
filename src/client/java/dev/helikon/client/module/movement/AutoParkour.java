package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Requests one ordinary local jump only for a shallow, locally observed safe ledge. */
public final class AutoParkour extends Module {
    /** The maximum observed drop this conservative first version will consider safe. */
    public static final int MAXIMUM_SAFE_DROP_BLOCKS = 2;

    private final NumberSetting minimumMovementSpeed;

    public AutoParkour() {
        super("auto_parkour", "AutoParkour", "Requests a normal jump at safe local ledges.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        minimumMovementSpeed = addSetting(new NumberSetting("minimum_movement_speed", "Minimum movement speed",
                "Require this observed horizontal speed before requesting a jump.", 0.08D, 0.02D, 0.50D));
    }

    /** Returns whether the current safe local facts permit a normal jump input request. */
    public boolean shouldJump(ParkourContext context) {
        ParkourContext current = Objects.requireNonNull(context, "context");
        return isEnabled()
                && !current.screenOpen()
                && current.onGround()
                && current.movingForward()
                && current.horizontalSpeed() >= minimumMovementSpeed.value()
                && current.ledgeAhead()
                && !current.lavaAhead()
                && current.dropBlocks() <= MAXIMUM_SAFE_DROP_BLOCKS
                && current.landingSupportsPlayer();
    }
}
