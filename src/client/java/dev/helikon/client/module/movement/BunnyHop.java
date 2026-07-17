package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Adds normal local jump input while moving on ground and caps assisted horizontal momentum. */
public final class BunnyHop extends Module {
    private final BooleanSetting autoJump;
    private final NumberSetting speedLimit;

    public BunnyHop() {
        super("bunny_hop", "BunnyHop", "Requests normal jumps while moving, with a conservative speed cap.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        autoJump = addSetting(new BooleanSetting("auto_jump", "Auto jump",
                "Request normal local jump input while moving on ground.", true));
        speedLimit = addSetting(new NumberSetting("speed_limit", "Speed limit",
                "Maximum local horizontal speed retained by hop assistance.", 0.30D, 0.05D, 0.45D));
    }

    public boolean shouldJump(boolean onGround, boolean moving, boolean screenOpen) {
        return isEnabled() && autoJump.value() && onGround && moving && !screenOpen;
    }

    public HorizontalVelocity cap(HorizontalVelocity velocity) {
        if (velocity == null) {
            throw new IllegalArgumentException("velocity must not be null");
        }
        return isEnabled() ? velocity.capped(speedLimit.value()) : velocity;
    }
}
