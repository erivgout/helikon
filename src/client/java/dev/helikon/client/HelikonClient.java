package dev.helikon.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.helikon.client.command.ChatCommands;
import dev.helikon.client.command.ChatHistoryCommand;
import dev.helikon.client.command.CommandDispatcher;
import dev.helikon.client.command.HelikonCommands;
import dev.helikon.client.command.BetterChatCommand;
import dev.helikon.client.command.MinecraftChatInputReopener;
import dev.helikon.client.command.ScheduledChatInputReopener;
import dev.helikon.client.command.MinecraftTextClipboard;
import dev.helikon.client.command.MinecraftServerCommandSender;
import dev.helikon.client.command.MinecraftKeyNameResolver;
import dev.helikon.client.chat.OutgoingChatFormatter;
import dev.helikon.client.chat.AnnouncerObservationTracker;
import dev.helikon.client.chat.ChatHistoryManager;
import dev.helikon.client.chat.ChatDisplayAccess;
import dev.helikon.client.chat.BetterChatDisplayAccess;
import dev.helikon.client.chat.IncomingChatMessage;
import dev.helikon.client.chat.IncomingMessageAdapter;
import dev.helikon.client.combat.CombatTargetTracker;
import dev.helikon.client.automation.MinecraftContainerClicker;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.config.HudConfigurationManager;
import dev.helikon.client.config.PanicConfigurationManager;
import dev.helikon.client.config.ProfileManager;
import dev.helikon.client.event.ClientTickEvent;
import dev.helikon.client.event.ChatEvent;
import dev.helikon.client.event.EventBus;
import dev.helikon.client.event.ScreenEvent;
import dev.helikon.client.event.ScreenTransitionTracker;
import dev.helikon.client.event.WorldEvent;
import dev.helikon.client.event.PlayerStateEventTracker;
import dev.helikon.client.event.PlayerStateSnapshot;
import dev.helikon.client.event.PlayerLifecycleEvent;
import dev.helikon.client.event.RenderEvent;
import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.friend.FriendToggleGesture;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.gui.HelikonClickGuiScreen;
import dev.helikon.client.gui.HelikonHudEditorScreen;
import dev.helikon.client.gui.HelikonThemeEditorScreen;
import dev.helikon.client.gui.HelikonAutoReconnectScreen;
import dev.helikon.client.hud.ActiveModulesHud;
import dev.helikon.client.hud.BetterCrosshairHud;
import dev.helikon.client.hud.ElytraHud;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.hud.MiniPlayerHud;
import dev.helikon.client.hud.RadarHud;
import dev.helikon.client.hud.ReachDisplayHud;
import dev.helikon.client.hud.SaturationHud;
import dev.helikon.client.hud.TargetHud;
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
import dev.helikon.client.module.combat.AntiBot;
import dev.helikon.client.module.combat.AutoPotion;
import dev.helikon.client.module.combat.BowAimAssist;
import dev.helikon.client.module.combat.CriticalAssist;
import dev.helikon.client.module.combat.KillAura;
import dev.helikon.client.module.combat.MinecraftCombatAccess;
import dev.helikon.client.module.combat.ReachDisplay;
import dev.helikon.client.module.combat.TriggerBot;
import dev.helikon.client.module.movement.AutoSprint;
import dev.helikon.client.module.movement.AutoSneak;
import dev.helikon.client.module.movement.AutoWalk;
import dev.helikon.client.module.movement.AutoParkour;
import dev.helikon.client.module.movement.InventoryWalk;
import dev.helikon.client.module.movement.AntiAfk;
import dev.helikon.client.module.movement.AdvancedMovementInputAccess;
import dev.helikon.client.module.movement.AntiAfkAccess;
import dev.helikon.client.module.movement.BunnyHop;
import dev.helikon.client.module.movement.ExtraElytra;
import dev.helikon.client.module.movement.FastLadders;
import dev.helikon.client.module.movement.Flight;
import dev.helikon.client.module.movement.FreecamAccess;
import dev.helikon.client.module.movement.MinecraftAdvancedMovementAccess;
import dev.helikon.client.module.movement.MovementModuleAccess;
import dev.helikon.client.module.movement.NoFall;
import dev.helikon.client.module.movement.NoSlow;
import dev.helikon.client.module.movement.NoSlowAccess;
import dev.helikon.client.module.movement.InventoryWalkAccess;
import dev.helikon.client.module.movement.ParkourAccess;
import dev.helikon.client.module.movement.Scaffold;
import dev.helikon.client.module.movement.SprintContext;
import dev.helikon.client.module.movement.Step;
import dev.helikon.client.module.movement.StepAccess;
import dev.helikon.client.module.movement.Speed;
import dev.helikon.client.module.movement.Timer;
import dev.helikon.client.module.movement.TimerModuleAccess;
import dev.helikon.client.module.player.AutoTool;
import dev.helikon.client.module.player.AutoArmor;
import dev.helikon.client.module.player.AutoEject;
import dev.helikon.client.module.player.AutoFish;
import dev.helikon.client.module.player.AutoReconnect;
import dev.helikon.client.module.player.AutoTotem;
import dev.helikon.client.module.player.ArmorCandidate;
import dev.helikon.client.module.player.ArmorSlot;
import dev.helikon.client.module.player.InventoryItem;
import dev.helikon.client.module.player.InventoryManager;
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
import dev.helikon.client.module.chat.AntiSpam;
import dev.helikon.client.module.chat.ChatTimestamps;
import dev.helikon.client.module.chat.ChatColor;
import dev.helikon.client.module.chat.BetterChat;
import dev.helikon.client.module.chat.ChatHistory;
import dev.helikon.client.module.chat.Announcer;
import dev.helikon.client.module.chat.AnnouncerAccess;
import dev.helikon.client.module.chat.AnnouncementTrigger;
import dev.helikon.client.module.world.FastPlace;
import dev.helikon.client.module.world.BuilderAssist;
import dev.helikon.client.module.world.ChestItem;
import dev.helikon.client.module.world.ChestSteal;
import dev.helikon.client.module.world.MinecraftBuilderAssistAccess;
import dev.helikon.client.mixin.FishingHookAccessor;
import dev.helikon.client.module.world.MinecraftUseCooldownAccess;
import dev.helikon.client.module.render.Fullbright;
import dev.helikon.client.module.render.AntiBlind;
import dev.helikon.client.module.render.AntiTotemAnimation;
import dev.helikon.client.module.render.BetterCrosshair;
import dev.helikon.client.module.render.BetterNametags;
import dev.helikon.client.module.render.Dinnerbone;
import dev.helikon.client.module.render.BlockEsp;
import dev.helikon.client.module.render.Breadcrumbs;
import dev.helikon.client.module.render.DamageIndicators;
import dev.helikon.client.module.render.EntityEsp;
import dev.helikon.client.module.render.MinecraftGammaAccess;
import dev.helikon.client.module.render.MinecraftNightVisionAccess;
import dev.helikon.client.module.render.MiniPlayer;
import dev.helikon.client.module.render.RenderModuleAccess;
import dev.helikon.client.module.render.Radar;
import dev.helikon.client.module.render.RainbowEnchant;
import dev.helikon.client.module.render.SaturationDisplay;
import dev.helikon.client.module.render.StorageEsp;
import dev.helikon.client.module.render.Trajectories;
import dev.helikon.client.module.render.Tracers;
import dev.helikon.client.module.render.TrueSight;
import dev.helikon.client.module.render.XRay;
import dev.helikon.client.notification.ChatNotifier;
import dev.helikon.client.panic.PanicController;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.privatechat.PrivateMessageHistory;
import dev.helikon.client.render.MinecraftWorldVisualizationRenderer;
import dev.helikon.client.render.MinecraftXRayRendererInvalidator;
import dev.helikon.client.waypoint.MinecraftWaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final ChatHistoryManager chatHistory = new ChatHistoryManager(
            FabricLoader.getInstance().getConfigDir().resolve(MOD_ID)
    );
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
    private final ScreenTransitionTracker screenTransitions = new ScreenTransitionTracker();
    private final PlayerStateEventTracker playerStateEvents = new PlayerStateEventTracker();
    private long clientTick;
    private ServerData reconnectServer;
    private ServerData lastConnectedServer;
    private boolean reconnectAttemptInFlight;

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
        AntiTotemAnimation antiTotemAnimation = new AntiTotemAnimation();
        BetterCrosshair betterCrosshair = new BetterCrosshair();
        BetterNametags betterNametags = new BetterNametags();
        Dinnerbone dinnerbone = new Dinnerbone();
        RainbowEnchant rainbowEnchant = new RainbowEnchant();
        EntityEsp entityEsp = new EntityEsp();
        BlockEsp blockEsp = new BlockEsp();
        Tracers tracers = new Tracers();
        Trajectories trajectories = new Trajectories();
        TrueSight trueSight = new TrueSight();
        Radar radar = new Radar();
        SaturationDisplay saturationDisplay = new SaturationDisplay();
        StorageEsp storageEsp = new StorageEsp();
        XRay xray = new XRay(new MinecraftXRayRendererInvalidator());
        MiniPlayer miniPlayer = new MiniPlayer();
        DamageIndicators damageIndicators = new DamageIndicators();
        Breadcrumbs breadcrumbs = new Breadcrumbs();
        modules.register(antiBlind);
        modules.register(antiTotemAnimation);
        modules.register(betterCrosshair);
        modules.register(betterNametags);
        modules.register(dinnerbone);
        modules.register(rainbowEnchant);
        modules.register(entityEsp);
        modules.register(blockEsp);
        modules.register(tracers);
        modules.register(trajectories);
        modules.register(trueSight);
        modules.register(radar);
        modules.register(saturationDisplay);
        modules.register(storageEsp);
        modules.register(xray);
        modules.register(miniPlayer);
        modules.register(damageIndicators);
        modules.register(breadcrumbs);
        RenderModuleAccess.install(antiBlind, betterCrosshair, antiTotemAnimation, dinnerbone, rainbowEnchant);
        AutoSprint autoSprint = new AutoSprint();
        AutoWalk autoWalk = new AutoWalk();
        AutoSneak autoSneak = new AutoSneak();
        AutoParkour autoParkour = new AutoParkour();
        InventoryWalk inventoryWalk = new InventoryWalk();
        AntiAfk antiAfk = new AntiAfk();
        NoSlow noSlow = new NoSlow();
        FastLadders fastLadders = new FastLadders();
        Step step = new Step();
        Speed speed = new Speed();
        BunnyHop bunnyHop = new BunnyHop();
        Flight flight = new Flight();
        NoFall noFall = new NoFall();
        ExtraElytra extraElytra = new ExtraElytra();
        Scaffold scaffold = new Scaffold();
        Timer timer = new Timer();
        AutoEat autoEat = new AutoEat(new MinecraftUseKeyAccess());
        AutoTool autoTool = new AutoTool();
        AutoArmor autoArmor = new AutoArmor();
        AutoEject autoEject = new AutoEject();
        AutoFish autoFish = new AutoFish();
        AutoReconnect autoReconnect = new AutoReconnect();
        AutoTotem autoTotem = new AutoTotem();
        InventoryManager inventoryManager = new InventoryManager();
        FastPlace fastPlace = new FastPlace(new MinecraftUseCooldownAccess());
        ChestSteal chestSteal = new ChestSteal();
        BuilderAssist builderAssist = new BuilderAssist();
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
        AntiSpam antiSpam = new AntiSpam();
        ChatTimestamps chatTimestamps = new ChatTimestamps();
        ChatColor chatColor = new ChatColor();
        BetterChat betterChat = new BetterChat();
        Announcer announcer = new Announcer();
        ChatHistory chatHistoryModule = new ChatHistory();
        chatHistoryModule.setStorageHooks(
                () -> chatHistory.activate(chatHistoryModule, currentChatHistoryScope()),
                chatHistory::deactivate,
                () -> chatHistory.updateSettings(chatHistoryModule)
        );
        AntiBot antiBot = new AntiBot();
        TriggerBot triggerBot = new TriggerBot();
        BowAimAssist bowAimAssist = new BowAimAssist();
        CriticalAssist criticalAssist = new CriticalAssist();
        AutoPotion autoPotion = new AutoPotion();
        dev.helikon.client.module.combat.TargetHud targetHud = new dev.helikon.client.module.combat.TargetHud();
        KillAura killAura = new KillAura();
        ReachDisplay reachDisplay = new ReachDisplay();
        CombatTargetTracker combatTracker = new CombatTargetTracker();
        modules.register(autoSprint);
        modules.register(autoWalk);
        modules.register(autoSneak);
        modules.register(autoParkour);
        modules.register(inventoryWalk);
        modules.register(antiAfk);
        modules.register(noSlow);
        modules.register(fastLadders);
        modules.register(step);
        modules.register(speed);
        modules.register(bunnyHop);
        modules.register(flight);
        modules.register(noFall);
        modules.register(extraElytra);
        modules.register(scaffold);
        modules.register(timer);
        modules.register(autoEat);
        modules.register(autoTool);
        modules.register(autoArmor);
        modules.register(autoEject);
        modules.register(autoFish);
        modules.register(autoReconnect);
        modules.register(autoTotem);
        modules.register(inventoryManager);
        modules.register(fastPlace);
        modules.register(chestSteal);
        modules.register(builderAssist);
        modules.register(chatPrefix);
        modules.register(chatSuffix);
        modules.register(chatMute);
        modules.register(chatFilter);
        modules.register(chatSpammer);
        modules.register(privateMessageHelper);
        modules.register(mentionNotifier);
        modules.register(autoReply);
        modules.register(antiSpam);
        modules.register(chatTimestamps);
        modules.register(chatColor);
        modules.register(betterChat);
        modules.register(announcer);
        modules.register(chatHistoryModule);
        modules.register(antiBot);
        modules.register(triggerBot);
        modules.register(bowAimAssist);
        modules.register(criticalAssist);
        modules.register(autoPotion);
        modules.register(targetHud);
        modules.register(killAura);
        modules.register(reachDisplay);
        ChatDisplayAccess.install(chatTimestamps);
        ChatDisplayAccess.install(chatColor);
        BetterChatDisplayAccess.install(betterChat);
        AnnouncerAccess.install(announcer, normalChatSender);
        MovementModuleAccess.install(autoWalk, autoSneak);
        InventoryWalkAccess.install(inventoryWalk);
        ParkourAccess.install(autoParkour);
        AntiAfkAccess.install(antiAfk);
        AdvancedMovementInputAccess.install(bunnyHop, scaffold);
        FreecamAccess.install(flight);
        NoSlowAccess.install(noSlow);
        StepAccess.install(step);
        TimerModuleAccess.install(timer);
        MinecraftWorldVisualizationRenderer worldVisuals = new MinecraftWorldVisualizationRenderer(
                modules, friends, entityEsp, betterNametags, blockEsp, tracers, trajectories, trueSight, storageEsp, damageIndicators,
                breadcrumbs, builderAssist, bowAimAssist
        );
        AtomicReference<MinecraftCombatAccess.Snapshot> combatSnapshot = new AtomicReference<>(
                MinecraftCombatAccess.Snapshot.unavailable());
        AtomicBoolean combatAttackStarted = new AtomicBoolean();
        events.subscribe(ClientTickEvent.class, event -> {
            if (event.phase() == ClientTickEvent.Phase.POST) {
                if (Minecraft.getInstance().level == null && timer.isEnabled()) {
                    modules.setEnabled(timer, false);
                }
                modules.runGuarded(fullbright, "tick", fullbright::tick);
                modules.runGuarded(autoSprint, "tick", () -> tickAutoSprint(autoSprint));
                modules.runGuarded(fastLadders, "tick", () -> MinecraftAdvancedMovementAccess.tickFastLadders(fastLadders));
                modules.runGuarded(speed, "tick", () -> MinecraftAdvancedMovementAccess.tickSpeed(speed));
                modules.runGuarded(bunnyHop, "tick", () -> MinecraftAdvancedMovementAccess.tickBunnyHop(bunnyHop));
                modules.runGuarded(flight, "tick", () -> MinecraftAdvancedMovementAccess.tickFlight(flight));
                modules.runGuarded(noFall, "tick", () -> MinecraftAdvancedMovementAccess.tickNoFall(noFall));
                modules.runGuarded(extraElytra, "tick", () -> MinecraftAdvancedMovementAccess.tickElytra(extraElytra));
                modules.runGuarded(scaffold, "tick", () -> MinecraftAdvancedMovementAccess.tickScaffold(scaffold, clientTick));
                modules.runGuarded(autoEat, "tick", () -> tickAutoEat(autoEat));
                modules.runGuarded(autoTool, "tick", () -> tickAutoTool(autoTool));
                modules.runGuarded(autoArmor, "tick", () -> tickAutoArmor(autoArmor, clientTick));
                modules.runGuarded(autoEject, "tick", () -> tickAutoEject(autoEject, clientTick));
                modules.runGuarded(autoFish, "tick", () -> tickAutoFish(autoFish, clientTick));
                modules.runGuarded(autoReconnect, "tick", () -> tickAutoReconnect(autoReconnect, clientTick));
                modules.runGuarded(autoTotem, "tick", () -> tickAutoTotem(autoTotem, clientTick));
                modules.runGuarded(inventoryManager, "tick", () -> tickInventoryManager(inventoryManager, clientTick));
                modules.runGuarded(fastPlace, "tick", () -> tickFastPlace(fastPlace));
                modules.runGuarded(chestSteal, "tick", () -> tickChestSteal(chestSteal, clientTick));
                modules.runGuarded(builderAssist, "tick", () -> MinecraftBuilderAssistAccess.tick(builderAssist, clientTick));
                modules.runGuarded(chatSpammer, "tick", () -> tickChatSpammer(chatSpammer));
                modules.runGuarded(announcer, "tick", HelikonClient::tickAnnouncer);
                combatAttackStarted.set(false);
                combatSnapshot.set(MinecraftCombatAccess.Snapshot.unavailable());
                modules.runGuarded(antiBot, "observe", () -> combatSnapshot.set(MinecraftCombatAccess.observe(friends, antiBot)));
                modules.runGuarded(targetHud, "tick", () -> MinecraftCombatAccess.observeTarget(targetHud,
                        combatSnapshot.get(), combatTracker));
                modules.runGuarded(autoPotion, "tick", () -> MinecraftCombatAccess.tickAutoPotion(clientTick, autoPotion));
                modules.runGuarded(bowAimAssist, "tick", () -> MinecraftCombatAccess.tickBowAim(bowAimAssist,
                        combatSnapshot.get()));
                modules.runGuarded(triggerBot, "tick", () -> {
                    if (!combatAttackStarted.get()) {
                        combatAttackStarted.set(MinecraftCombatAccess.tickTriggerBot(clientTick, triggerBot,
                                combatSnapshot.get(), combatTracker));
                    }
                });
                modules.runGuarded(criticalAssist, "tick", () -> {
                    if (!combatAttackStarted.get()) {
                        combatAttackStarted.set(MinecraftCombatAccess.tickCriticalAssist(clientTick, criticalAssist,
                                combatSnapshot.get(), combatTracker));
                    }
                });
                modules.runGuarded(killAura, "tick", () -> {
                    if (!combatAttackStarted.get()) {
                        combatAttackStarted.set(MinecraftCombatAccess.tickKillAura(clientTick, killAura,
                                combatSnapshot.get(), combatTracker));
                    }
                });
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
        commands.register(new BetterChatCommand(betterChat, BetterChatDisplayAccess::localHistory,
                new MinecraftTextClipboard()));
        commands.register(new ChatHistoryCommand(chatHistoryModule, chatHistory, new MinecraftTextClipboard(),
                new ScheduledChatInputReopener(new MinecraftChatInputReopener(), pendingScreenAction::set)));
        ChatCommands.register(commands, notifier);
        OutgoingChatFormatter outgoingChat = new OutgoingChatFormatter(chatPrefix, chatSuffix,
                () -> macroServerContext.currentServerAddress().orElse(null),
                () -> ThreadLocalRandom.current().nextInt());
        ClientSendMessageEvents.MODIFY_CHAT.register(outgoingChat::format);
        ClientSendMessageEvents.CHAT_CANCELED.register(chatSpammer::reportRejected);
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, chatType, receivedAt) -> {
            IncomingChatMessage incoming = IncomingMessageAdapter.chat(message, signedMessage, sender, receivedAt.toEpochMilli());
            boolean allowed = allowIncomingMessage(chatMute, chatFilter, antiSpam, mentionNotifier, autoReply,
                    normalChatSender, incoming);
            if (allowed && !incoming.overlay()) {
                chatHistory.recordIncoming(chatHistoryModule, incoming);
            }
            return allowed;
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            IncomingChatMessage incoming = IncomingMessageAdapter.game(message, overlay, System.currentTimeMillis());
            boolean allowed = allowIncomingMessage(chatMute, chatFilter, antiSpam, mentionNotifier, autoReply,
                    normalChatSender, incoming);
            if (allowed && !incoming.overlay()) {
                chatHistory.recordIncoming(chatHistoryModule, incoming);
            }
            return allowed;
        });
        ClientSendMessageEvents.CHAT.register(message -> {
            if (!message.startsWith(CommandDispatcher.PREFIX)) {
                chatHistory.recordOutgoing(chatHistoryModule, message, System.currentTimeMillis());
            }
            events.post(new ChatEvent(ChatEvent.Direction.SEND, message, false));
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, chatType, receivedAt) ->
                events.post(new ChatEvent(ChatEvent.Direction.RECEIVE, message.getString(), false)));
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                events.post(new ChatEvent(ChatEvent.Direction.RECEIVE, message.getString(), true)));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            reconnectServer = null;
            lastConnectedServer = client.getCurrentServer();
            reconnectAttemptInFlight = false;
            AnnouncerAccess.enqueue(AnnouncementTrigger.JOIN, "joined the world");
            chatHistory.switchScope(chatHistoryModule, currentChatHistoryScope());
            events.post(new WorldEvent(WorldEvent.Phase.JOIN, serverAddress(lastConnectedServer)));
            modules.runGuarded(autoReconnect, "connect", autoReconnect::onConnected);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            modules.runGuarded(announcer, "leave", () -> announcer.messageFor(AnnouncementTrigger.LEAVE,
                    "left the world", System.currentTimeMillis(), false).ifPresent(notifier::info));
            AnnouncerAccess.reset();
            try {
                chatHistory.saveIfNeeded();
            } catch (ConfigurationException exception) {
                LOGGER.log(Level.WARNING, "Unable to save local chat history while disconnecting", exception);
            }
            events.post(new WorldEvent(WorldEvent.Phase.LEAVE, serverAddress(lastConnectedServer)));
            playerStateEvents.reset();
            modules.runGuarded(autoReconnect, "disconnect", () -> observeAutoReconnectDisconnect(autoReconnect, client));
            if (timer.isEnabled()) {
                modules.setEnabled(timer, false);
            }
        });
        events.subscribe(PlayerLifecycleEvent.class, event -> {
            if (event.phase() == PlayerLifecycleEvent.Phase.DEATH) {
                AnnouncerAccess.enqueue(AnnouncementTrigger.DEATH, "died");
            }
        });
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, position, state) -> {
            if (player == Minecraft.getInstance().player) {
                AnnouncerAccess.enqueue(AnnouncementTrigger.BLOCK_MINED,
                        BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            }
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                AnnouncerAccess.observeEntityUnload(entity.getUUID(), living.isDeadOrDying(), System.currentTimeMillis());
            }
        });

        HelikonKeybinds.register(modules, configuration, clickGuiWindow, hudLayout, hudConfiguration);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "active_modules"),
                new ActiveModulesHud(modules, hudLayout, panicState));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "waypoints"),
                new WaypointHud(waypoints, waypointLocations, panicState));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "better_crosshair"),
                new BetterCrosshairHud(betterCrosshair, panicState));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "radar"),
                new RadarHud(radar, friends, panicState));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "saturation"),
                new SaturationHud(saturationDisplay, panicState));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "mini_player"),
                new MiniPlayerHud(miniPlayer, panicState));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "elytra"),
                new ElytraHud(extraElytra, panicState));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "target_hud"),
                new TargetHud(targetHud, combatTracker, panicState));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "reach_display"),
                new ReachDisplayHud(reachDisplay, combatTracker, panicState));
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            events.post(new RenderEvent(RenderEvent.Kind.WORLD, 0.0D, ""));
            worldVisuals.render(context);
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            observeScreenTransition(client);
            screenWasOpenAtTickStart = client.gui.screen() != null;
            helikonScreenWasOpenAtTickStart = isHelikonScreen(client);
            events.post(new ClientTickEvent(ClientTickEvent.Phase.PRE));
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            clientTick++;
            playerStateEvents.observe(playerStateSnapshot(client)).forEach(events::post);
            events.post(new ClientTickEvent(ClientTickEvent.Phase.POST));
            BetterChatDisplayAccess.tickSmoothScroll();
            worldVisuals.tick(client);

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

    /** Publishes screen changes at a tick boundary without retaining a Minecraft screen object in the event layer. */
    private void observeScreenTransition(Minecraft client) {
        Screen screen = client.gui.screen();
        String nextScreenId = screen == null ? "" : screen.getClass().getName();
        screenTransitions.update(screen, nextScreenId).forEach(events::post);
    }

    private static String serverAddress(ServerData server) {
        return server == null || server.ip == null ? "" : server.ip;
    }

    private static void tickAnnouncer() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            AnnouncerAccess.reset();
            return;
        }
        AnnouncerAccess.tick(new AnnouncerObservationTracker.Fact(client.player.getX(), client.player.getY(),
                        client.player.getZ(), client.player.getHealth(), client.level.dimension().identifier().toString()),
                client.gui.screen() != null, System.currentTimeMillis());
    }

    private String currentChatHistoryScope() {
        return macroServerContext.currentServerAddress().orElse(ChatHistoryManager.SINGLEPLAYER_SCOPE);
    }

    /** Samples only local player state; event interpretation remains in the Minecraft-free tracker. */
    private static PlayerStateSnapshot playerStateSnapshot(Minecraft client) {
        if (client.player == null) {
            return null;
        }
        Player player = client.player;
        Inventory inventory = player.getInventory();
        long fingerprint = 1L;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            fingerprint = 31L * fingerprint + System.identityHashCode(stack.getItem());
            fingerprint = 31L * fingerprint + stack.getCount();
            fingerprint = 31L * fingerprint + stack.getDamageValue();
            fingerprint = 31L * fingerprint + stack.getComponents().hashCode();
        }
        return new PlayerStateSnapshot(player.isAlive(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), inventory.getSelectedSlot(), fingerprint);
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

    /** Runs one verified normal inventory swap for a strictly better local armor candidate. */
    private static void tickAutoArmor(AutoArmor autoArmor, long tick) {
        Minecraft client = Minecraft.getInstance();
        if (!isOwnInventoryScreen(client)) {
            return;
        }
        InventoryMenu menu = client.player.inventoryMenu;
        Inventory inventory = client.player.getInventory();
        autoArmor.nextAction(tick, armorCandidates(menu, inventory), equippedArmor(client.player), armorDestinationSlots())
                .ifPresent(clicks -> MinecraftContainerClicker.apply(client, clicks));
    }

    /** Drops one configured item only while the player's ordinary inventory screen is open. */
    private static void tickAutoEject(AutoEject autoEject, long tick) {
        Minecraft client = Minecraft.getInstance();
        if (!isOwnInventoryScreen(client)) {
            return;
        }
        autoEject.nextAction(tick, inventoryItems(client.player.inventoryMenu, client.player.getInventory()))
                .ifPresent(clicks -> MinecraftContainerClicker.apply(client, clicks));
    }

    /** Reels/recasts only an already selected fishing rod through Minecraft's normal interaction method. */
    private static void tickAutoFish(AutoFish autoFish, long tick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        ItemStack held = client.player.getInventory().getSelectedItem();
        FishingHook hook = client.player.fishing;
        AutoFish.Action action = autoFish.update(tick, held.getItem() instanceof FishingRodItem, remainingDurability(held),
                hook != null, hook != null && ((FishingHookAccessor) hook).helikon$isBiting(),
                hook != null && hook.isOpenWaterFishing());
        if (action == AutoFish.Action.USE_HELD_ROD) {
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        }
    }

    /** Records a server disconnect only when Minecraft still has a valid multiplayer target. */
    private void observeAutoReconnectDisconnect(AutoReconnect autoReconnect, Minecraft client) {
        ServerData server = client.getCurrentServer();
        if (server == null) {
            server = lastConnectedServer;
        }
        if (server == null || client.isLocalServer() || !ServerAddress.isValidAddress(server.ip)) {
            reconnectServer = null;
            reconnectAttemptInFlight = false;
            autoReconnect.onConnected();
            return;
        }
        reconnectServer = server;
        reconnectAttemptInFlight = false;
        autoReconnect.onDisconnected(clientTick, server.ip);
    }

    /** Bridges the local reconnect policy to Minecraft's ordinary ConnectScreen workflow. */
    private void tickAutoReconnect(AutoReconnect autoReconnect, long tick) {
        if (!autoReconnect.isAwaitingDisconnectScreen()) {
            reconnectServer = null;
            reconnectAttemptInFlight = false;
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Screen current = client.gui.screen();
        if (reconnectAttemptInFlight && current instanceof DisconnectedScreen) {
            reconnectAttemptInFlight = false;
            autoReconnect.onReconnectFailed(tick);
        }
        if (current instanceof DisconnectedScreen disconnectScreen) {
            client.setScreenAndShow(new HelikonAutoReconnectScreen(disconnectScreen, autoReconnect, () -> clientTick));
            current = client.gui.screen();
        }
        if (current instanceof ConnectScreen) {
            return;
        }
        boolean disconnectScreenVisible = current instanceof DisconnectedScreen
                || current instanceof HelikonAutoReconnectScreen;
        Screen reconnectParent = current;
        autoReconnect.nextReconnect(tick, disconnectScreenVisible).ifPresent(address -> {
            if (reconnectServer == null) {
                autoReconnect.onConnected();
                return;
            }
            reconnectAttemptInFlight = true;
            ConnectScreen.startConnecting(reconnectParent, client, ServerAddress.parseString(address), reconnectServer,
                    false, null);
        });
    }

    /** Uses only an existing inventory totem and normal inventory swaps while the player inventory is open. */
    private static void tickAutoTotem(AutoTotem autoTotem, long tick) {
        Minecraft client = Minecraft.getInstance();
        if (!isOwnInventoryScreen(client)) {
            return;
        }
        ItemStack offhand = client.player.getItemBySlot(EquipmentSlot.OFFHAND);
        autoTotem.nextAction(tick, new AutoTotem.Context(client.player.getHealth(), (float) client.player.fallDistance,
                        offhand.is(Items.TOTEM_OF_UNDYING), offhand.isEmpty() ? "" : itemId(offhand),
                        InventoryMenu.SHIELD_SLOT, inventoryItems(client.player.inventoryMenu, client.player.getInventory())))
                .ifPresent(clicks -> MinecraftContainerClicker.apply(client, clicks));
    }

    /** Runs one conservative inventory organization click sequence on the open vanilla inventory. */
    private static void tickInventoryManager(InventoryManager inventoryManager, long tick) {
        Minecraft client = Minecraft.getInstance();
        if (!isOwnInventoryScreen(client)) {
            return;
        }
        InventoryMenu menu = client.player.inventoryMenu;
        Inventory inventory = client.player.getInventory();
        inventoryManager.nextAction(tick, inventoryItems(menu, inventory), inventoryMenuSlots(menu, inventory))
                .ifPresent(clicks -> MinecraftContainerClicker.apply(client, clicks));
    }

    /** Transfers one visible chest item through Minecraft's normal QUICK_MOVE action, or closes that menu. */
    private static void tickChestSteal(ChestSteal chestSteal, long tick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || !(client.gui.screen() instanceof AbstractContainerScreen<?>)
                || !(client.player.containerMenu instanceof ChestMenu menu) || !menu.getCarried().isEmpty()) {
            return;
        }
        List<ChestItem> items = new ArrayList<>();
        int chestSlots = menu.getContainer().getContainerSize();
        for (int slot = 0; slot < chestSlots; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (!stack.isEmpty()) {
                items.add(new ChestItem(slot, itemId(stack), stack.getCount(), stack.getRarity().ordinal()));
            }
        }
        chestSteal.nextAction(tick, menu.containerId, items).ifPresent(action -> {
            if (action.type() == ChestSteal.ActionType.CLOSE) {
                client.player.closeContainer();
            } else {
                MinecraftContainerClicker.apply(client, action.clicks());
            }
        });
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

    private static boolean isOwnInventoryScreen(Minecraft client) {
        return client.player != null
                && client.gameMode != null
                && client.gui.screen() instanceof InventoryScreen
                && client.player.containerMenu == client.player.inventoryMenu
                && client.player.inventoryMenu.getCarried().isEmpty();
    }

    private static List<InventoryItem> inventoryItems(InventoryMenu menu, Inventory inventory) {
        List<InventoryItem> items = new ArrayList<>();
        for (int menuSlot = InventoryMenu.INV_SLOT_START; menuSlot < InventoryMenu.USE_ROW_SLOT_END; menuSlot++) {
            Slot slot = menu.getSlot(menuSlot);
            if (slot.container != inventory || slot.getContainerSlot() < 0 || slot.getContainerSlot() >= 36) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                items.add(new InventoryItem(menuSlot, slot.getContainerSlot(), itemId(stack), stack.getCount(),
                        stack.getCustomName() != null, stack.isEnchanted(), remainingDurability(stack)));
            }
        }
        return items;
    }

    private static Map<Integer, Integer> inventoryMenuSlots(InventoryMenu menu, Inventory inventory) {
        Map<Integer, Integer> menuSlots = new java.util.LinkedHashMap<>();
        for (int menuSlot = InventoryMenu.INV_SLOT_START; menuSlot < InventoryMenu.USE_ROW_SLOT_END; menuSlot++) {
            Slot slot = menu.getSlot(menuSlot);
            if (slot.container == inventory && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 36) {
                menuSlots.put(slot.getContainerSlot(), menuSlot);
            }
        }
        return Map.copyOf(menuSlots);
    }

    private static List<ArmorCandidate> armorCandidates(InventoryMenu menu, Inventory inventory) {
        List<ArmorCandidate> candidates = new ArrayList<>();
        for (int menuSlot = InventoryMenu.INV_SLOT_START; menuSlot < InventoryMenu.USE_ROW_SLOT_END; menuSlot++) {
            Slot slot = menu.getSlot(menuSlot);
            if (slot.container == inventory && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 36) {
                armorCandidate(menuSlot, slot.getItem()).ifPresent(candidates::add);
            }
        }
        return candidates;
    }

    private static Map<ArmorSlot, ArmorCandidate> equippedArmor(Player player) {
        Map<ArmorSlot, ArmorCandidate> equipped = new EnumMap<>(ArmorSlot.class);
        for (Map.Entry<ArmorSlot, Integer> entry : armorDestinationSlots().entrySet()) {
            ArmorCandidate candidate = armorCandidate(entry.getValue(), player.getItemBySlot(minecraftArmorSlot(entry.getKey())))
                    .orElse(null);
            if (candidate != null) {
                equipped.put(entry.getKey(), candidate);
            }
        }
        return equipped;
    }

    private static Optional<ArmorCandidate> armorCandidate(int menuSlot, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || !equippable.slot().isArmor()) {
            return Optional.empty();
        }
        ArmorSlot armorSlot = armorSlot(equippable.slot());
        if (armorSlot == null) {
            return Optional.empty();
        }
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY);
        return Optional.of(new ArmorCandidate(menuSlot, armorSlot,
                modifiers.compute(Attributes.ARMOR, 0.0D, equippable.slot()),
                modifiers.compute(Attributes.ARMOR_TOUGHNESS, 0.0D, equippable.slot()),
                durabilityFraction(stack), hasBindingCurse(stack)));
    }

    private static Map<ArmorSlot, Integer> armorDestinationSlots() {
        return Map.of(
                ArmorSlot.FEET, InventoryMenu.ARMOR_SLOT_START,
                ArmorSlot.LEGS, InventoryMenu.ARMOR_SLOT_START + 1,
                ArmorSlot.CHEST, InventoryMenu.ARMOR_SLOT_START + 2,
                ArmorSlot.HEAD, InventoryMenu.ARMOR_SLOT_START + 3
        );
    }

    private static EquipmentSlot minecraftArmorSlot(ArmorSlot slot) {
        return switch (slot) {
            case FEET -> EquipmentSlot.FEET;
            case LEGS -> EquipmentSlot.LEGS;
            case CHEST -> EquipmentSlot.CHEST;
            case HEAD -> EquipmentSlot.HEAD;
        };
    }

    private static ArmorSlot armorSlot(EquipmentSlot slot) {
        return switch (slot) {
            case FEET -> ArmorSlot.FEET;
            case LEGS -> ArmorSlot.LEGS;
            case CHEST -> ArmorSlot.CHEST;
            case HEAD -> ArmorSlot.HEAD;
            default -> null;
        };
    }

    private static boolean hasBindingCurse(ItemStack stack) {
        return stack.getEnchantments().keySet().stream()
                .anyMatch(enchantment -> enchantment.unwrapKey().map(Enchantments.BINDING_CURSE::equals).orElse(false));
    }

    private static int remainingDurability(ItemStack stack) {
        return stack.isDamageableItem() ? Math.max(0, stack.getMaxDamage() - stack.getDamageValue()) : 0;
    }

    private static double durabilityFraction(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() < 1) {
            return 1.0D;
        }
        return Math.max(0.0D, (double) remainingDurability(stack) / stack.getMaxDamage());
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** Evaluates local incoming-message policies through normal module failure isolation. */
    private boolean allowIncomingMessage(ChatMute chatMute, ChatFilter chatFilter, AntiSpam antiSpam, MentionNotifier mentionNotifier,
                                         AutoReply autoReply, MinecraftChatSender normalChatSender,
                                         IncomingChatMessage message) {
        if (shouldHideIncoming(chatMute, "incoming-message", () -> chatMute.shouldHide(message))) {
            return false;
        }
        if (shouldHideIncoming(chatFilter, "incoming-message", () -> chatFilter.shouldHide(message))) {
            return false;
        }
        if (shouldHideIncoming(antiSpam, "incoming-message", () -> antiSpam.evaluate(message).shouldHide())) {
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
            chatHistory.saveIfNeeded();
        } catch (ConfigurationException exception) {
            LOGGER.log(Level.WARNING, "Unable to save local chat history while stopping", exception);
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
