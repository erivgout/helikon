package dev.helikon.client;

import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.event.ClientTickEvent;
import dev.helikon.client.event.EventBus;
import dev.helikon.client.input.HelikonKeybinds;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.render.FullbrightStub;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

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

    @Override
    public void onInitializeClient() {
        modules.addFailureHandler((module, operation, exception) ->
                LOGGER.warning(() -> "Disabled module '" + module.id() + "' after a failed " + operation)
        );
        modules.register(new FullbrightStub());

        ConfigurationManager.LoadResult configurationResult = configuration.load(modules);
        if (configurationResult != ConfigurationManager.LoadResult.LOADED) {
            modules.enableDefaultModules();
        }

        HelikonKeybinds.register(modules, configuration);
        ClientTickEvents.START_CLIENT_TICK.register(client -> events.post(new ClientTickEvent(ClientTickEvent.Phase.PRE)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> events.post(new ClientTickEvent(ClientTickEvent.Phase.POST)));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> configuration.save(modules));

        LOGGER.info("Helikon bootstrap initialized with " + modules.all().size() + " module(s)");
    }

    public ModuleRegistry modules() {
        return modules;
    }

    public EventBus events() {
        return events;
    }
}
