package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

import java.util.List;

/** One-shot local update-notes screen trigger. */
public final class Changelog extends Module {
    private boolean pending;

    public Changelog() {
        super("changelog", "Changelog", "Opens Helikon's bundled update notes.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
    }

    @Override
    protected void onEnable() {
        pending = true;
    }

    @Override
    protected void onDisable() {
        pending = false;
    }

    public boolean consumeOpenRequest() {
        if (!isEnabled() || !pending) {
            return false;
        }
        pending = false;
        return true;
    }

    public List<String> notes() {
        return List.of(
                "Helikon 1.6.0",
                "Radar can now save already-loaded discovered terrain in a persistent local atlas.",
                "Press M to open a full-screen north-up map with pan, zoom, recenter, and saved waypoints.",
                "Map data is isolated by singleplayer world or server and by dimension.",
                "Versioned atomic region files include backups, corruption recovery, bounded caches, and a quota.",
                "The new Update Checker can report newer stable GitHub releases with a local toast and chat link.",
                "Update Checker is opt-in, makes one bounded GitHub request per enabled session, and never installs code.",
                "No discovered terrain, coordinates, waypoints, credentials, or gameplay data leaves the client."
        );
    }
}
