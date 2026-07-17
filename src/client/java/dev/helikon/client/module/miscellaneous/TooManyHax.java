package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Deterministically prevents known incompatible module combinations. */
public final class TooManyHax extends Module {
    private static final List<Set<String>> GROUPS = List.of(
            Set.of("flight", "boat_flight", "jetpack", "freecam", "no_clip", "phase"),
            Set.of("kill_aura", "silent_aura", "click_aura", "fight_bot", "tp_aura", "anime_aura"),
            Set.of("blink", "fake_lag", "back_track")
    );

    public TooManyHax() {
        super("too_many_hax", "TooManyHax", "Disables later conflicting modules in known compatibility groups.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
    }

    public List<String> conflicts(Collection<String> enabledIds) {
        if (!isEnabled()) {
            return List.of();
        }
        Set<String> enabled = new LinkedHashSet<>(enabledIds);
        List<String> losers = new ArrayList<>();
        for (Set<String> group : GROUPS) {
            boolean keptOne = false;
            for (String id : enabled) {
                if (!group.contains(id)) {
                    continue;
                }
                if (!keptOne) {
                    keptOne = true;
                } else {
                    losers.add(id);
                }
            }
        }
        return List.copyOf(losers);
    }
}
