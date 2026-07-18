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
                "Helikon 1.1.1",
                "Fixed the Minecraft 26.2 camera mixin startup crash.",
                "Completed the permitted legacy issue backlog.",
                "Added Enderman Aura and bounded anime/JJK combat.",
                "Added interface, chat, world, player, and render utilities.",
                "All automation is off by default; servers remain authoritative."
        );
    }
}
