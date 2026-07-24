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
                "Helikon 1.6.1",
                "The full-screen map now renders discovered regions correctly at every zoom level.",
                "Loaded terrain inside render distance now backfills the persistent map while you stand still.",
                "Map capture runs four chunks per tick and periodically re-seeds chunks the queue had to drop.",
                "Full-screen map waypoints use clearer pixel-art pins with a distinct death marker."
        );
    }
}
