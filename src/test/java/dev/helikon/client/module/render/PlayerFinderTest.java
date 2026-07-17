package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberRange;
import dev.helikon.client.setting.RangeSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerFinderTest {
    @Test
    void defaultsAreDistantBoundedAndOff() {
        PlayerFinder finder = new PlayerFinder();

        assertEquals("player_finder", finder.id());
        assertEquals("PlayerFinder", finder.name());
        assertEquals(ModuleCategory.RENDER, finder.category());
        assertFalse(finder.defaultEnabled());
        assertEquals(new NumberRange(64.0D, 1_024.0D), rangeSetting(finder).value());
        assertEquals(16, integerSetting(finder, "maximum_players").value());
    }

    @Test
    void filtersFriendsSpectatorsAndDistanceThenOrdersNearestFirst() {
        PlayerFinder finder = new PlayerFinder();
        List<PlayerFinder.Candidate> selected = finder.select(List.of(
                candidate(4, "Far", 150.0D, false, false),
                candidate(2, "Near", 70.0D, false, false),
                candidate(3, "Friend", 80.0D, true, false),
                candidate(1, "Spectator", 75.0D, false, true),
                candidate(5, "TooNear", 20.0D, false, false),
                candidate(6, "TooFar", 1_100.0D, false, false)));

        assertEquals(List.of("Near", "Far"), selected.stream().map(PlayerFinder.Candidate::name).toList());
        assertEquals("Near 70m", finder.label(selected.getFirst()));
    }

    @Test
    void honorsInclusiveConfiguredRangeAndResultCap() {
        PlayerFinder finder = new PlayerFinder();
        rangeSetting(finder).set(new NumberRange(10.0D, 20.0D));
        integerSetting(finder, "maximum_players").set(1);

        List<PlayerFinder.Candidate> selected = finder.select(List.of(
                candidate(2, "Upper", 20.0D, false, false),
                candidate(1, "Lower", 10.0D, false, false),
                candidate(3, "Middle", 15.0D, false, false)));

        assertEquals(List.of("Lower"), selected.stream().map(PlayerFinder.Candidate::name).toList());
        assertThrows(IllegalArgumentException.class,
                () -> rangeSetting(finder).set(new NumberRange(0.0D, 4_097.0D)));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerFinder.Candidate(1, "", 1.0D, false, false));
    }

    @Test
    void canOptIntoFriendsAndSpectators() {
        PlayerFinder finder = new PlayerFinder();
        booleanSetting(finder, "include_friends").set(true);
        booleanSetting(finder, "include_spectators").set(true);

        List<PlayerFinder.Candidate> selected = finder.select(List.of(
                candidate(2, "Friend", 80.0D, true, false),
                candidate(1, "Spectator", 70.0D, false, true)));

        assertEquals(List.of("Spectator", "Friend"),
                selected.stream().map(PlayerFinder.Candidate::name).toList());
    }

    private static PlayerFinder.Candidate candidate(
            int id,
            String name,
            double distance,
            boolean friend,
            boolean spectator
    ) {
        return new PlayerFinder.Candidate(id, name, distance * distance, friend, spectator);
    }

    private static RangeSetting rangeSetting(PlayerFinder finder) {
        return (RangeSetting) finder.settings().stream()
                .filter(setting -> setting.id().equals("distance"))
                .findFirst()
                .orElseThrow();
    }

    private static IntegerSetting integerSetting(PlayerFinder finder, String id) {
        return (IntegerSetting) finder.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static BooleanSetting booleanSetting(PlayerFinder finder, String id) {
        return (BooleanSetting) finder.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
