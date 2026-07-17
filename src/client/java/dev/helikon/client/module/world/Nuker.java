package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.player.ToolCandidate;
import dev.helikon.client.module.player.ToolSelection;
import dev.helikon.client.render.BlockIdList;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/** Selects a small, explicitly whitelisted set of ordinary local block-destroy requests. */
public final class Nuker extends Module {
    public static final int HARD_MAXIMUM_ACTIONS_PER_TICK = 2;

    public record Context(boolean screenOpen, boolean attackHeld) {
    }

    public record Target(int x, int y, int z, String blockId, double squaredDistance, boolean lineOfSight) {
        public Target {
            if (blockId == null || blockId.isBlank() || !Double.isFinite(squaredDistance) || squaredDistance < 0.0D) {
                throw new IllegalArgumentException("target facts are invalid");
            }
        }
    }

    public enum ToolActionType {
        NONE,
        SELECT,
        RESTORE
    }

    public record ToolAction(ToolActionType type, int slot) {
        private static final ToolAction NONE = new ToolAction(ToolActionType.NONE, -1);

        public static ToolAction none() {
            return NONE;
        }
    }

    private final NumberSetting radius;
    private final NumberSetting blocksPerTick;
    private final StringSetting whitelist;
    private final StringSetting blacklist;
    private final BooleanSetting toolSelection;
    private final NumberSetting minimumToolDurability;
    private final BooleanSetting lineOfSight;
    private final BooleanSetting rotate;
    private final NumberSetting safetyLimit;
    private int priorSlot = -1;
    private int selectedSlot = -1;
    private boolean restoreRequested;
    private boolean selectionSuspended;
    private Set<String> allowedBlocks;
    private Set<String> blockedBlocks;

    public Nuker() {
        super("nuker", "Nuker", "Makes a bounded set of ordinary destroy requests for explicitly whitelisted blocks.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        radius = addSetting(new NumberSetting("radius", "Radius", "Loaded local block radius to consider.",
                2.0D, 1.0D, 4.0D));
        blocksPerTick = addSetting(new NumberSetting("blocks_per_tick", "Blocks per tick",
                "Maximum ordinary destroy requests considered each tick.", 1.0D, 1.0D, HARD_MAXIMUM_ACTIONS_PER_TICK));
        whitelist = addSetting(new StringSetting("whitelist", "Whitelist",
                "Required semicolon-separated block IDs; blank disables Nuker for safety.", "", 1_024, true));
        blacklist = addSetting(new StringSetting("blacklist", "Blacklist",
                "Semicolon-separated block IDs never selected locally.", "", 1_024, true));
        toolSelection = addSetting(new BooleanSetting("tool_selection", "Tool selection",
                "Select the best safe existing hotbar tool before a normal destroy request.", true));
        minimumToolDurability = addSetting(new NumberSetting("minimum_tool_durability", "Minimum tool durability",
                "Avoid tools with fewer remaining durability points than this local reserve.", 8.0D, 0.0D, 2_032.0D));
        lineOfSight = addSetting(new BooleanSetting("line_of_sight", "Line of sight",
                "Require a local block ray to reach each selected target.", true));
        rotate = addSetting(new BooleanSetting("rotate", "Rotate", "Turn the local view toward selected targets.", false));
        safetyLimit = addSetting(new NumberSetting("safety_limit", "Safety limit",
                "Hard local cap on ordinary destroy requests per tick.", 1.0D, 1.0D, HARD_MAXIMUM_ACTIONS_PER_TICK));
        refreshBlockFilters();
        whitelist.addChangeListener(ignored -> refreshBlockFilters());
        blacklist.addChangeListener(ignored -> refreshBlockFilters());
    }

    /** Filters and orders already-observed local targets without Minecraft dependencies. */
    public List<Target> selectTargets(Context context, List<Target> candidates) {
        if (context == null || candidates == null) {
            throw new IllegalArgumentException("nuker context and candidates must not be null");
        }
        if (!shouldScan(context)) {
            return List.of();
        }
        int requested = Math.min((int) Math.round(blocksPerTick.value()), (int) Math.round(safetyLimit.value()));
        int limit = Math.min(HARD_MAXIMUM_ACTIONS_PER_TICK, requested);
        double maximumDistance = radius.value() * radius.value();
        return candidates.stream()
                .filter(target -> target.squaredDistance() <= maximumDistance)
                .filter(target -> isConfiguredTarget(target.blockId()))
                .filter(target -> !lineOfSight.value() || target.lineOfSight())
                .sorted(Comparator.comparingDouble(Target::squaredDistance).thenComparing(Target::blockId)
                        .thenComparingInt(Target::x).thenComparingInt(Target::y).thenComparingInt(Target::z))
                .limit(limit)
                .toList();
    }

    /** Avoids even local world scanning until every explicit user-activation guard is satisfied. */
    public boolean shouldScan(Context context) {
        return context != null && isEnabled() && !context.screenOpen() && context.attackHeld() && !allowedBlocks.isEmpty();
    }

    /** Owns a temporary hotbar selection only while a safe target remains active. */
    public ToolAction toolAction(boolean targetActive, int currentSlot, List<ToolCandidate> candidates) {
        if (currentSlot < 0 || currentSlot >= 9 || candidates == null) {
            throw new IllegalArgumentException("tool facts are invalid");
        }
        if (restoreRequested) {
            restoreRequested = false;
            return releaseTool(currentSlot);
        }
        if (!isEnabled() || !targetActive || !toolSelection.value()) {
            return releaseTool(currentSlot);
        }
        if (selectedSlot >= 0 && currentSlot != selectedSlot) {
            clearToolOwnership();
            selectionSuspended = true;
            return ToolAction.none();
        }
        if (selectionSuspended) {
            return ToolAction.none();
        }
        OptionalInt best = ToolSelection.bestSlot(candidates, (int) Math.round(minimumToolDurability.value()));
        if (best.isEmpty() || best.getAsInt() == currentSlot) {
            return ToolAction.none();
        }
        if (priorSlot < 0) {
            priorSlot = currentSlot;
        }
        selectedSlot = best.getAsInt();
        return new ToolAction(ToolActionType.SELECT, selectedSlot);
    }

    public int radius() {
        return (int) Math.round(radius.value());
    }

    public boolean lineOfSightRequired() {
        return lineOfSight.value();
    }

    public boolean rotatesToTarget() {
        return rotate.value();
    }

    /** Lets the narrow scanner avoid retaining irrelevant targets before bounded local ray checks. */
    public boolean isConfiguredTarget(String blockId) {
        return blockId != null && allowedBlocks.contains(blockId) && !blockedBlocks.contains(blockId);
    }

    @Override
    protected void onDisable() {
        if (priorSlot >= 0) {
            restoreRequested = true;
        }
    }

    private ToolAction releaseTool(int currentSlot) {
        if (priorSlot < 0 || currentSlot != selectedSlot) {
            clearToolOwnership();
            return ToolAction.none();
        }
        int restoreSlot = priorSlot;
        clearToolOwnership();
        return new ToolAction(ToolActionType.RESTORE, restoreSlot);
    }

    private void clearToolOwnership() {
        priorSlot = -1;
        selectedSlot = -1;
        selectionSuspended = false;
    }

    private void refreshBlockFilters() {
        allowedBlocks = BlockIdList.parse(whitelist.value());
        blockedBlocks = BlockIdList.parse(blacklist.value());
    }
}
