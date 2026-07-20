package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.input.KeybindInputConsumer;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringListSetting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Minecraft-free Domain Expansion state machine. The version-bound adapter supplies observations
 * and performs each returned placement as an ordinary, individually validated block interaction.
 */
public final class DomainExpansion extends Module implements KeybindInputConsumer {
    private static final int CONFIRMATION_TIMEOUT_TICKS = 4;

    public enum State {
        IDLE,
        ARMED,
        ACQUIRE_TARGET,
        CALCULATE_BOUNDS,
        VALIDATE_RESOURCES,
        GENERATE_PLACEMENT_PLAN,
        PLACE_WALLS,
        PLACE_ROOF,
        PLACE_FLOOR,
        VERIFY_STRUCTURE,
        COMPLETE,
        CANCELLED
    }

    public enum ActivationMode {
        MANUAL,
        AUTOMATIC_PROXIMITY
    }

    public enum RotationMode {
        NONE,
        VISIBLE,
        SILENT
    }

    public enum TargetMovement {
        STATIC,
        RECALCULATE_BEFORE_FOUNDATION
    }

    public enum PlacementStatus {
        PENDING,
        REQUESTED,
        PLACED,
        EXISTING,
        INVALID,
        FAILED
    }

    public enum CancelReason {
        NONE,
        MANUAL,
        NO_TARGET,
        LOCAL_PLAYER_DIED,
        TARGET_DIED,
        TARGET_DISCONNECTED,
        DIMENSIONS_EXCEEDED,
        INSUFFICIENT_BLOCKS,
        NO_VALID_BLOCKS,
        CHUNK_UNLOADED,
        LOCAL_PLAYER_ESCAPED,
        TARGET_ESCAPED,
        INVENTORY_CONFLICT,
        SAFETY_REQUEST,
        MAXIMUM_PLACEMENTS,
        STRUCTURE_INCOMPLETE
    }

    public interface WorldView {
        boolean loaded(DomainPosition position);

        boolean solid(DomainPosition position);

        boolean replaceable(DomainPosition position);

        boolean liquid(DomainPosition position);

        boolean supported(DomainPosition position);

        boolean reachable(DomainPosition position);

        boolean intersectsProtectedEntity(DomainPosition position);
    }

    public record Context(
            long tick,
            boolean localPlayerAlive,
            DomainPosition localFeet,
            List<DomainTarget> targets,
            int availableBlocks,
            boolean safetyCancellation,
            boolean inventoryConflict,
            WorldView world
    ) {
        public Context {
            if (tick < 0L || availableBlocks < 0) {
                throw new IllegalArgumentException("Domain context counters are invalid");
            }
            localFeet = Objects.requireNonNull(localFeet, "localFeet");
            targets = List.copyOf(Objects.requireNonNull(targets, "targets"));
            world = Objects.requireNonNull(world, "world");
        }
    }

    public record TickResult(
            List<DomainPosition> placements,
            boolean disableRequested,
            boolean completedNow,
            CancelReason cancelReason
    ) {
        public TickResult {
            placements = List.copyOf(Objects.requireNonNull(placements, "placements"));
            cancelReason = Objects.requireNonNull(cancelReason, "cancelReason");
        }

        public static TickResult idle() {
            return new TickResult(List.of(), false, false, CancelReason.NONE);
        }
    }

    public record RenderBlock(DomainPosition position, DomainPlacementPlan.Part part, PlacementStatus status) {
        public RenderBlock {
            position = Objects.requireNonNull(position, "position");
            part = Objects.requireNonNull(part, "part");
            status = Objects.requireNonNull(status, "status");
        }
    }

    public record RenderSnapshot(
            State state,
            String targetName,
            double targetX,
            double targetZ,
            DomainBounds bounds,
            List<RenderBlock> blocks,
            double completion,
            CancelReason cancelReason
    ) {
        public RenderSnapshot {
            state = Objects.requireNonNull(state, "state");
            targetName = Objects.requireNonNullElse(targetName, "");
            blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
            cancelReason = Objects.requireNonNull(cancelReason, "cancelReason");
        }
    }

