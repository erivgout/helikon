package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Safety switch that detects nearby creative/spectator players and requests a one-shot utility shutdown. */
public final class CreativeSpectatorDetector extends Module {
    public enum Mode {
        CREATIVE,
        SPECTATOR
    }

    public record Candidate(String id, String name, Mode mode, boolean friend, double distance) {
        public Candidate {
            if (id == null || id.isBlank() || name == null || name.isBlank() || mode == null
                    || !Double.isFinite(distance) || distance < 0.0D) {
                throw new IllegalArgumentException("creative/spectator candidate is invalid");
            }
        }
    }

    private final NumberSetting range;
    private final BooleanSetting excludeFriends;
    private boolean dangerLatched;

    public CreativeSpectatorDetector() {
        super("creative_spectator_detector", "Creative/Spectator Detector",
                "Disables every other active utility when a nearby creative or spectator player is detected.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        range = addSetting(new NumberSetting("range", "Detection range",
                "Maximum distance in blocks for creative or spectator player detection.", 64.0D, 4.0D, 256.0D));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Ignore creative or spectator players on the local friend list.", true));
    }

    /** Returns a threat only on the first tick of each continuous nearby-danger episode. */
    public Optional<Candidate> observe(List<Candidate> candidates) {
        if (!isEnabled()) {
            dangerLatched = false;
            return Optional.empty();
        }
        Candidate nearest = candidates == null ? null : candidates.stream()
                .filter(candidate -> candidate.distance() <= range.value())
                .filter(candidate -> !excludeFriends.value() || !candidate.friend())
                .min(Comparator.comparingDouble(Candidate::distance))
                .orElse(null);
        if (nearest == null) {
            dangerLatched = false;
            return Optional.empty();
        }
        if (dangerLatched) {
            return Optional.empty();
        }
        dangerLatched = true;
        return Optional.of(nearest);
    }

    @Override
    protected void onDisable() {
        dangerLatched = false;
    }
}
