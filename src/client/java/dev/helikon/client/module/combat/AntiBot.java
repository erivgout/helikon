package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Conservative local bot heuristics; the result is never sent to a service or server. */
public final class AntiBot extends Module {
    public record Facts(boolean listedInTab, boolean impossibleState, int spawnAgeTicks, boolean duplicateName,
                        boolean invisible, boolean hasProfile) {
        public Facts {
            if (spawnAgeTicks < 0) {
                throw new IllegalArgumentException("spawnAgeTicks must not be negative");
            }
        }
    }

    private final BooleanSetting tabListPresence;
    private final BooleanSetting impossibleState;
    private final NumberSetting minimumSpawnAge;
    private final BooleanSetting duplicateNames;
    private final BooleanSetting invisible;
    private final BooleanSetting missingProfile;

    public AntiBot() {
        super("anti_bot", "AntiBot", "Locally excludes players matching conservative bot heuristics.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        tabListPresence = addSetting(new BooleanSetting("tab_list_presence", "Tab-list presence",
                "Treat a player absent from the local tab list as suspicious.", true));
        impossibleState = addSetting(new BooleanSetting("impossible_state", "Impossible state",
                "Treat a removed or wrong-world entity state as suspicious.", true));
        minimumSpawnAge = addSetting(new NumberSetting("minimum_spawn_age", "Minimum spawn age",
                "Treat very newly spawned player entities as suspicious.", 4.0D, 0.0D, 200.0D));
        duplicateNames = addSetting(new BooleanSetting("duplicate_names", "Duplicate names",
                "Treat repeated locally observed player names as suspicious.", true));
        invisible = addSetting(new BooleanSetting("invisible", "Invisible",
                "Treat invisible player entities as suspicious.", false));
        missingProfile = addSetting(new BooleanSetting("missing_profile", "Missing profile",
                "Treat a player with no usable local profile name as suspicious.", true));
    }

    public boolean isSuspected(Facts facts) {
        if (facts == null || !isEnabled()) {
            return false;
        }
        return (tabListPresence.value() && !facts.listedInTab())
                || (impossibleState.value() && facts.impossibleState())
                || (facts.spawnAgeTicks() < Math.round(minimumSpawnAge.value()))
                || (duplicateNames.value() && facts.duplicateName())
                || (invisible.value() && facts.invisible())
                || (missingProfile.value() && !facts.hasProfile());
    }
}
