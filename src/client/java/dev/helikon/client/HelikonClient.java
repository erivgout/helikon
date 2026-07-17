package dev.helikon.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.helikon.client.command.ChatCommands;
import dev.helikon.client.command.CommandDispatcher;
import dev.helikon.client.command.HelikonCommands;
import dev.helikon.client.command.MinecraftServerCommandSender;
import dev.helikon.client.command.MinecraftKeyNameResolver;
import dev.helikon.client.chat.OutgoingChatFormatter;
import dev.helikon.client.chat.IncomingChatMessage;
import dev.helikon.client.chat.IncomingMessageAdapter;
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
import dev.helikon.client.hud.BetterCrosshairHud;
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
import dev.helikon.client.module.movement.AutoSprint;
import dev.helikon.client.module.movement.AutoSneak;
import dev.helikon.client.module.movement.AutoWalk;
import dev.helikon.client.module.movement.MovementModuleAccess;
import dev.helikon.client.module.movement.SprintContext;
import dev.helikon.client.module.player.AutoTool;
import dev.helikon.client.module.player.ToolCandidate;
import dev.helikon.client.module.player.AutoEat;
import dev.helikon.client.module.player.FoodCandidate;
import dev.helikon.client.module.player.MinecraftUseKeyAccess;
import dev.helikon.client.module.chat.ChatPrefix;
import dev.helikon.client.module.chat.ChatSuffix;
import dev.helikon.client.module.chat.ChatMute;
import dev.helikon.client.module.chat.ChatFilter;
import dev.helikon.client.module.chat.ChatSpammer;
import dev.helikon.client.module.chat.MinecraftChatSender;
import dev.helikon.client.module.chat.PrivateMessageHelper;
import dev.helikon.client.module.chat.MentionNotifier;
import dev.helikon.client.module.chat.AutoReply;
import dev.helikon.client.module.world.FastPlace;
import dev.helikon.client.module.world.MinecraftUseCooldownAccess;
import dev.helikon.client.module.render.Fullbright;
import dev.helikon.client.module.render.AntiBlind;
import dev.helikon.client.module.render.BetterCrosshair;
import dev.helikon.client.module.render.MinecraftGammaAccess;
import dev.helikon.client.module.render.MinecraftNightVisionAccess;
import dev.helikon.client.module.render.RenderModuleAccess;
import dev.helikon.client.notification.ChatNotifier;
import dev.helikon.client.panic.PanicController;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.privatechat.PrivateMessageHistory;
import dev.helikon.client.waypoint.MinecraftWaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadLocalRandom;
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
    private final PrivateMessageHistory privateMessageHistory = new PrivateMessageHistory();
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
        AntiBlind antiBlind = new AntiBlind();
        BetterCrosshair betterCrosshair = new BetterCrosshair();
        modules.register(antiBlind);
        modules.register(betterCrosshair);
        RenderModuleAccess.install(antiBlind, betterCrosshair);
        AutoSprint autoSprint = new AutoSprint();
        AutoWalk autoWalk = new AutoWalk();
        AutoSneak autoSneak = new AutoSneak();
        AutoEat autoEat = new AutoEat(new MinecraftUseKeyAccess());
        AutoTool autoTool = new AutoTool();
        FastPlace fastPlace = new FastPlace(new MinecraftUseCooldownAccess());
        ChatPrefix chatPrefix = new ChatPrefix();
        ChatSuffix chatSuffix = new ChatSuffix();
        ChatMute chatMute = new ChatMute();
        ChatFilter chatFilter = new ChatFilter();
        MinecraftChatSender normalChatSender = new MinecraftChatSender();
        ChatSpammer chatSpammer = new ChatSpammer(normalChatSender,
                () -> ThreadLocalRandom.current().nextInt());
        PrivateMessageHelper privateMessageHelper = new PrivateMessageHelper();
        MentionNotifier mentionNotifier = new MentionNotifier();
        AutoReply autoReply = new AutoReply();
        modules.register(autoSprint);
        modules.register(autoWalk);
        modules.register(autoSneak);
        modules.register(autoEat);
        modules.register(autoTool);
        modules.register(fastPlace);
        modules.register(chatPrefix);
        modules.register(chatSuffix);
        modules.register(chatMute);
        modules.register(chatFilter);
        modules.register(chatSpammer);
        modules.register(privateMessageHelper);
        modules.register(mentionNotifier);
        modules.register(autoReply);
        MovementModuleAccess.install(autoWalk, autoSneak);
        events.subscribe(ClientTickEvent.class, event -> {
            if (event.phase() == ClientTickEvent.Phase.POST) {
                modules.runGuarded(fullbright, "tick", fullbright::tick);
                modules.runGuarded(autoSprint, "tick", () -> tickAutoSprint(autoSprint));
                modules.runGuarded(autoEat, "tick", () -> tickAutoEat(autoEat));
                modules.runGuarded(autoTool, "tick", () -> tickAutoTool(autoTool));
                modules.runGuarded(fastPlace, "tick", () -> tickFastPlace(fastPlace));
                modules.runGuarded(chatSpammer, "tick", () -> tickChatSpammer(chatSpammer));
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
                panic, panicKeybinds, panicConfiguration, privateMessageHelper, privateMessageHistory,
                new MinecraftServerCommandSender());
        ChatCommands.register(commands, notifier);
        OutgoingChatFormatter outgoingChat = new OutgoingChatFormatter(chatPrefix, chatSuffix,
                () -> macroServerContext.currentServerAddress().orElse(null),
                () -> ThreadLocalRandom.current().nextInt());
        ClientSendMessageEvents.MODIFY_CHAT.register(outgoingChat::format);
        ClientSendMessageEvents.CHAT_CANCELED.register(chatSpammer::reportRejected);
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, chatType, receivedAt) ->
                allowIncomingMessage(chatMute, chatFilter, mentionNotifier, autoReply, normalChatSender,
                        IncomingMessageAdapter.chat(message, signedMessage, sender, receivedAt.toEpochMilli()))
        );
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                allowIncomingMessage(chatMute, chatFilter, mentionNotifier, autoReply, normalChatSender,
                        IncomingMessageAdapter.game(message, overlay, System.currentTimeMillis()))
        );

        HelikonKeybinds.register(modules, configuration, clickGuiWindow, hudLayout, hudConfiguration);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "active_modules"),
                new ActiveModulesHud(modules, hudLayout, panicState));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "waypoints"),
                new WaypointHud(waypoints, waypointLocations, panicState));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "better_crosshair"),
                new BetterCrosshairHud(betterCrosshair, panicState));
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

    /** Applies the Minecraft-free AutoSprint decision through normal local player state only. */
    private void tickAutoSprint(AutoSprint autoSprint) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            autoSprint.onPlayerUnavailable();
            return;
        }

        boolean screenOpen = client.gui.screen() != null;
        net.minecraft.world.entity.player.Input input = client.player.input.keyPresses;
        boolean forward = !screenOpen && input.forward();
        boolean moving = !screenOpen && (input.forward() || input.backward() || input.left() || input.right());
        AutoSprint.SprintAction action = autoSprint.update(new SprintContext(
                forward,
                moving,
                client.player.getFoodData().getFoodLevel(),
                client.player.horizontalCollision,
                client.player.isSprinting()
        ));
        switch (action) {
            case START -> client.player.setSprinting(true);
            case STOP -> client.player.setSprinting(false);
            case NONE -> {
                // The module has no local state transition to apply this tick.
            }
        }
    }

    /** Adapts ordinary local mining state into AutoTool's Minecraft-free selector. */
    private void tickAutoTool(AutoTool autoTool) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            autoTool.onPlayerUnavailable();
            return;
        }

        boolean mining = client.gui.screen() == null
                && client.level != null
                && client.gameMode != null
                && client.gameMode.isDestroying()
                && client.options.keyAttack.isDown()
                && client.hitResult instanceof BlockHitResult;
        List<ToolCandidate> candidates = mining ? toolCandidates(client) : List.of();
        AutoTool.Action action = autoTool.update(mining, client.player.getInventory().getSelectedSlot(), candidates);
        if (action.type() == AutoTool.ActionType.SELECT || action.type() == AutoTool.ActionType.RESTORE) {
            client.player.getInventory().setSelectedSlot(action.slot());
        }
    }

    /** Adapts ordinary local player state into AutoEat's safe hotbar-food policy. */
    private static void tickAutoEat(AutoEat autoEat) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            autoEat.onPlayerUnavailable();
            return;
        }

        boolean screenOpen = client.gui.screen() != null;
        List<FoodCandidate> candidates = screenOpen ? List.of() : foodCandidates(client);
        AutoEat.Action action = autoEat.tick(new AutoEat.Context(
                client.player.getInventory().getSelectedSlot(),
                client.player.getFoodData().getFoodLevel(),
                client.player.getHealth(),
                client.options.keyAttack.isDown(),
                client.player.hurtTime > 0,
                screenOpen,
                client.options.keyUse.isDown(),
                client.player.isUsingItem(),
                candidates
        ));
        if (action.restoreSlot() >= 0) {
            client.player.getInventory().setSelectedSlot(action.restoreSlot());
        } else if (action.selectSlot() >= 0) {
            client.player.getInventory().setSelectedSlot(action.selectSlot());
        }
    }

    private static List<ToolCandidate> toolCandidates(Minecraft client) {
        BlockHitResult hit = (BlockHitResult) client.hitResult;
        var blockState = client.level.getBlockState(hit.getBlockPos());
        List<ToolCandidate> candidates = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int remainingDurability = stack.isDamageableItem()
                    ? stack.getMaxDamage() - stack.getDamageValue()
                    : Integer.MAX_VALUE;
            candidates.add(new ToolCandidate(slot, stack.getDestroySpeed(blockState),
                    stack.isCorrectToolForDrops(blockState), remainingDurability));
        }
        return candidates;
    }

    private static List<FoodCandidate> foodCandidates(Minecraft client) {
        List<FoodCandidate> candidates = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (food == null || food.nutrition() < 1) {
                continue;
            }
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            candidates.add(new FoodCandidate(slot, itemId, food.nutrition(), food.saturation(), food.canAlwaysEat()));
        }
        return candidates;
    }

    /** Evaluates local incoming-message policies through normal module failure isolation. */
    private boolean allowIncomingMessage(ChatMute chatMute, ChatFilter chatFilter, MentionNotifier mentionNotifier,
                                         AutoReply autoReply, MinecraftChatSender normalChatSender,
                                         IncomingChatMessage message) {
        if (shouldHideIncoming(chatMute, "incoming-message", () -> chatMute.shouldHide(message))) {
            return false;
        }
        if (shouldHideIncoming(chatFilter, "incoming-message", () -> chatFilter.shouldHide(message))) {
            return false;
        }
        observeIncomingMessage(mentionNotifier, autoReply, normalChatSender, message);
        return true;
    }

    /** Runs local post-display chat actions through module failure isolation. */
    private void observeIncomingMessage(MentionNotifier mentionNotifier, AutoReply autoReply,
                                        MinecraftChatSender normalChatSender, IncomingChatMessage message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        String localPlayerName = client.player.getGameProfile().name();
        modules.runGuarded(mentionNotifier, "incoming-message", () -> {
            if (mentionNotifier.shouldNotify(message, localPlayerName)) {
                notifier.info("Mention from '" + message.sender() + "'.");
            }
        });
        modules.runGuarded(autoReply, "incoming-message", () -> autoReply.replyFor(
                message,
                localPlayerName,
                macroServerContext.currentServerAddress().orElse(""),
                client.gui.screen() != null
        ).ifPresent(normalChatSender::send));
    }

    private boolean shouldHideIncoming(dev.helikon.client.module.Module module, String operation, java.util.function.BooleanSupplier policy) {
        java.util.concurrent.atomic.AtomicBoolean hidden = new java.util.concurrent.atomic.AtomicBoolean();
        boolean successful = modules.runGuarded(module, operation, () -> hidden.set(policy.getAsBoolean()));
        return successful && hidden.get();
    }

    /** Applies a FastPlace cooldown reduction after Minecraft has handled this tick's normal use input. */
    private static void tickFastPlace(FastPlace fastPlace) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null) {
            return;
        }

        ItemStack heldItem = client.player.getInventory().getItem(client.player.getInventory().getSelectedSlot());
        if (heldItem.isEmpty()) {
            return;
        }
        fastPlace.tick(client.options.keyUse.isDown(), heldItem.getItem() instanceof BlockItem);
    }

    /** Sends only through ChatSpammer's constrained, ordinary local chat policy. */
    private static void tickChatSpammer(ChatSpammer chatSpammer) {
        Minecraft client = Minecraft.getInstance();
        chatSpammer.tick(client.player != null, client.gui.screen() != null);
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
