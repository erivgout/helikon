package dev.helikon.client.command;

import java.util.List;
import java.util.Objects;

/** Opens the ClickGUI through a caller-supplied action. */
public final class GuiCommand implements HelikonCommand {
    private final Runnable guiOpener;

    public GuiCommand(Runnable guiOpener) {
        this.guiOpener = Objects.requireNonNull(guiOpener, "guiOpener");
    }

    @Override
    public String name() {
        return "gui";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "gui";
    }

    @Override
    public String description() {
        return "Opens the ClickGUI.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        guiOpener.run();
    }
}
