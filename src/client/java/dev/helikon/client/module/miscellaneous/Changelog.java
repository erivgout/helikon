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
                "Helikon 1.1.4",
                "Speed now follows held WASD direction in midair, including with Jetpack and Air Jump.",
                "AutoFarm now supports wart, berries, cocoa, stacked plants, melons, and pumpkins.",
                "Infinity now repels nearby threats with ordinary client-side attacks.",
                "Enderman Aura uses gravity-aware arrow prediction and wider side escapes.",
                "Air Jump now supports bounded repeat jumps while Space is held.",
                "TP-Aura now orbits targets and returns only when explicitly configured.",
                "Zoom now applies its configured camera FOV and guards zero-FOV frames.",
                "Fixed Nuker All Blocks selection; held Attack remains the default.",
                "Fixed detached Freecam horizontal look and mouse rotation shaking.",
                "Clipped empty ClickGUI settings text inside its panel.",
                "TpClick now coordinates its fall reset with NoFall before teleporting.",
                "Raised Timer to 5x and made FastBreak accelerate active mining.",
                "Added the missing bounded Regen fast-heal attempt.",
                "KillAura now attacks without moving your camera or head.",
                "Fixed selected ClickGUI labels and favorite controls blending into their rows.",
                "Extended Reach to 9 blocks for attacks, mining, and placement.",
                "Added a BetterCrosshair frame toggle; the square border now defaults off.",
                "Fixed the Minecraft 26.2 camera mixin startup crash.",
                "Completed the permitted legacy issue backlog.",
                "Added Enderman Aura and bounded anime/JJK combat.",
                "Added interface, chat, world, player, and render utilities.",
                "All automation is off by default; servers remain authoritative."
        );
    }
}
