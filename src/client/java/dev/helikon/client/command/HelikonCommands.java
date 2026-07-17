package dev.helikon.client.command;

import dev.helikon.client.config.ProfileManager;
import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.macro.MacroManager;
import dev.helikon.client.macro.MacroRunner;
import dev.helikon.client.macro.MacroServerContextProvider;
import dev.helikon.client.module.chat.PrivateMessageHelper;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.config.PanicConfigurationManager;
import dev.helikon.client.input.PanicKeybindManager;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.panic.PanicController;
import dev.helikon.client.privatechat.PrivateMessageHistory;
import dev.helikon.client.waypoint.WaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointManager;

import java.util.function.Predicate;

/** Registers the built-in local commands. */
public final class HelikonCommands {
    private HelikonCommands() {
    }

    /** Registers the full command set, including local profile management. */
    public static void registerDefaults(
            CommandDispatcher dispatcher,
            ModuleRegistry registry,
            KeyNameResolver keyNames,
            Predicate<Keybind> reservedKeybinds,
            Runnable guiOpener,
            ProfileManager profiles,
            ClickGuiWindowState clickGuiWindow,
            FriendManager friends,
            WaypointManager waypoints,
            WaypointLocationProvider waypointLocations,
            MacroManager macros,
            MacroRunner macroRunner,
            MacroServerContextProvider macroServerContext,
            PanicController panic,
            PanicKeybindManager panicKeybinds,
            PanicConfigurationManager panicConfiguration,
            PrivateMessageHelper privateMessageHelper,
            PrivateMessageHistory privateMessageHistory,
            ServerCommandSender serverCommandSender
    ) {
        dispatcher.register(new HelpCommand(dispatcher));
        dispatcher.register(new ModulesCommand(registry));
        dispatcher.register(new ToggleCommand(registry));
        dispatcher.register(new SearchCommand(registry));
        dispatcher.register(new SettingCommand(registry));
        dispatcher.register(new ResetCommand(registry));
        dispatcher.register(new BindCommand(registry, keyNames, reservedKeybinds));
        dispatcher.register(new UnbindCommand(registry));
        dispatcher.register(new GuiCommand(guiOpener));
        dispatcher.register(new PanicCommand(panic, panicKeybinds, panicConfiguration, keyNames, reservedKeybinds));
        dispatcher.register(new ProfileCommand(profiles, registry, clickGuiWindow));
        dispatcher.register(new FriendCommand(friends));
        dispatcher.register(new WaypointCommand(waypoints, waypointLocations));
        dispatcher.register(new MacroCommand(macros, macroRunner, macroServerContext));
        dispatcher.register(new PrivateMessageCommand(false, privateMessageHelper, privateMessageHistory, serverCommandSender));
        dispatcher.register(new PrivateMessageCommand(true, privateMessageHelper, privateMessageHistory, serverCommandSender));
    }
}
