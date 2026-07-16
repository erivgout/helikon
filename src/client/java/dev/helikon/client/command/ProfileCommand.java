package dev.helikon.client.command;

import dev.helikon.client.config.ProfileManager;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.module.ModuleRegistry;

import java.util.List;
import java.util.Objects;

/** Manages local configuration snapshots; no profile data is sent anywhere. */
public final class ProfileCommand implements HelikonCommand {
    private final ProfileManager profiles;
    private final ModuleRegistry registry;
    private final ClickGuiWindowState clickGuiWindow;

    public ProfileCommand(ProfileManager profiles, ModuleRegistry registry, ClickGuiWindowState clickGuiWindow) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.clickGuiWindow = Objects.requireNonNull(clickGuiWindow, "clickGuiWindow");
    }

    @Override
    public String name() {
        return "profile";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "profile list|save <name>|load <name>|duplicate <from> <to>|rename <from> <to>|delete <name>";
    }

    @Override
    public String description() {
        return "Lists, saves, loads, duplicates, renames, or deletes local profiles.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.isEmpty()) {
            feedback.error("Usage: " + usage());
            return;
        }
        try {
            switch (arguments.get(0)) {
                case "list" -> list(arguments, feedback);
                case "save" -> save(arguments, feedback);
                case "load" -> load(arguments, feedback);
                case "duplicate" -> duplicate(arguments, feedback);
                case "rename" -> rename(arguments, feedback);
                case "delete" -> delete(arguments, feedback);
                default -> feedback.error("Usage: " + usage());
            }
        } catch (RuntimeException exception) {
            feedback.error("Profile action failed: " + exception.getMessage());
        }
    }

    private void list(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: " + CommandDispatcher.PREFIX + "profile list");
            return;
        }
        List<String> names = profiles.list();
        feedback.info(names.isEmpty() ? "No saved profiles." : "Profiles: " + String.join(", ", names));
    }

    private void save(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) {
            feedback.error("Usage: " + CommandDispatcher.PREFIX + "profile save <name>");
            return;
        }
        profiles.save(arguments.get(1), registry, clickGuiWindow);
        feedback.info("Saved local profile '" + arguments.get(1).trim().toLowerCase(java.util.Locale.ROOT) + "'.");
    }

    private void load(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) {
            feedback.error("Usage: " + CommandDispatcher.PREFIX + "profile load <name>");
            return;
        }
        ProfileManager.LoadResult result = profiles.load(arguments.get(1), registry, clickGuiWindow);
        switch (result) {
            case LOADED -> feedback.info("Loaded local profile '" + arguments.get(1).trim().toLowerCase(java.util.Locale.ROOT) + "'.");
            case MISSING -> feedback.error("No local profile named '" + arguments.get(1) + "'.");
            case UNAVAILABLE -> feedback.error("Profile '" + arguments.get(1) + "' could not be read; it was left unchanged.");
            case RECOVERED_FROM_ERROR -> feedback.error("Profile '" + arguments.get(1) + "' was invalid and was preserved without activation.");
        }
    }

    private void delete(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) {
            feedback.error("Usage: " + CommandDispatcher.PREFIX + "profile delete <name>");
            return;
        }
        if (profiles.delete(arguments.get(1))) {
            feedback.info("Deleted local profile '" + arguments.get(1).trim().toLowerCase(java.util.Locale.ROOT) + "'.");
        } else {
            feedback.error("No local profile named '" + arguments.get(1) + "'.");
        }
    }

    private void duplicate(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 3) {
            feedback.error("Usage: " + CommandDispatcher.PREFIX + "profile duplicate <from> <to>");
            return;
        }
        if (profiles.duplicate(arguments.get(1), arguments.get(2))) {
            feedback.info("Duplicated local profile '" + arguments.get(1) + "' as '" + arguments.get(2) + "'.");
        } else {
            feedback.error("No local profile named '" + arguments.get(1) + "'.");
        }
    }

    private void rename(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 3) {
            feedback.error("Usage: " + CommandDispatcher.PREFIX + "profile rename <from> <to>");
            return;
        }
        if (profiles.rename(arguments.get(1), arguments.get(2))) {
            feedback.info("Renamed local profile '" + arguments.get(1) + "' to '" + arguments.get(2) + "'.");
        } else {
            feedback.error("No local profile named '" + arguments.get(1) + "'.");
        }
    }
}
