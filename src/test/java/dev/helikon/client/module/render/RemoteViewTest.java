package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteViewTest {
    @Test
    void defaultsToSafeCrosshairPlayerSelectionAndOff() {
        RemoteView view = new RemoteView();

        assertEquals("remote_view", view.id());
        assertEquals("RemoteView", view.name());
        assertEquals(ModuleCategory.RENDER, view.category());
        assertFalse(view.defaultEnabled());
        assertTrue(view.select(List.of(candidate(2, "minecraft:player", "Target", 10.0D,
                false, false, false)), 2).isPresent());
        assertTrue(view.select(List.of(candidate(2, "minecraft:zombie", "Zombie", 10.0D,
                false, false, false)), 2).isEmpty());
        NumberSetting range = numberSetting(view, "range");
        assertEquals(4.0D, range.minimum(), 1.0e-9D);
        assertEquals(1_024.0D, range.maximum(), 1.0e-9D);
        assertThrows(IllegalArgumentException.class, () -> range.set(1_025.0D));
    }

    @Test
    void nearestModeExcludesFriendsInvisibleEntitiesAndSpectatorsByDefault() {
        RemoteView view = new RemoteView();
        modeSetting(view).set(RemoteView.TargetMode.NEAREST);

        RemoteView.Candidate selected = view.select(List.of(
                candidate(4, "minecraft:player", "Far", 80.0D, false, false, false),
                candidate(1, "minecraft:player", "Friend", 10.0D, true, false, false),
                candidate(2, "minecraft:player", "Invisible", 20.0D, false, true, false),
                candidate(3, "minecraft:player", "Spectator", 30.0D, false, false, true),
                candidate(5, "minecraft:player", "Near", 40.0D, false, false, false)
        ), null).orElseThrow();

        assertEquals("Near", selected.name());
    }

    @Test
    void namedModeMatchesPlayerNamesCaseInsensitively() {
        RemoteView view = new RemoteView();
        modeSetting(view).set(RemoteView.TargetMode.NAMED_PLAYER);
        stringSetting(view, "target_name").set("alice");

        RemoteView.Candidate selected = view.select(List.of(
                candidate(1, "minecraft:player", "Bob", 10.0D, false, false, false),
                candidate(2, "minecraft:player", "Alice", 20.0D, false, false, false)
        ), null).orElseThrow();

        assertEquals(2, selected.entityId());
    }

    @Test
    void settingsChangesReviseSelectionAndDisableRestoresTheCamera() {
        RemoteView view = new RemoteView();
        long revision = view.selectionRevision();
        booleanSetting(view, "include_friends").set(true);
        assertTrue(view.selectionRevision() > revision);

        AtomicBoolean restored = new AtomicBoolean();
        view.setViewRestorer(() -> restored.set(true));
        view.enable();
        view.disable();
        assertTrue(restored.get());
    }

    @SuppressWarnings("unchecked")
    private static EnumSetting<RemoteView.TargetMode> modeSetting(RemoteView view) {
        return (EnumSetting<RemoteView.TargetMode>) view.settings().stream()
                .filter(setting -> setting.id().equals("target_mode"))
                .findFirst()
                .orElseThrow();
    }

    private static StringSetting stringSetting(RemoteView view, String id) {
        return (StringSetting) view.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static BooleanSetting booleanSetting(RemoteView view, String id) {
        return (BooleanSetting) view.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static NumberSetting numberSetting(RemoteView view, String id) {
        return (NumberSetting) view.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static RemoteView.Candidate candidate(
            int id,
            String typeId,
            String name,
            double distance,
            boolean friend,
            boolean invisible,
            boolean spectator
    ) {
        return new RemoteView.Candidate(id, typeId, name, distance * distance,
                friend, invisible, spectator);
    }
}
