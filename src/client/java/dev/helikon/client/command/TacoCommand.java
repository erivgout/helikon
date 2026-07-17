package dev.helikon.client.command;

import java.util.List;

/** A deliberately local, harmless legacy easter egg. */
public final class TacoCommand implements HelikonCommand {
    @Override
    public String name() {
        return "taco";
    }

    @Override
    public String usage() {
        return ".taco";
    }

    @Override
    public String description() {
        return "Shows a taco in local chat.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (!arguments.isEmpty()) {
            feedback.error("Usage: " + usage());
            return;
        }
        feedback.info("  /\\_/\\\\");
        feedback.info(" (  taco  )");
        feedback.info("  \\\\_____/");
    }
}