    private final EnumSetting<ActivationMode> activationMode;
    private final NumberSetting targetRange;
    private final NumberSetting activationRange;
    private final IntegerSetting targetDelay;
    private final IntegerSetting activationCooldown;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final EnumSetting<DomainTargetSelector.Priority> targetPriority;
    private final IntegerSetting interiorPadding;
    private final IntegerSetting interiorHeight;
    private final IntegerSetting maximumWidth;
    private final IntegerSetting maximumLength;
    private final BooleanSetting buildRoof;
    private final BooleanSetting buildFloor;
    private final EnumSetting<DomainPlanGenerator.ExitMode> leaveSelfExit;
    private final BooleanSetting closeEscapeSideFirst;
    private final IntegerSetting blocksPerTick;
    private final IntegerSetting placementDelay;
    private final EnumSetting<RotationMode> rotationMode;
    private final BooleanSetting silentSwitch;
    private final BooleanSetting inventoryToHotbar;
    private final BooleanSetting requireEnoughBlocks;
    private final IntegerSetting minimumBlocksRequired;
    private final IntegerSetting maximumRetries;
    private final IntegerSetting maximumPlacements;
    private final BooleanSetting disableAfterCompletion;
    private final StringListSetting allowedBlocks;
    private final BooleanSetting preferBlastResistant;
    private final BooleanSetting allowFallingBlocks;
    private final BooleanSetting allowContainers;
    private final BooleanSetting allowLiquids;
    private final EnumSetting<TargetMovement> targetMovement;
    private final IntegerSetting recalculationLimit;
    private final IntegerSetting lockPlanAfterPlacements;
    private final BooleanSetting cancelIfTargetEscapes;
    private final BooleanSetting followTarget;
    private final BooleanSetting renderPlan;
    private final BooleanSetting renderThroughWalls;
    private final BooleanSetting renderRemainingOnly;
    private final BooleanSetting renderTarget;
    private final BooleanSetting showCompletion;

    private final DomainTargetSelector selector = new DomainTargetSelector();
    private final Map<DomainPosition, TrackedPlacement> tracked = new LinkedHashMap<>();
    private State state = State.IDLE;
    private CancelReason cancelReason = CancelReason.NONE;
    private DomainTarget target;
    private DomainPosition plannedTargetFeet;
    private DomainBounds bounds;
    private DomainPlacementPlan provisionalPlan;
    private List<DomainPosition> doorway = List.of();
    private long nextPlacementTick;
    private long terminalTick = -1L;
    private int attemptedPlacements;
    private int confirmedPlacements;
    private int recalculations;
    private boolean completionAnnounced;
    private boolean finalSealRequested;
    private boolean finalSealInputOwned;
    private Runnable cleanupHook = () -> {
    };

    public DomainExpansion() {
        super("domain_expansion", "Domain Expansion",
                "Builds one bounded combat arena around the local player and a nearby enemy using normal placements.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        activationMode = addSetting(new EnumSetting<>("activation_mode", "Activation Mode",
                "Activate on enable or remain armed for proximity.", ActivationMode.class, ActivationMode.MANUAL));
        targetRange = addSetting(new NumberSetting("target_range", "Target Range",
                "Maximum manual target distance.", 6.0D, 1.0D, 12.0D));
        activationRange = addSetting(new NumberSetting("activation_range", "Activation Range",
                "Automatic proximity activation radius.", 4.0D, 1.0D, 10.0D));
        targetDelay = addSetting(new IntegerSetting("target_delay", "Target Delay",
                "Ticks an automatic target must remain eligible.", 4, 0, 40));
        activationCooldown = addSetting(new IntegerSetting("activation_cooldown", "Activation Cooldown",
                "Per-target cooldown after an activation.", 100, 0, 1_200));
        players = addSetting(new BooleanSetting("players", "Players", "Allow non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Allow hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Allow passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never target locally listed friends.", true));
        targetPriority = addSetting(new EnumSetting<>("target_priority", "Target Priority",
                "Choose between nearest, lowest-health, and crosshair targets.",
                DomainTargetSelector.Priority.class, DomainTargetSelector.Priority.NEAREST));

