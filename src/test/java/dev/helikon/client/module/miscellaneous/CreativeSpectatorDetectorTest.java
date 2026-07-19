package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreativeSpectatorDetectorTest {
    @Test
    void triggersOncePerContinuousDangerEpisode() {
        CreativeSpectatorDetector detector = enabledDetector();
        var threat = new CreativeSpectatorDetector.Candidate("id", "Operator",
                CreativeSpectatorDetector.Mode.CREATIVE, false, 12.0D);

        assertEquals(threat, detector.observe(List.of(threat)).orElseThrow());
        assertTrue(detector.observe(List.of(threat)).isEmpty());
        assertTrue(detector.observe(List.of()).isEmpty());
        assertEquals(threat, detector.observe(List.of(threat)).orElseThrow());
    }

    @Test
    void excludesFriendsByDefaultAndCanBeConfiguredToIncludeThem() {
        CreativeSpectatorDetector detector = enabledDetector();
        var friend = new CreativeSpectatorDetector.Candidate("friend", "Friend",
                CreativeSpectatorDetector.Mode.SPECTATOR, true, 8.0D);

        assertTrue(detector.observe(List.of(friend)).isEmpty());
        ((BooleanSetting) detector.settings().stream()
                .filter(setting -> setting.id().equals("exclude_friends"))
                .findFirst().orElseThrow()).set(false);
        assertEquals(friend, detector.observe(List.of(friend)).orElseThrow());
    }

    private static CreativeSpectatorDetector enabledDetector() {
        CreativeSpectatorDetector detector = new CreativeSpectatorDetector();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(detector);
        registry.setEnabled(detector, true);
        return detector;
    }
}
