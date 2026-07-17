package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

/** Configures an independently toggled, local-only detached camera. */
public final class Freecam extends Module {
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
}
