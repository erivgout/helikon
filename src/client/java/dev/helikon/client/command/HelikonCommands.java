package dev.helikon.client.command;

import dev.helikon.client.module.ModuleRegistry;

/** Registers the built-in local commands. */
public final class HelikonCommands {
    private HelikonCommands() {
    }

    public static void registerDefaults(
            CommandDispatcher dispatcher,
            ModuleRegistry registry,
            KeyNameResolver keyNames,
            Runnable guiOpener
    ) {
        dispatcher.register(new HelpCommand(dispatcher));
        dispatcher.register(new ModulesCommand(registry));
        dispatcher.register(new ToggleCommand(registry));
        dispatcher.register(new SearchCommand(registry));
        dispatcher.register(new SettingCommand(registry));
        dispatcher.register(new ResetCommand(registry));
        dispatcher.register(new BindCommand(registry, keyNames));
        dispatcher.register(new UnbindCommand(registry));
        dispatcher.register(new GuiCommand(guiOpener));
        dispatcher.register(new PanicCommand(registry));
    }
}
