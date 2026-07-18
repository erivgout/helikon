package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

/** Configures an independently toggled, local-only detached camera. */
public final class Freecam extends Module {
    public record Rotation(float yaw, float pitch) {
        public Rotation {
            if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || pitch < -90.0F || pitch > 90.0F) {
                throw new IllegalArgumentException("freecam rotation is invalid");
            }
        }
    }

    private final NumberSetting speed;

    public Freecam() {
        super("freecam", "Freecam",
                "Detaches a local-only camera without moving or sending movement input for the player.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        speed = addSetting(new NumberSetting("speed", "Speed",
                "Local-only detached camera movement per client tick.", 0.15D, 0.02D, 0.50D));
    }

    public double speed() {
        return speed.value();
    }

    /** Mirrors vanilla's bounded mouse-turn math without any Minecraft dependency. */
    public Rotation turn(float yaw, float pitch, double deltaX, double deltaY) {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)
                || !Double.isFinite(deltaX) || !Double.isFinite(deltaY)) {
            throw new IllegalArgumentException("freecam turn input is invalid");
        }
        float nextYaw = yaw + (float) deltaX * 0.15F;
        float nextPitch = Math.clamp(pitch + (float) deltaY * 0.15F, -90.0F, 90.0F);
        return new Rotation(nextYaw, nextPitch);
    }
}
