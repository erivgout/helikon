package dev.helikon.client.command;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.world.BaritoneNavigation;

import java.util.List;
import java.util.Objects;

/** Local Helikon command bridge to the embedded Baritone command manager. */
public final class BaritoneCommand implements HelikonCommand {
    private final ModuleRegistry modules;
    private final BaritoneNavigation baritone;

    public BaritoneCommand(ModuleRegistry modules, BaritoneNavigation baritone) {
        this.modules = Objects.requireNonNull(modules, "modules");
        this.baritone = Objects.requireNonNull(baritone, "baritone");
    }

    @Override
    public String name() {
        return "baritone";
    }

    @Override
    public String usage() {
        return ".baritone <status|stop|Baritone command...>";
    }

    @Override
    public String description() {
        return "Controls the embedded Baritone pathfinder; for example .baritone goto 100 64 -30.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.isEmpty() || arguments.getFirst().equalsIgnoreCase("status")) {
            feedback.info("Baritone is " + (baritone.isEnabled() ? "enabled" : "disabled")
                    + " (" + baritone.status() + ").");
            feedback.info("Examples: .baritone goto 100 64 -30, .baritone mine diamond_ore, .baritone stop");
            return;
        }

        String command = String.join(" ", arguments).trim();
        if (command.equalsIgnoreCase("stop") || command.equalsIgnoreCase("cancel")) {
            baritone.cancel();
            feedback.info("Baritone processes canceled.");
            return;
        }

        if (!baritone.isEnabled() && !modules.setEnabled(baritone, true)) {
            feedback.error("Baritone could not be enabled.");
            return;
        }
        if (!baritone.execute(command)) {
            feedback.error("Unknown Baritone command. Try .baritone help.");
        }
    }
}
