package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Steers toward the nearest already-rendered entity with the configured name. */
public final class Follow extends Module {
    public record Target(String name, double horizontalDistance, double deltaX, double deltaZ) {
        public Target {
            if (name == null || name.isBlank() || !Double.isFinite(horizontalDistance) || horizontalDistance < 0.0D
                    || !Double.isFinite(deltaX) || !Double.isFinite(deltaZ)) {
                throw new IllegalArgumentException("follow target is invalid");
            }
        }
    }

    public record Context(boolean screenOpen, boolean passenger, boolean abilityFlying, boolean fallFlying,
                          List<Target> targets) {
        public Context {
            targets = List.copyOf(Objects.requireNonNull(targets, "targets"));
        }
    }

    private final StringSetting targetName;
    private final NumberSetting range;
    private final NumberSetting stopDistance;
    private final NumberSetting speed;

    public Follow() {
        super("follow", "Follow", "Moves toward the nearest loaded entity with a configured name.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        targetName = addSetting(new StringSetting("target_name", "Target name",
                "Exact visible name of the loaded entity to follow.", "", 64, true));
        range = addSetting(new NumberSetting("range", "Range", "Maximum local horizontal target distance.",
                32.0D, 2.0D, 64.0D));
        stopDistance = addSetting(new NumberSetting("stop_distance", "Stop distance",
                "Horizontal distance at which movement toward the target stops.", 2.0D, 0.5D, 8.0D));
        speed = addSetting(new NumberSetting("speed", "Speed", "Local horizontal follow velocity in blocks per tick.",
                0.20D, 0.05D, 0.50D));
    }

    /** Calculates one bounded horizontal velocity from local, already-observed target facts. */
    public Optional<HorizontalVelocity> velocity(Context context) {
        Context current = Objects.requireNonNull(context, "context");
        String selectedName = targetName.value().trim();
        if (!isEnabled() || selectedName.isEmpty() || current.screenOpen() || current.passenger()
                || current.abilityFlying() || current.fallFlying()) {
            return Optional.empty();
        }
        Target target = current.targets().stream()
                .filter(candidate -> candidate.name().equalsIgnoreCase(selectedName))
                .filter(candidate -> candidate.horizontalDistance() > stopDistance.value() && candidate.horizontalDistance() <= range.value())
                .min(Comparator.comparingDouble(Target::horizontalDistance))
                .orElse(null);
        if (target == null || target.horizontalDistance() == 0.0D) {
            return Optional.empty();
        }
        double scale = speed.value() / target.horizontalDistance();
        return Optional.of(new HorizontalVelocity(target.deltaX() * scale, target.deltaZ() * scale));
    }
}
