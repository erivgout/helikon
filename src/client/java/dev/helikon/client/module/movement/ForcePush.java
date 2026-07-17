package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Selects a nearby non-friend entity and produces bounded movement into its collision box. */
public final class ForcePush extends Module {
    private final NumberSetting range;
    private final NumberSetting speed;
    private final BooleanSetting requireAttack;
    private final BooleanSetting excludeFriends;

    public ForcePush() {
        super("force_push", "ForcePush", "Moves into a nearby entity to attempt ordinary collision pushing.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        range = addSetting(new NumberSetting("range", "Range", "Maximum target distance.", 3.0D, 1.0D, 6.0D));
        speed = addSetting(new NumberSetting("speed", "Speed", "Horizontal approach velocity.", 0.18D, 0.03D, 0.5D));
        requireAttack = addSetting(new BooleanSetting("require_attack", "Require Attack",
                "Only push while the physical Attack key is held.", true));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never push locally saved friends.", true));
    }

    public Optional<Motion> motion(boolean attackHeld, List<Candidate> candidates) {
        if (!isEnabled() || (requireAttack.value() && !attackHeld) || candidates == null) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(candidate -> !excludeFriends.value() || !candidate.friend())
                .filter(candidate -> candidate.distance() > 0.0D && candidate.distance() <= range.value())
                .min(Comparator.comparingDouble(Candidate::distance))
                .map(candidate -> new Motion(candidate.id(), candidate.deltaX() / candidate.distance() * speed.value(),
                        candidate.deltaZ() / candidate.distance() * speed.value()));
    }

    public record Candidate(int id, boolean friend, double distance, double deltaX, double deltaZ) {
    }

    public record Motion(int entityId, double x, double z) {
    }
}
