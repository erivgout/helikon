package dev.helikon.client.command;

import dev.helikon.client.config.PanicConfigurationManager;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.input.PanicKeybindManager;
import dev.helikon.client.panic.PanicController;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Disables every enabled module through the registry so each module's
 * {@code onDisable} cleanup runs. Configuration is preserved.
 */
public final class PanicCommand implements HelikonCommand {
    private final PanicController controller;
    private final PanicKeybindManager keybinds;
    private final PanicConfigurationManager configuration;
    private final KeyNameResolver keyNames;
    private final Predicate<Keybind> reservedKeybinds;

    public PanicCommand(
            PanicController controller,
            PanicKeybindManager keybinds,
            PanicConfigurationManager configuration,
            KeyNameResolver keyNames,
            Predicate<Keybind> reservedKeybinds
    ) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.keybinds = Objects.requireNonNull(keybinds, "keybinds");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.keyNames = Objects.requireNonNull(keyNames, "keyNames");
        this.reservedKeybinds = Objects.requireNonNull(reservedKeybinds, "reservedKeybinds");
    }

    @Override
    public String name() {
        return "panic";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "panic [bind <key>|unbind|status|restorehud]";
    }

    @Override
    public String description() {
        return "Disables modules, hides custom HUD, and manages the local panic key.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.isEmpty()) {
            activate(feedback);
            return;
        }
        try {
            switch (arguments.getFirst()) {
                case "bind" -> bind(arguments, feedback);
                case "unbind" -> unbind(arguments, feedback);
                case "status" -> status(arguments, feedback);
                case "restorehud" -> restoreHud(arguments, feedback);
                default -> feedback.error("Usage: " + usage());
            }
        } catch (RuntimeException exception) {
            feedback.error("Panic action failed: " + exception.getMessage());
        }
    }

    private void activate(CommandFeedback feedback) {
        PanicController.Result result = controller.activate();
        feedback.info("Panic: disabled " + result.disabledModules()
                + " module(s), hid custom HUD, and preserved configuration.");
    }

    private void bind(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) {
            feedback.error("Usage: .panic bind <key|modifier+key|modifier+mouseN>");
            return;
        }
        KeybindArgument input = KeybindArgument.parse(arguments.get(1), keyNames).orElse(null);
        if (input == null) {
            feedback.error("Unknown key '" + arguments.get(1) + "'.");
            return;
        }
        Keybind keybind = new Keybind(input.inputType(), input.code(), input.modifiers(), Keybind.Activation.TOGGLE);
        if (reservedKeybinds.test(keybind)) {
            feedback.error("That key opens the Helikon GUI and is reserved.");
            return;
        }
        configuration.setKeybindAndSave(keybinds, keybind);
        feedback.info("Bound local panic key to '" + arguments.get(1) + "'.");
    }

    private void unbind(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: .panic unbind");
            return;
        }
        configuration.setKeybindAndSave(keybinds, Keybind.unbound());
        feedback.info("Cleared local panic key.");
    }

    private void status(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: .panic status");
            return;
        }
        Keybind keybind = keybinds.keybind();
        feedback.info(keybind.isBound() ? "Local panic key code: " + keybind.keyCode() + "."
                : "Local panic key is unbound.");
    }

    private void restoreHud(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: .panic restorehud");
            return;
        }
        controller.restoreCustomHud();
        feedback.info("Restored custom HUD visibility without re-enabling modules.");
    }
}
