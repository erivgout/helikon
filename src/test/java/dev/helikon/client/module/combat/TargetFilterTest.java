package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.StringListSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetFilterTest {
    @Test
    void disabledFilterNeverRestrictsTargeting() {
        TargetFilter filter = new TargetFilter();
        players(filter).set(List.of("Alice"));
        mode(filter).set(TargetFilter.Mode.ALLOWLIST);

        assertTrue(filter.allowsPlayer("Bob", false));
        assertTrue(filter.allowsPlayer("Alice", true));
    }

    @Test
    void blocklistExcludesListedPlayersCaseInsensitivelyAndKeepsOthers() {
        TargetFilter filter = enabled();
        mode(filter).set(TargetFilter.Mode.BLOCKLIST);
        players(filter).set(List.of("Griefer", "  Spy  "));

        assertFalse(filter.allowsPlayer("griefer", false));
        assertFalse(filter.allowsPlayer("SPY", false));
        assertTrue(filter.allowsPlayer("Stranger", false));
        assertTrue(filter.allowsPlayer(null, false));
    }

    @Test
    void allowlistTargetsOnlyListedPlayersIncludingEmptyMeaningNone() {
        TargetFilter filter = enabled();
        mode(filter).set(TargetFilter.Mode.ALLOWLIST);
        players(filter).set(List.of("Rival"));

        assertTrue(filter.allowsPlayer("rival", false));
        assertFalse(filter.allowsPlayer("Bystander", false));

        players(filter).set(List.of());
        assertFalse(filter.allowsPlayer("Rival", false));
    }

    @Test
    void friendsAreExcludedByDefaultButNotWhenTheSettingIsOff() {
        TargetFilter filter = enabled();
        mode(filter).set(TargetFilter.Mode.BLOCKLIST);

        assertFalse(filter.allowsPlayer("Buddy", true));
        excludeFriends(filter).set(false);
        assertTrue(filter.allowsPlayer("Buddy", true));
    }

    private static TargetFilter enabled() {
        TargetFilter filter = new TargetFilter();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(filter);
        registry.setEnabled(filter, true);
        return filter;
    }

    @SuppressWarnings("unchecked")
    private static EnumSetting<TargetFilter.Mode> mode(TargetFilter filter) {
        return (EnumSetting<TargetFilter.Mode>) setting(filter, "mode");
    }

    private static StringListSetting players(TargetFilter filter) {
        return (StringListSetting) setting(filter, "players");
    }

    private static BooleanSetting excludeFriends(TargetFilter filter) {
        return (BooleanSetting) setting(filter, "exclude_friends");
    }

    private static dev.helikon.client.setting.Setting<?> setting(TargetFilter filter, String id) {
        return filter.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