        interiorPadding = addSetting(new IntegerSetting("interior_padding", "Interior Padding",
                "Empty blocks beyond both outermost player block positions.", 1, 0, 3));
        interiorHeight = addSetting(new IntegerSetting("interior_height", "Interior Height",
                "Empty vertical space between floor and roof.", 3, 2, 6));
        maximumWidth = addSetting(new IntegerSetting("maximum_width", "Maximum Width",
                "Maximum outside width including both walls.", 10, 3, 16));
        maximumLength = addSetting(new IntegerSetting("maximum_length", "Maximum Length",
                "Maximum outside length including both walls.", 10, 3, 16));
        buildRoof = addSetting(new BooleanSetting("build_roof", "Build Roof",
                "Close the full roof after the walls.", true));
        buildFloor = addSetting(new BooleanSetting("build_floor", "Build Floor",
                "Fill the footprint beneath the arena after walls and roof.", false));
        leaveSelfExit = addSetting(new EnumSetting<>("leave_self_exit", "Leave Self Exit",
                "Leave no exit, a one/two-block doorway, or a doorway awaiting manual final seal.",
                DomainPlanGenerator.ExitMode.class, DomainPlanGenerator.ExitMode.NO_EXIT));
        closeEscapeSideFirst = addSetting(new BooleanSetting("close_escape_side_first", "Close Escape Side First",
                "Prioritize the wall in the target's probable movement or facing direction.", true));

        blocksPerTick = addSetting(new IntegerSetting("blocks_per_tick", "Blocks Per Tick",
                "Maximum ordinary placement interactions requested in one placement cycle.", 2, 1, 6));
        placementDelay = addSetting(new IntegerSetting("placement_delay", "Placement Delay",
                "Ticks between placement cycles.", 1, 0, 20));
        rotationMode = addSetting(new EnumSetting<>("rotation_mode", "Rotation Mode",
                "Do not rotate, visibly face support, or send a normal silent rotation.",
                RotationMode.class, RotationMode.VISIBLE));
        silentSwitch = addSetting(new BooleanSetting("silent_switch", "Silent Switch",
                "Temporarily select and restore the chosen hotbar block for each interaction.", true));
        inventoryToHotbar = addSetting(new BooleanSetting("inventory_to_hotbar", "Inventory To Hotbar",
                "Allow ordinary inventory-menu swaps into an available hotbar slot.", false));
        requireEnoughBlocks = addSetting(new BooleanSetting("require_enough_blocks", "Require Enough Blocks",
                "Abort unless allowed inventory blocks can fill every missing required position.", false));
        minimumBlocksRequired = addSetting(new IntegerSetting("minimum_blocks_required", "Minimum Blocks Required",
                "Minimum allowed blocks required before any activation.", 16, 1, 512));
        maximumRetries = addSetting(new IntegerSetting("maximum_retries", "Maximum Retries",
                "Retries after a rejected or unconfirmed placement.", 2, 0, 8));
        maximumPlacements = addSetting(new IntegerSetting("maximum_placements", "Maximum Placements",
                "Hard cap on ordinary interaction attempts per activation.", 256, 1, 1_024));
        disableAfterCompletion = addSetting(new BooleanSetting("disable_after_completion", "Disable After Completion",
                "Toggle off after a completed manual or automatic enclosure.", true));
        allowedBlocks = addSetting(new StringListSetting("allowed_blocks", "Allowed Blocks",
                "Priority-ordered block IDs; '*' permits other full solid blocks as the final fallback.",
                List.of("minecraft:obsidian", "minecraft:crying_obsidian", "minecraft:ender_chest",
                        "minecraft:cobblestone", "*"), 64, 128, false));
        preferBlastResistant = addSetting(new BooleanSetting("prefer_blast_resistant", "Prefer Blast Resistant",
                "Prefer more blast-resistant candidates within the configured priority.", true));
        allowFallingBlocks = addSetting(new BooleanSetting("allow_falling_blocks", "Allow Falling Blocks",
                "Allow gravity-affected block items.", false));
        allowContainers = addSetting(new BooleanSetting("allow_containers", "Allow Containers",
                "Allow container blocks such as Ender Chests.", false));
        allowLiquids = addSetting(new BooleanSetting("allow_liquids", "Replace Liquids",
                "Permit a normal placement interaction at replaceable liquid positions.", false));

        targetMovement = addSetting(new EnumSetting<>("target_movement", "Target Movement",
                "Keep static bounds or recalculate a limited number of times before the plan locks.",
                TargetMovement.class, TargetMovement.STATIC));
        recalculationLimit = addSetting(new IntegerSetting("recalculation_limit", "Recalculation Limit",
                "Maximum early plan recalculations.", 2, 0, 8));
        lockPlanAfterPlacements = addSetting(new IntegerSetting("lock_plan_after_placements",
                "Lock Plan After Placements", "Lock bounds after this many confirmed module placements.", 8, 0, 64));
        cancelIfTargetEscapes = addSetting(new BooleanSetting("cancel_if_target_escapes",
                "Cancel If Target Escapes", "Cancel a locked/static plan after the target leaves its interior.", true));
        followTarget = addSetting(new BooleanSetting("follow_target", "Follow Target",
                "Allow limited early recalculation when a target crosses the planned boundary.", false));

