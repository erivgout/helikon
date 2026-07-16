package dev.helikon.client.command;

import dev.helikon.client.macro.Macro;
import dev.helikon.client.macro.MacroAction;
import dev.helikon.client.macro.MacroActionType;
import dev.helikon.client.macro.MacroManager;
import dev.helikon.client.macro.MacroRunner;
import dev.helikon.client.macro.MacroServerContextProvider;

import java.util.List;
import java.util.Objects;

/** Configures and starts only explicit local/chat/command macro actions. */
public final class MacroCommand implements HelikonCommand {
    private final MacroManager macros;
    private final MacroRunner runner;
    private final MacroServerContextProvider serverContext;

    public MacroCommand(MacroManager macros, MacroRunner runner, MacroServerContextProvider serverContext) {
        this.macros = Objects.requireNonNull(macros, "macros");
        this.runner = Objects.requireNonNull(runner, "runner");
        this.serverContext = Objects.requireNonNull(serverContext, "serverContext");
    }

    @Override
    public String name() {
        return "macro";
    }

    @Override
    public String usage() {
        return ".macro list|create <name> [global|server]|delete <name>|add <name> <local|chat|command|delay> <text|ticks>|show <name>|clear <name>|scope <name> <global|server>|run <name>|stop";
    }

    @Override
    public String description() {
        return "Manages explicit local, chat, command, and delay macros stored only on this client.";
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
                case "create" -> create(arguments, feedback);
                case "delete" -> delete(arguments, feedback);
                case "add" -> add(arguments, feedback);
                case "show" -> show(arguments, feedback);
                case "clear" -> clear(arguments, feedback);
                case "scope" -> scope(arguments, feedback);
                case "run" -> run(arguments, feedback);
                case "stop" -> stop(arguments, feedback);
                default -> feedback.error("Usage: " + usage());
            }
        } catch (RuntimeException exception) {
            feedback.error("Macro action failed: " + exception.getMessage());
        }
    }

    private void list(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: .macro list");
            return;
        }
        List<Macro> listed = macros.list();
        if (listed.isEmpty()) {
            feedback.info("No local macros.");
            return;
        }
        feedback.info("Macros: " + listed.stream().map(this::summary)
                .collect(java.util.stream.Collectors.joining(", ")));
    }

    private void create(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2 && arguments.size() != 3) {
            feedback.error("Usage: .macro create <name> [global|server]");
            return;
        }
        String server = scopeAddress(arguments.size() == 3 ? arguments.get(2) : "global");
        if (!macros.createAndSave(arguments.get(1), server)) {
            feedback.error("A local macro named '" + arguments.get(1) + "' already exists.");
            return;
        }
        feedback.info("Created " + (server.isEmpty() ? "global" : "server-scoped")
                + " local macro '" + Macro.normalizeName(arguments.get(1)) + "'.");
    }

    private void delete(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) {
            feedback.error("Usage: .macro delete <name>");
            return;
        }
        if (!macros.removeAndSave(arguments.get(1))) {
            feedback.error("No local macro named '" + arguments.get(1) + "'.");
            return;
        }
        feedback.info("Deleted local macro '" + arguments.get(1) + "'.");
    }

    private void add(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() < 4) {
            feedback.error("Usage: .macro add <name> <local|chat|command|delay> <text|ticks>");
            return;
        }
        MacroActionType type = MacroActionType.parse(arguments.get(2));
        MacroAction action;
        if (type == MacroActionType.DELAY) {
            if (arguments.size() != 4) {
                feedback.error("Usage: .macro add <name> delay <ticks>");
                return;
            }
            action = MacroAction.delay(parseTicks(arguments.get(3)));
        } else {
            action = switch (type) {
                case LOCAL -> MacroAction.local(join(arguments, 3));
                case CHAT -> MacroAction.chat(join(arguments, 3));
                case COMMAND -> MacroAction.command(join(arguments, 3));
                case DELAY -> throw new IllegalStateException("Handled above");
            };
        }
        if (!macros.addActionAndSave(arguments.get(1), action)) {
            feedback.error("No local macro named '" + arguments.get(1) + "'.");
            return;
        }
        feedback.info("Added " + action.type().name().toLowerCase(java.util.Locale.ROOT)
                + " action to local macro '" + arguments.get(1) + "'.");
    }

    private void show(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) {
            feedback.error("Usage: .macro show <name>");
            return;
        }
        Macro macro = macros.find(arguments.get(1)).orElse(null);
        if (macro == null) {
            feedback.error("No local macro named '" + arguments.get(1) + "'.");
            return;
        }
        feedback.info("Macro '" + macro.name() + "' (" + (macro.isServerScoped() ? macro.serverAddress() : "global") + "): ");
        if (macro.actions().isEmpty()) {
            feedback.info("  No actions.");
            return;
        }
        for (int index = 0; index < macro.actions().size(); index++) {
            MacroAction action = macro.actions().get(index);
            String detail = action.type() == MacroActionType.DELAY ? action.delayTicks() + " ticks" : action.text();
            feedback.info("  " + (index + 1) + ". " + action.type().name().toLowerCase(java.util.Locale.ROOT) + " " + detail);
        }
    }

    private void clear(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) {
            feedback.error("Usage: .macro clear <name>");
            return;
        }
        if (!macros.clearActionsAndSave(arguments.get(1))) {
            feedback.error("No local macro named '" + arguments.get(1) + "'.");
            return;
        }
        feedback.info("Cleared local macro '" + arguments.get(1) + "'.");
    }

    private void scope(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 3) {
            feedback.error("Usage: .macro scope <name> <global|server>");
            return;
        }
        String server = scopeAddress(arguments.get(2));
        if (!macros.setServerAddressAndSave(arguments.get(1), server)) {
            feedback.error("No local macro named '" + arguments.get(1) + "'.");
            return;
        }
        feedback.info("Set local macro '" + arguments.get(1) + "' scope to "
                + (server.isEmpty() ? "global" : "server '" + server + "'") + ".");
    }

    private void run(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 2) {
            feedback.error("Usage: .macro run <name>");
            return;
        }
        Macro macro = macros.find(arguments.get(1)).orElse(null);
        if (macro == null) {
            feedback.error("No local macro named '" + arguments.get(1) + "'.");
            return;
        }
        runner.start(macro, serverContext.currentServerAddress());
        feedback.info("Started local macro '" + macro.name() + "'.");
    }

    private void stop(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 1) {
            feedback.error("Usage: .macro stop");
            return;
        }
        runner.cancel().ifPresentOrElse(
                name -> feedback.info("Stopped local macro '" + name + "'."),
                () -> feedback.info("No local macro is running.")
        );
    }

    private String scopeAddress(String scope) {
        return switch (scope.toLowerCase(java.util.Locale.ROOT)) {
            case "global" -> Macro.GLOBAL;
            case "server" -> serverContext.currentServerAddress()
                    .orElseThrow(() -> new IllegalArgumentException("Server-scoped macros require an active multiplayer server"));
            default -> throw new IllegalArgumentException("Macro scope must be global or server");
        };
    }

    private static String join(List<String> arguments, int first) {
        return String.join(" ", arguments.subList(first, arguments.size()));
    }

    private static int parseTicks(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Macro delay must be a whole number of ticks");
        }
    }

    private String summary(Macro macro) {
        return macro.name() + " [" + (macro.isServerScoped() ? macro.serverAddress() : "global")
                + ", " + macro.actions().size() + " actions]";
    }
}
