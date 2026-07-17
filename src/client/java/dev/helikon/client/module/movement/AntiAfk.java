package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Schedules sparse, opt-in ordinary local input actions after an idle interval. */
public final class AntiAfk extends Module {
    /** The fixed bounded local look turn used when rotation is selected. */
    public static final float ROTATION_DEGREES = 15.0F;
    private static final int TICKS_PER_SECOND = 20;

    /** Immutable output for the thin Minecraft input adapter. */
    public record Action(float yawDegrees, boolean jump, boolean moveForward) {
        public static final Action NONE = new Action(0.0F, false, false);
    }

    /** Minecraft-free local activity facts for one client input tick. */
    public record Context(boolean screenOpen, boolean manualInput, boolean onGround) {
    }

    private final BooleanSetting rotation;
    private final BooleanSetting jump;
    private final BooleanSetting shortMovement;
    private final NumberSetting intervalSeconds;
    private int ticksUntilAction;

    public AntiAfk() {
        super("anti_afk", "AntiAFK", "Performs sparse selected local actions after idle time.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        rotation = addSetting(new BooleanSetting("rotation", "Rotation", "Turn the local view by 15 degrees.", true));
        jump = addSetting(new BooleanSetting("jump", "Jump", "Request one ordinary jump while grounded.", false));
        shortMovement = addSetting(new BooleanSetting("short_movement", "Short movement",
                "Request one ordinary forward-input tick.", false));
        intervalSeconds = addSetting(new NumberSetting("interval_seconds", "Interval seconds",
                "Wait this many seconds after local activity before an action.", 30.0D, 5.0D, 300.0D));
    }

    @Override
    protected void onEnable() {
        resetCountdown();
    }

    @Override
    protected void onDisable() {
        ticksUntilAction = 0;
    }

    /** Advances the idle timer and returns one sparse ordinary local action, if due. */
    public Action tick(Context context) {
        Context current = Objects.requireNonNull(context, "context");
        if (!isEnabled()) {
            return Action.NONE;
        }
        if (current.screenOpen() || current.manualInput()) {
            resetCountdown();
            return Action.NONE;
        }
        if (ticksUntilAction > 0) {
            ticksUntilAction--;
            return Action.NONE;
        }
        resetCountdown();
        return new Action(rotation.value() ? ROTATION_DEGREES : 0.0F,
                jump.value() && current.onGround(), shortMovement.value());
    }

    private void resetCountdown() {
        ticksUntilAction = Math.max(1, (int) Math.round(intervalSeconds.value() * TICKS_PER_SECOND));
    }
}
