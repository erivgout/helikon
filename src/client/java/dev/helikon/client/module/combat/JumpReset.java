package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/**
 * Times an ordinary local Jump input on the tick the player receives a hit so
 * Minecraft's normal ground friction reduces the received horizontal knockback.
 */
public final class JumpReset extends Module {
    private final BooleanSetting requireMovement;
    private final NumberSetting cooldownTicks;
    private long lastResetTick = -1L;

    public JumpReset() {
        super("jump_reset", "JumpReset", "Times a normal jump when hit to reduce received knockback.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        requireMovement = addSetting(new BooleanSetting("require_movement", "Require movement",
                "Only jump-reset when there is local horizontal movement or knockback.", true));
        cooldownTicks = addSetting(new NumberSetting("cooldown_ticks", "Cooldown",
                "Minimum ticks between jump resets.", 4.0D, 1.0D, 40.0D));
    }

    /**
     * Requests a jump only on the tick a fresh hit is observed while grounded and no screen is open.
     * The connected server remains authoritative over whether the jump and the reduced knockback apply.
     */
    public boolean shouldJumpReset(long tick, JumpResetContext context) {
        if (tick < 0L || context == null) {
            throw new IllegalArgumentException("jump reset inputs are invalid");
        }
        if (!isEnabled() || context.screenOpen() || !context.onGround() || !context.freshHit()) {
            return false;
        }
        if (requireMovement.value() && !context.movingHorizontally()) {
            return false;
        }
        if (lastResetTick >= 0L && tick - lastResetTick < Math.round(cooldownTicks.value())) {
            return false;
        }
        lastResetTick = tick;
        return true;
    }

    @Override
    protected void onDisable() {
        lastResetTick = -1L;
    }
}
