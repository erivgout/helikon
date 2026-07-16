package dev.helikon.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.helikon.client.command.ChatCommands;
import dev.helikon.client.command.CommandDispatcher;
import dev.helikon.client.command.HelikonCommands;
import dev.helikon.client.command.MinecraftKeyNameResolver;
import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.event.ClientTickEvent;
import dev.helikon.client.event.EventBus;
import dev.helikon.client.gui.HelikonClickGuiScreen;
import dev.helikon.client.input.HelikonKeybinds;
import dev.helikon.client.input.KeybindManager;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.render.FullbrightStub;
import dev.helikon.client.notification.ChatNotifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Fabric client entrypoint for the Helikon bootstrap. */
public final class HelikonClient implements ClientModInitializer {
    public static final String MOD_ID = "helikon";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    private final ModuleRegistry modules = new ModuleRegistry();
    private final EventBus events = new EventBus((event, exception) ->
            LOGGER.log(Level.SEVERE, "Unhandled listener error for " + event.getClass().getSimpleName(), exception)
    );
    private final ConfigurationManager configuration = new ConfigurationManager(
            FabricLoader.getInstance().getConfigDir().resolve(MOD_ID)
    );
    private final KeybindManager keybinds = new KeybindManager(modules);
    private final CommandDispatcher commands = new CommandDispatcher();
    private final ChatNotifier notifier = new ChatNotifier();

    /** A screen change requested from chat, applied on the next tick once chat has closed. */
    private final AtomicReference<Runnable> pendingScreenAction = new AtomicReference<>();

    @Override
    public void onInitializeClient() {
        modules.addFailureHandler((module, operation, exception) ->
                LOGGER.warning(() -> "Disabled module '" + module.id() + "' after a failed " + operation)
        );
        modules.addFailureHandler((module, operation, exception) ->
                notifier.error("Module '" + module.id() + "' failed to " + operation + " and was disabled (see log).")
        );
        modules.register(new FullbrightStub());

        ConfigurationManager.LoadResult configurationResult = configuration.load(modules);
        if (configurationResult != ConfigurationManager.LoadResult.LOADED) {
            modules.enableDefaultModules();
        }

        HelikonCommands.registerDefaults(commands, modules, new MinecraftKeyNameResolver(), () ->
                pendingScreenAction.set(() -> openClickGui())
        );
        ChatCommands.register(commands, notifier);

        HelikonKeybinds.register(modules, configuration);
        ClientTickEvents.START_CLIENT_TICK.register(client -> events.post(new ClientTickEvent(ClientTickEvent.Phase.PRE)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            events.post(new ClientTickEvent(ClientTickEvent.Phase.POST));

            // Module keybinds count as released while any screen is open so
            // typing into text fields can never trigger them.
            keybinds.tick(
                    key -> InputConstants.isKeyDown(client.getWindow(), key),
                    client.gui.screen() != null
            );

            Runnable screenAction = pendingScreenAction.getAndSet(null);
            if (screenAction != null && client.gui.screen() == null) {
                screenAction.run();
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> configuration.save(modules));

        LOGGER.info("Helikon bootstrap initialized with " + modules.all().size() + " module(s)");
    }

    private void openClickGui() {
        Minecraft.getInstance().setScreenAndShow(new HelikonClickGuiScreen(modules, configuration));
    }

    public ModuleRegistry modules() {
        return modules;
    }

    public EventBus events() {
        return events;
    }
}
