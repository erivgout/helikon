package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Local Elytra pitch assistance, speed facts, and low-durability warning policy. */
public final class ExtraElytra extends Module {
    public record Status(double speed, boolean lowDurability) {
    }

    private final BooleanSetting pitchAssist;
    private final NumberSetting targetPitch;
    private final NumberSetting pitchAdjustment;
    private final BooleanSetting saferLanding;
    private final NumberSetting landingDistance;
    private final NumberSetting durabilityWarning;
    private final BooleanSetting speedDisplay;

    public ExtraElytra() {
        super("extra_elytra", "ExtraElytra", "Adds conservative local Elytra pitch, speed, and landing assistance.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        pitchAssist = addSetting(new BooleanSetting("pitch_assist", "Pitch assist", "Nudge flight pitch toward a target.", true));
        targetPitch = addSetting(new NumberSetting("target_pitch", "Target pitch", "Desired local Elytra pitch in degrees.", -10.0D, -45.0D, 30.0D));
        pitchAdjustment = addSetting(new NumberSetting("pitch_adjustment", "Pitch adjustment", "Maximum local pitch change per tick.",
                1.5D, 0.1D, 8.0D));
        saferLanding = addSetting(new BooleanSetting("safer_landing", "Safer landing", "Level pitch when descending near ground.", true));
        landingDistance = addSetting(new NumberSetting("landing_distance", "Landing distance", "Ground distance for landing pitch assistance.",
                6.0D, 1.0D, 24.0D));
        durabilityWarning = addSetting(new NumberSetting("durability_warning", "Durability warning", "Remaining Elytra durability warning threshold.",
                12.0D, 1.0D, 432.0D));
        speedDisplay = addSetting(new BooleanSetting("speed_display", "Speed display", "Show local Elytra speed in the HUD.", true));
    }

    public float adjustedPitch(float currentPitch, boolean fallFlying, boolean descending, double groundDistance) {
        if (!Float.isFinite(currentPitch) || !Double.isFinite(groundDistance) || groundDistance < 0.0D) {
            throw new IllegalArgumentException("pitch facts are invalid");
        }
        if (!isEnabled() || !fallFlying) {
            return currentPitch;
        }
        double target = targetPitch.value();
        if (saferLanding.value() && descending && groundDistance <= landingDistance.value()) {
            target = 0.0D;
        }
        if (!pitchAssist.value() && !(saferLanding.value() && descending && groundDistance <= landingDistance.value())) {
            return currentPitch;
        }
        double delta = Math.max(-pitchAdjustment.value(), Math.min(pitchAdjustment.value(), target - currentPitch));
        return (float) (currentPitch + delta);
    }

    public Status status(HorizontalVelocity horizontalVelocity, double verticalVelocity, int remainingDurability) {
        if (horizontalVelocity == null || !Double.isFinite(verticalVelocity) || remainingDurability < 0) {
            throw new IllegalArgumentException("Elytra status facts are invalid");
        }
        return new Status(Math.sqrt(horizontalVelocity.speed() * horizontalVelocity.speed() + verticalVelocity * verticalVelocity),
                remainingDurability <= Math.round(durabilityWarning.value()));
    }

    public boolean showSpeed() {
        return isEnabled() && speedDisplay.value();
    }
}