        renderPlan = addSetting(new BooleanSetting("render_plan", "Render Plan",
                "Render planned enclosure positions.", true));
        renderThroughWalls = addSetting(new BooleanSetting("render_through_walls", "Render Through Walls",
                "Draw the local plan on top of intervening terrain.", true));
        renderRemainingOnly = addSetting(new BooleanSetting("render_remaining_only", "Render Remaining Only",
                "Hide successfully placed and existing plan positions.", false));
        renderTarget = addSetting(new BooleanSetting("render_target", "Render Target",
                "Show the selected target name with the plan.", true));
        showCompletion = addSetting(new BooleanSetting("show_completion", "Show Completion",
                "Show target, state, and completion percentage on the HUD.", true));
    }

    public TickResult tick(Context context) {
        Objects.requireNonNull(context, "context");
        if (!isEnabled() || state == State.IDLE) {
            return TickResult.idle();
        }
        if (context.safetyCancellation()) {
            return cancel(context.tick(), CancelReason.SAFETY_REQUEST);
        }
        if (context.inventoryConflict()) {
            return cancel(context.tick(), CancelReason.INVENTORY_CONFLICT);
        }
        if (!context.localPlayerAlive()) {
            return cancel(context.tick(), CancelReason.LOCAL_PLAYER_DIED);
        }
        if (state == State.COMPLETE || state == State.CANCELLED) {
            return handleTerminal(context.tick());
        }

        boolean completedNow = false;
        for (int transition = 0; transition < 8; transition++) {
            switch (state) {
                case ARMED -> {
                    Optional<DomainTarget> selected = selector.select(context.tick(), context.targets(),
                            activationRange.value(), targetDelay.value(), targetOptions(),
                            targetPriority.value(), true);
                    if (selected.isEmpty()) {
                        return TickResult.idle();
                    }
                    target = selected.get();
                    selector.coolDown(target.id(), context.tick(), activationCooldown.value());
                    state = State.CALCULATE_BOUNDS;
                }
                case ACQUIRE_TARGET -> {
                    Optional<DomainTarget> selected = selector.select(context.tick(), context.targets(),
                            targetRange.value(), 0, targetOptions(), targetPriority.value(), false);
                    if (selected.isEmpty()) {
                        return cancel(context.tick(), CancelReason.NO_TARGET);
                    }
                    target = selected.get();
                    state = State.CALCULATE_BOUNDS;
                }
                case CALCULATE_BOUNDS -> {
                    Optional<DomainBounds> calculated = DomainBoundsCalculator.calculate(
                            context.localFeet(), target.feet(), interiorPadding.value(), interiorHeight.value(),
                            maximumWidth.value(), maximumLength.value());
                    if (calculated.isEmpty()) {
                        return cancel(context.tick(), CancelReason.DIMENSIONS_EXCEEDED);
                    }
                    bounds = calculated.get();
                    plannedTargetFeet = target.feet();
                    provisionalPlan = createPlan(context.localFeet(), target, bounds);
                    state = State.VALIDATE_RESOURCES;
                }
                case VALIDATE_RESOURCES -> {
                    int missing = missingBlocks(provisionalPlan, context.world());
                    if (context.availableBlocks() < minimumBlocksRequired.value()
                            || requireEnoughBlocks.value() && context.availableBlocks() < missing) {
                        return cancel(context.tick(), CancelReason.INSUFFICIENT_BLOCKS);
                    }
                    state = State.GENERATE_PLACEMENT_PLAN;
                }
                case GENERATE_PLACEMENT_PLAN -> {
                    installPlan(provisionalPlan, context.world(), true);
                    state = State.PLACE_WALLS;
                }
                case PLACE_WALLS, PLACE_ROOF, PLACE_FLOOR -> {
                    PlacementCycle cycle = placementCycle(context);
                    if (cycle.cancelReason() != CancelReason.NONE) {
                        return cancel(context.tick(), cycle.cancelReason());
                    }
                    if (!cycle.placements().isEmpty()) {
                        return new TickResult(cycle.placements(), false, false, CancelReason.NONE);
                    }
                    if (state == State.VERIFY_STRUCTURE) {
                        continue;
                    }
                    return TickResult.idle();
                }
                case VERIFY_STRUCTURE -> {
                    refreshWorldStatuses(context);
                    if (tracked.values().stream().allMatch(TrackedPlacement::complete)) {
                        state = State.COMPLETE;
                        terminalTick = context.tick();
                        completionAnnounced = true;
                        completedNow = true;
                        return new TickResult(List.of(), disableAfterCompletion.value(), true, CancelReason.NONE);
                    }
                    return cancel(context.tick(), CancelReason.STRUCTURE_INCOMPLETE);
                }
                case COMPLETE, CANCELLED -> {
                    return new TickResult(List.of(), disableAfterCompletion.value() && state == State.COMPLETE,
                            completedNow, cancelReason);
                }
                case IDLE -> {
                    return TickResult.idle();
                }
            }
        }
        return TickResult.idle();
    }

    /** Records the immediate result of one ordinary Minecraft interaction returned by {@link #tick(Context)}. */
    public void recordPlacementAttempt(DomainPosition position, boolean interactionAccepted, long tick) {
        TrackedPlacement placement = tracked.get(Objects.requireNonNull(position, "position"));
        if (placement == null || placement.status != PlacementStatus.PENDING || tick < 0L) {
            return;
        }
        attemptedPlacements++;
        if (interactionAccepted) {
            placement.status = PlacementStatus.REQUESTED;
            placement.requestTick = tick;
            placement.deferrals = 0;
        } else {
            reject(placement);
        }
    }

    /** Requests the deferred two-block doorway seal after a Manual Final Seal plan has completed. */
    public boolean requestFinalSeal() {
        if (!isEnabled() || state != State.COMPLETE
                || leaveSelfExit.value() != DomainPlanGenerator.ExitMode.MANUAL_FINAL_SEAL
                || doorway.isEmpty() || finalSealRequested) {
            return false;
        }
        for (DomainPosition position : doorway) {
            tracked.put(position, new TrackedPlacement(
                    new DomainPlacementPlan.Entry(position, DomainPlacementPlan.Part.WALL),
                    PlacementStatus.PENDING));
        }
        finalSealRequested = true;
        finalSealInputOwned = true;
        completionAnnounced = false;
        state = State.PLACE_WALLS;
        cancelReason = CancelReason.NONE;
        return true;
    }

    public void setCleanupHook(Runnable cleanupHook) {
        this.cleanupHook = Objects.requireNonNull(cleanupHook, "cleanupHook");
    }

    public State state() {
        return state;
    }

    public CancelReason cancelReason() {
        return cancelReason;
    }

    public Optional<DomainBounds> bounds() {
        return Optional.ofNullable(bounds);
    }

    public String targetId() {
        return target == null ? "" : target.id();
    }

    public int attemptedPlacements() {
        return attemptedPlacements;
    }

    public double completion() {
        if (tracked.isEmpty()) {
            return 0.0D;
        }
        long complete = tracked.values().stream().filter(TrackedPlacement::complete).count();
        return (double) complete / tracked.size();
    }

    public RenderSnapshot renderSnapshot() {
        List<RenderBlock> blocks = new ArrayList<>(tracked.size());
        for (TrackedPlacement placement : tracked.values()) {
            PlacementStatus renderStatus = placement.status == PlacementStatus.PENDING && placement.deferrals > 0
                    ? PlacementStatus.INVALID : placement.status;
            blocks.add(new RenderBlock(placement.entry.position(), placement.entry.part(), renderStatus));
        }
        return new RenderSnapshot(state, target == null ? "" : target.name(),
                target == null ? 0.0D : target.x(), target == null ? 0.0D : target.z(),
                bounds, blocks, completion(), cancelReason);
    }

    public ActivationMode activationMode() {
        return activationMode.value();
    }

    public RotationMode rotationMode() {
        return rotationMode.value();
    }

    public List<String> allowedBlocks() {
        return allowedBlocks.value();
    }

    public boolean preferBlastResistant() {
        return preferBlastResistant.value();
    }

    public boolean allowFallingBlocks() {
        return allowFallingBlocks.value();
    }

    public boolean allowContainers() {
        return allowContainers.value();
    }

    public boolean inventoryToHotbar() {
        return inventoryToHotbar.value();
    }

    public boolean silentSwitch() {
        return silentSwitch.value();
    }

    public boolean renderPlan() {
        return renderPlan.value();
    }

    public boolean renderThroughWalls() {
        return renderThroughWalls.value();
    }

    public boolean renderRemainingOnly() {
        return renderRemainingOnly.value();
    }

    public boolean renderTarget() {
        return renderTarget.value();
    }

    public boolean showCompletion() {
        return showCompletion.value();
    }

    public boolean completionAnnounced() {
        return completionAnnounced;
    }

    public void releaseFinalSealInput() {
        finalSealInputOwned = false;
    }

    @Override
    public boolean consumesKeybindInput() {
        return isEnabled() && (finalSealInputOwned || state == State.COMPLETE
                && leaveSelfExit.value() == DomainPlanGenerator.ExitMode.MANUAL_FINAL_SEAL
                && !finalSealRequested);
    }

    @Override
    protected void onEnable() {
        resetSession(false);
        state = activationMode.value() == ActivationMode.AUTOMATIC_PROXIMITY
                ? State.ARMED : State.ACQUIRE_TARGET;
    }

    @Override
    protected void onDisable() {
        if (state != State.IDLE && state != State.COMPLETE && state != State.CANCELLED) {
            state = State.CANCELLED;
            cancelReason = CancelReason.MANUAL;
        }
        cleanupHook.run();
        resetSession(true);
    }

    private PlacementCycle placementCycle(Context context) {
        CancelReason safety = validateActiveContext(context);
        if (safety != CancelReason.NONE) {
            return PlacementCycle.cancel(safety);
        }
        refreshWorldStatuses(context);
        if (attemptedPlacements >= maximumPlacements.value()) {
            return PlacementCycle.cancel(CancelReason.MAXIMUM_PLACEMENTS);
        }
        if (context.availableBlocks() == 0 && tracked.values().stream().anyMatch(TrackedPlacement::unresolved)) {
            return PlacementCycle.cancel(CancelReason.NO_VALID_BLOCKS);
        }
        advancePhaseIfResolved();
        if (state == State.VERIFY_STRUCTURE || context.tick() < nextPlacementTick) {
            return PlacementCycle.empty();
        }

        List<DomainPosition> placements = new ArrayList<>();
        DomainPlacementPlan.Part[] parts = activeParts();
        for (TrackedPlacement placement : tracked.values()) {
            if (placement.status != PlacementStatus.PENDING || !includes(parts, placement.entry.part())) {
                continue;
            }
            DomainPosition position = placement.entry.position();
            if (!context.world().loaded(position)) {
                return PlacementCycle.cancel(CancelReason.CHUNK_UNLOADED);
            }
            if (context.world().solid(position)) {
                placement.status = PlacementStatus.EXISTING;
                continue;
            }
            if (!context.world().replaceable(position)
                    || context.world().liquid(position) && !allowLiquids.value()) {
                placement.status = PlacementStatus.FAILED;
                continue;
            }
            if (context.world().intersectsProtectedEntity(position) || !context.world().reachable(position)
                    || !context.world().supported(position)) {
                defer(placement);
                continue;
            }
            placements.add(position);
            if (placements.size() >= Math.min(blocksPerTick.value(),
                    maximumPlacements.value() - attemptedPlacements)) {
                break;
            }
        }
        if (!placements.isEmpty()) {
            nextPlacementTick = context.tick() + placementDelay.value();
        } else {
            advancePhaseIfResolved();
        }
        return new PlacementCycle(List.copyOf(placements), CancelReason.NONE);
    }

    private CancelReason validateActiveContext(Context context) {
        if (target == null || bounds == null) {
            return CancelReason.NO_TARGET;
        }
        DomainTarget current = context.targets().stream()
                .filter(candidate -> candidate.id().equals(target.id()))
                .findFirst().orElse(null);
        if (current == null) {
            return CancelReason.TARGET_DISCONNECTED;
        }
        if (!current.alive()) {
            return CancelReason.TARGET_DIED;
        }
        if (!current.loaded()) {
            return CancelReason.CHUNK_UNLOADED;
        }
        target = current;

        int confirmedWalls = (int) tracked.values().stream()
                .filter(placement -> placement.entry.part() == DomainPlacementPlan.Part.WALL)
                .filter(TrackedPlacement::complete).count();
        int wallCount = (int) tracked.values().stream()
                .filter(placement -> placement.entry.part() == DomainPlacementPlan.Part.WALL).count();
        if (!bounds.containsFeet(context.localFeet()) && confirmedWalls * 2 < Math.max(1, wallCount)) {
            return CancelReason.LOCAL_PLAYER_ESCAPED;
        }

        boolean targetMoved = !current.feet().equals(targetPlanFeet());
        boolean targetOutside = !bounds.containsFeet(current.feet());
        boolean mayRecalculate = targetMovement.value() == TargetMovement.RECALCULATE_BEFORE_FOUNDATION
                && recalculations < recalculationLimit.value()
                && modulePlacedCount() < lockPlanAfterPlacements.value()
                && (!targetOutside || followTarget.value());
        if (targetMoved && mayRecalculate && recalculate(context, current)) {
            return CancelReason.NONE;
        }
        if (targetOutside && cancelIfTargetEscapes.value()) {
            return CancelReason.TARGET_ESCAPED;
        }
        for (TrackedPlacement placement : tracked.values()) {
            if (!context.world().loaded(placement.entry.position())) {
                return CancelReason.CHUNK_UNLOADED;
            }
        }
        return CancelReason.NONE;
    }

    private DomainPosition targetPlanFeet() {
        return plannedTargetFeet == null ? new DomainPosition(0, 0, 0) : plannedTargetFeet;
    }

    private boolean recalculate(Context context, DomainTarget current) {
        Optional<DomainBounds> calculated = DomainBoundsCalculator.calculate(
                context.localFeet(), current.feet(), interiorPadding.value(), interiorHeight.value(),
                maximumWidth.value(), maximumLength.value());
        if (calculated.isEmpty()) {
            return false;
        }
        DomainPlacementPlan updated = createPlan(context.localFeet(), current, calculated.get());
        int missing = missingBlocks(updated, context.world());
        if (requireEnoughBlocks.value() && context.availableBlocks() < missing) {
            return false;
        }
        bounds = calculated.get();
        plannedTargetFeet = current.feet();
        provisionalPlan = updated;
        installPlan(updated, context.world(), false);
        recalculations++;
        state = State.PLACE_WALLS;
        return true;
    }

    private void refreshWorldStatuses(Context context) {
        for (TrackedPlacement placement : tracked.values()) {
            if (placement.complete() || placement.status == PlacementStatus.FAILED) {
                continue;
            }
            DomainPosition position = placement.entry.position();
            if (context.world().solid(position)) {
                if (placement.status == PlacementStatus.REQUESTED) {
                    placement.status = PlacementStatus.PLACED;
                    confirmedPlacements++;
                } else {
                    placement.status = PlacementStatus.EXISTING;
                }
                continue;
            }
            if (placement.status == PlacementStatus.REQUESTED
                    && context.tick() - placement.requestTick >= CONFIRMATION_TIMEOUT_TICKS) {
                reject(placement);
            }
        }
    }

    private void reject(TrackedPlacement placement) {
        placement.failures++;
        placement.status = placement.failures > maximumRetries.value()
                ? PlacementStatus.FAILED : PlacementStatus.PENDING;
        placement.requestTick = -1L;
    }

    private void defer(TrackedPlacement placement) {
        placement.deferrals++;
        if (placement.deferrals > (maximumRetries.value() + 1) * 20) {
            placement.status = PlacementStatus.FAILED;
        }
    }

    private void advancePhaseIfResolved() {
        boolean wallsResolved = phaseResolved(DomainPlacementPlan.Part.WALL);
        if (state == State.PLACE_WALLS && wallsResolved) {
            state = buildRoof.value() ? State.PLACE_ROOF
                    : buildFloor.value() ? State.PLACE_FLOOR : State.VERIFY_STRUCTURE;
        }
        if (state == State.PLACE_ROOF
                && phaseResolved(DomainPlacementPlan.Part.ROOF_PERIMETER, DomainPlacementPlan.Part.ROOF)) {
            state = buildFloor.value() ? State.PLACE_FLOOR : State.VERIFY_STRUCTURE;
        }
        if (state == State.PLACE_FLOOR && phaseResolved(DomainPlacementPlan.Part.FLOOR)) {
            state = State.VERIFY_STRUCTURE;
        }
    }

    private boolean phaseResolved(DomainPlacementPlan.Part... parts) {
        for (TrackedPlacement placement : tracked.values()) {
            if (includes(parts, placement.entry.part()) && placement.unresolved()) {
                return false;
            }
        }
        return true;
    }

    private DomainPlacementPlan.Part[] activeParts() {
        return switch (state) {
            case PLACE_WALLS -> new DomainPlacementPlan.Part[]{DomainPlacementPlan.Part.WALL};
            case PLACE_ROOF -> new DomainPlacementPlan.Part[]{
                    DomainPlacementPlan.Part.ROOF_PERIMETER, DomainPlacementPlan.Part.ROOF};
            case PLACE_FLOOR -> new DomainPlacementPlan.Part[]{DomainPlacementPlan.Part.FLOOR};
            default -> new DomainPlacementPlan.Part[0];
        };
    }

    private void installPlan(DomainPlacementPlan plan, WorldView world, boolean resetCounters) {
        tracked.clear();
        doorway = plan.doorway();
        for (DomainPlacementPlan.Entry entry : plan.entries()) {
            tracked.put(entry.position(), new TrackedPlacement(entry,
                    world.solid(entry.position()) ? PlacementStatus.EXISTING : PlacementStatus.PENDING));
        }
        if (resetCounters) {
            attemptedPlacements = 0;
            confirmedPlacements = 0;
        }
        nextPlacementTick = 0L;
    }

    private DomainPlacementPlan createPlan(DomainPosition localFeet, DomainTarget target, DomainBounds bounds) {
        return DomainPlanGenerator.generate(bounds, localFeet, target, buildRoof.value(), buildFloor.value(),
                leaveSelfExit.value(), closeEscapeSideFirst.value());
    }

    private DomainTargetSelector.Options targetOptions() {
        return new DomainTargetSelector.Options(
                players.value(), hostiles.value(), passive.value(), excludeFriends.value());
    }

    private static int missingBlocks(DomainPlacementPlan plan, WorldView world) {
        int missing = 0;
        for (DomainPlacementPlan.Entry entry : plan.entries()) {
            if (!world.solid(entry.position())) {
                missing++;
            }
        }
        return missing;
    }

    private TickResult cancel(long tick, CancelReason reason) {
        state = State.CANCELLED;
        cancelReason = reason;
        terminalTick = tick;
        if (target != null) {
            selector.coolDown(target.id(), tick, activationCooldown.value());
        }
        return new TickResult(List.of(), activationMode.value() == ActivationMode.MANUAL,
                false, reason);
    }

    private TickResult handleTerminal(long tick) {
        if (activationMode.value() == ActivationMode.AUTOMATIC_PROXIMITY
                && tick > terminalTick) {
            resetSession(false);
            state = State.ARMED;
        }
        return new TickResult(List.of(), state == State.COMPLETE && disableAfterCompletion.value(),
                false, cancelReason);
    }

    private int modulePlacedCount() {
        return confirmedPlacements;
    }

    private void resetSession(boolean resetSelector) {
        state = State.IDLE;
        cancelReason = CancelReason.NONE;
        target = null;
        plannedTargetFeet = null;
        bounds = null;
        provisionalPlan = null;
        doorway = List.of();
        tracked.clear();
        nextPlacementTick = 0L;
        terminalTick = -1L;
        attemptedPlacements = 0;
        confirmedPlacements = 0;
        recalculations = 0;
        completionAnnounced = false;
        finalSealRequested = false;
        finalSealInputOwned = false;
        if (resetSelector) {
            selector.reset();
        } else {
            selector.resetDwell();
        }
    }

    private static boolean includes(DomainPlacementPlan.Part[] parts, DomainPlacementPlan.Part part) {
        for (DomainPlacementPlan.Part candidate : parts) {
            if (candidate == part) {
                return true;
            }
        }
        return false;
    }

    private static final class TrackedPlacement {
        private final DomainPlacementPlan.Entry entry;
        private PlacementStatus status;
        private int failures;
        private int deferrals;
        private long requestTick = -1L;

        private TrackedPlacement(DomainPlacementPlan.Entry entry, PlacementStatus status) {
            this.entry = Objects.requireNonNull(entry, "entry");
            this.status = Objects.requireNonNull(status, "status");
        }

        private boolean complete() {
            return status == PlacementStatus.PLACED || status == PlacementStatus.EXISTING;
        }

        private boolean unresolved() {
            return status == PlacementStatus.PENDING || status == PlacementStatus.REQUESTED;
        }
    }

    private record PlacementCycle(List<DomainPosition> placements, CancelReason cancelReason) {
        private static PlacementCycle empty() {
            return new PlacementCycle(List.of(), CancelReason.NONE);
        }

        private static PlacementCycle cancel(CancelReason reason) {
            return new PlacementCycle(List.of(), reason);
        }
    }
}
