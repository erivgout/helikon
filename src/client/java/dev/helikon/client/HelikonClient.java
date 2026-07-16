package dev.helikon.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.helikon.client.command.ChatCommands;
import dev.helikon.client.command.CommandDispatcher;
import dev.helikon.client.command.HelikonCommands;
import dev.helikon.client.command.MinecraftKeyNameResolver;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.config.HudConfigurationManager;
import dev.helikon.client.config.PanicConfigurationManager;
import dev.helikon.client.config.ProfileManager;
import dev.helikon.client.event.ClientTickEvent;
import dev.helikon.client.event.EventBus;
import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.friend.FriendToggleGesture;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.gui.HelikonClickGuiScreen;
import dev.helikon.client.gui.HelikonHudEditorScreen;
import dev.helikon.client.gui.HelikonThemeEditorScreen;
import dev.helikon.client.hud.ActiveModulesHud;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.hud.WaypointHud;
import dev.helikon.client.input.HelikonKeybinds;
import dev.helikon.client.input.KeybindManager;
import dev.helikon.client.input.PanicKeybindManager;
import dev.helikon.client.macro.MacroActionExecutor;
import dev.helikon.client.macro.MacroManager;
import dev.helikon.client.macro.MacroRunner;
import dev.helikon.client.macro.MacroServerContextProvider;
import dev.helikon.client.macro.MinecraftMacroActionExecutor;
import dev.helikon.client.macro.MinecraftMacroServerContextProvider;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.render.Fullbright;
import dev.helikon.client.module.render.MinecraftGammaAccess;
import dev.helikon.client.module.render.MinecraftNightVisionAccess;
import dev.helikon.client.notification.ChatNotifier;
import dev.helikon.client.panic.PanicController;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.waypoint.MinecraftWaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

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
    private final ClickGuiWindowState clickGuiWindow = new ClickGuiWindowState();
    private final ProfileManager profiles = new ProfileManager(configuration);
    private final FriendManager friends = new FriendManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID));
    private final FriendToggleGesture friendToggleGesture = new FriendToggleGesture();
    private final PanicState panicState = new PanicState();
    private final PanicKeybindManager panicKeybinds = new PanicKeybindManager();
    private final PanicConfigurationManager panicConfiguration = new PanicConfigurationManager(
            FabricLoader.getInstance().getConfigDir().resolve(MOD_ID), HelikonKeybinds::isGuiKey
    );
    private final WaypointManager waypoints = new WaypointManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID));
    private final WaypointLocationProvider waypointLocations = new MinecraftWaypointLocationProvider();
    private final MacroManager macros = new MacroManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID));
    private final MacroRunner macroRunner = new MacroRunner();
    private final MacroServerContextProvider macroServerContext = new MinecraftMacroServerContextProvider();
    private final PanicController panic = new PanicController(
            modules, panicState, this::closeHelikonScreen, () -> macroRunner.cancel()
    );
    private final HudLayout hudLayout = new HudLayout();
    private final HudConfigurationManager hudConfiguration = new HudConfigurationManager(
            FabricLoader.getInstance().getConfigDir().resolve(MOD_ID)
    );
    private final KeybindManager keybinds = new KeybindManager(modules);
    private final CommandDispatcher commands = new CommandDispatcher();
    private final ChatNotifier notifier = new ChatNotifier();
    private final MacroActionExecutor macroExecutor = new MinecraftMacroActionExecutor(commands, notifier);

    /** A screen change requested from chat, applied on the next tick once chat has closed. */
    private final AtomicReference<Runnable> pendingScreenAction = new AtomicReference<>();
    private boolean screenWasOpenAtTickStart;
    private boolean helikonScreenWasOpenAtTickStart;

    @Override
    public void onInitializeClient() {
        modules.addFailureHandler((module, operation, exception) ->
                LOGGER.warning(() -> "Disabled module '" + module.id() + "' after a failed " + operation)
        );
        modules.addFailureHandler((module, operation, exception) ->
                notifier.error("Module '" + module.id() + "' failed to " + operation + " and was disabled (see log).")
        );
        Fullbright fullbright = new Fullbright(new MinecraftGammaAccess(), new MinecraftNightVisionAccess());
        modules.register(fullbright);
        events.subscribe(ClientTickEvent.class, event -> {
            if (event.phase() == ClientTickEvent.Phase.POST) {
                modules.runGuarded(fullbright, "tick", fullbright::tick);
            }
        });

        ConfigurationManager.LoadResult configurationResult = configuration.load(modules, clickGuiWindow);
        if (configurationResult != ConfigurationManager.LoadResult.LOADED) {
            modules.enableDefaultModules();
        }
        hudConfiguration.load(hudLayout);
        try {
            friends.load();
        } catch (ConfigurationException exception) {
            LOGGER.log(Level.WARNING, "Unable to load friends; continuing with an empty local friend list", exception);
        }
        try {
            waypoints.load();
        } catch (ConfigurationException exception) {
            LOGGER.log(Level.WARNING, "Unable to load waypoints; continuing with an empty local waypoint list", exception);
        }
        try {
            macros.load();
        } catch (ConfigurationException exception) {
            LOGGER.log(Level.WARNING, "Unable to load macros; continuing with an empty local macro list", exception);
        }
        try {
            panicConfiguration.load(panicKeybinds);
        } catch (ConfigurationException exception) {
            LOGGER.log(Level.WARNING, "Unable to load panic keybind; keeping it unbound", exception);
        }

        HelikonCommands.registerDefaults(commands, modules, new MinecraftKeyNameResolver(),
                HelikonKeybinds::isGuiKey, () -> pendingScreenAction.set(this::openClickGui), profiles, clickGuiWindow,
                friends, waypoints, waypointLocations, macros, macroRunner, macroServerContext,
                panic, panicKeybinds, panicConfiguration);
        ChatCommands.register(commands, notifier);

        HelikonKeybinds.register(modules, configuration, clickGuiWindow, hudLayout, hudConfiguration);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "active_modules"),
                new ActiveModulesHud(modules, hudLayout, panicState));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "waypoints"),
                new WaypointHud(waypoints, waypointLocations, panicState));
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            screenWasOpenAtTickStart = client.gui.screen() != null;
            helikonScreenWasOpenAtTickStart = isHelikonScreen(client);
            events.post(new ClientTickEvent(ClientTickEvent.Phase.PRE));
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            events.post(new ClientTickEvent(ClientTickEvent.Phase.POST));

            boolean anyScreenOpen = screenWasOpenAtTickStart || client.gui.screen() != null;
            boolean panicTriggered = panicKeybinds.tick(
                    key -> InputConstants.isKeyDown(client.getWindow(), key),
                    anyScreenOpen,
                    helikonScreenWasOpenAtTickStart || isHelikonScreen(client),
                    this::activatePanic
            );

            // Module keybinds count as released while any screen is open so
            // typing into text fields can never trigger them. A panic press
            // also suppresses this tick so a shared key cannot re-enable one.
            keybinds.tick(
                    key -> InputConstants.isKeyDown(client.getWindow(), key),
                    anyScreenOpen || panicTriggered
            );
            toggleFriendOnMiddleClick(client);
            tickMacro(client);

            Runnable screenAction = pendingScreenAction.getAndSet(null);
            if (screenAction != null && client.gui.screen() == null) {
                screenAction.run();
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> saveConfigurations());

        LOGGER.info("Helikon bootstrap initialized with " + modules.all().size() + " module(s)");
    }

    private void openClickGui() {
        Minecraft.getInstance().setScreenAndShow(new HelikonClickGuiScreen(
                modules, configuration, clickGuiWindow, hudLayout, hudConfiguration
        ));
    }

    private void activatePanic() {
        PanicController.Result result = panic.activate();
        notifier.info("Panic: disabled " + result.disabledModules()
                + " module(s), hid custom HUD, and preserved configuration.");
    }

    private void closeHelikonScreen() {
        Minecraft client = Minecraft.getInstance();
        if (isHelikonScreen(client)) {
            client.gui.setScreen(null);
        }
    }

    private static boolean isHelikonScreen(Minecraft client) {
        return client.gui.screen() instanceof HelikonClickGuiScreen
                || client.gui.screen() instanceof HelikonHudEditorScreen
                || client.gui.screen() instanceof HelikonThemeEditorScreen;
    }

    private void toggleFriendOnMiddleClick(Minecraft client) {
        String targetedPlayerName = null;
        if (client.hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof Player player) {
            targetedPlayerName = player.getGameProfile().name();
        }

        friendToggleGesture.update(
                client.mouseHandler.isMiddlePressed(),
                client.gui.screen() != null,
                targetedPlayerName
        ).ifPresent(this::toggleFriend);
    }

    private void toggleFriend(String playerName) {
        try {
            boolean isFriend = friends.toggle(playerName);
            friends.save();
            notifier.info((isFriend ? "Added local friend '" : "Removed local friend '") + playerName + "'.");
        } catch (ConfigurationException | IllegalArgumentException exception) {
            LOGGER.log(Level.WARNING, "Unable to toggle local friend '" + playerName + "'", exception);
            notifier.error("Unable to toggle local friend '" + playerName + "' (see log).");
        }
    }

    private void tickMacro(Minecraft client) {
        MacroRunner.TickResult contextResult = macroRunner.validateServerContext(macroServerContext.currentServerAddress());
        if (contextResult.status() == MacroRunner.TickStatus.CANCELLED_CONTEXT) {
            reportMacroResult(contextResult);
            return;
        }
        if (client.gui.screen() != null) {
            return;
        }
        reportMacroResult(macroRunner.tick(macroServerContext.currentServerAddress(), macroExecutor));
    }

    private void reportMacroResult(MacroRunner.TickResult result) {
        switch (result.status()) {
            case COMPLETED -> notifier.info("Completed local macro '" + result.macroName() + "'.");
            case CANCELLED_CONTEXT -> notifier.error("Stopped local macro '" + result.macroName() + "': server context changed.");
            case FAILED -> {
                LOGGER.warning("Local macro '" + result.macroName() + "' failed: " + result.detail());
                notifier.error("Stopped local macro '" + result.macroName() + "' after an action failed (see log).");
            }
            case IDLE, WAITING, EXECUTED -> {
                // Avoid chat noise for each queued action or delay tick.
            }
        }
    }

    private void saveConfigurations() {
        try {
            configuration.save(modules, clickGuiWindow);
        } catch (ConfigurationException exception) {
            LOGGER.log(Level.WARNING, "Unable to save Helikon configuration while stopping", exception);
        }
        try {
            hudConfiguration.save(hudLayout);
        } catch (ConfigurationException exception) {
            LOGGER.log(Level.WARNING, "Unable to save Helikon HUD layout while stopping", exception);
        }
        try { friends.save(); } catch (ConfigurationException exception) {
            LOGGER.log(Level.WARNING, "Unable to save friends while stopping", exception);
        }
        try {
            waypoints.save();
        } catch (ConfigurationException exception) {
            LOGGER.log(Level.WARNING, "Unable to save waypoints while stopping", exception);
        }
        try {
            macros.save();
        } catch (ConfigurationException exception) {
            LOGGER.log(Level.WARNING, "Unable to save macros while stopping", exception);
        }
        try {
            panicConfiguration.save(panicKeybinds);
        } catch (ConfigurationException exception) {
            LOGGER.log(Level.WARNING, "Unable to save panic keybind while stopping", exception);
        }
    }

    public ModuleRegistry modules() {
        return modules;
    }

    public EventBus events() {
        return events;
    }
}
