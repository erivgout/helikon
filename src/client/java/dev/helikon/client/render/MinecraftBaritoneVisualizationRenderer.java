package dev.helikon.client.render;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalInverted;
import baritone.api.pathing.goals.GoalYLevel;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.interfaces.IGoalRenderPos;
import baritone.utils.GuiClick;
import dev.helikon.client.module.world.BaritoneNavigation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.List;
import java.util.Set;

/**
 * Renders Baritone state through Minecraft 26.2's supported gizmo phase.
 * This replaces Baritone's pre-26.2 direct buffer renderer.
 */
public final class MinecraftBaritoneVisualizationRenderer {
    private static final int MAXIMUM_PATH_SEGMENTS = 512;
    private static final int MAXIMUM_GOAL_MARKERS = 128;
    private static final int MAXIMUM_ACTION_MARKERS = 256;
    private static final int MAXIMUM_SELECTIONS = 128;
    private static final double SELECTION_BOX_EXPANSION = 0.005D;
    private static volatile long renderInvocations;
    private static volatile int lastSubmittedPathPositions;
    private static volatile int lastSubmittedGoalMarkers;
    private static volatile int lastSubmittedSelections;
    private static volatile boolean lastSubmittedClickHover;
    private static volatile boolean lastSubmittedClickDrag;

    private MinecraftBaritoneVisualizationRenderer() {
    }

    public static void render(BaritoneNavigation module) {
        if (!module.isEnabled()) {
            return;
        }
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        IPathingBehavior behavior = baritone.getPathingBehavior();
        renderInvocations++;
        lastSubmittedPathPositions = pathPositionCount(behavior.getCurrent())
                + pathPositionCount(behavior.getNext());
        lastSubmittedGoalMarkers = countRenderableGoalMarkers(behavior.getGoal(), MAXIMUM_GOAL_MARKERS);
        if (module.rendersPath()) {
            IPathExecutor current = behavior.getCurrent();
            IPathExecutor next = behavior.getNext();
            renderPath(module, current);
            renderPath(module, next);
            behavior.getInProgress().ifPresent(finder -> {
                finder.bestPathSoFar().ifPresent(path -> renderPath(module, path, 0));
                finder.pathToMostRecentNodeConsidered().ifPresent(path -> renderPath(module, path, 0));
            });
        }
        IPathExecutor current = behavior.getCurrent();
        if (module.rendersActions() && current != null) {
            renderBlocks(module, current.toBreak(), module.breakColor());
            renderBlocks(module, current.toPlace(), module.placeColor());
            renderBlocks(module, current.toWalkInto(), module.walkIntoColor());
        }
        if (module.rendersGoal()) {
            renderGoal(module, behavior.getGoal(), new MarkerBudget(MAXIMUM_GOAL_MARKERS));
        }
    }

    /**
     * Submits selection and #click previews to Minecraft's per-frame gizmo
     * collector. Interactive mouse state changes between client ticks and
     * must not rely on the retained 20 Hz path snapshot.
     */
    public static void renderInteractiveState(BaritoneNavigation module) {
        if (!module.isEnabled()) {
            return;
        }
        lastSubmittedSelections = renderSelections(BaritoneAPI.getProvider().getPrimaryBaritone());
        renderClickPreview();
    }

    private static int renderSelections(IBaritone baritone) {
        Settings settings = BaritoneAPI.getSettings();
        if (!settings.renderSelection.value) {
            return 0;
        }
        ISelection[] selections = baritone.getSelectionManager().getSelections();
        int count = Math.min(selections.length, MAXIMUM_SELECTIONS);
        int selectionColor = withOpacity(settings.colorSelection.value, settings.selectionOpacity.value);
        int positionOneColor = withOpacity(settings.colorSelectionPos1.value, settings.selectionOpacity.value);
        int positionTwoColor = withOpacity(settings.colorSelectionPos2.value, settings.selectionOpacity.value);
        float lineWidth = settings.selectionLineWidth.value;
        boolean alwaysOnTop = settings.renderSelectionIgnoreDepth.value;

        for (int index = 0; index < count; index++) {
            ISelection selection = selections[index];
            renderSelectionBox(selection.aabb().inflate(SELECTION_BOX_EXPANSION), selectionColor, lineWidth,
                    alwaysOnTop);
            if (settings.renderSelectionCorners.value) {
                renderSelectionBox(new AABB(selection.pos1()).inflate(SELECTION_BOX_EXPANSION),
                        positionOneColor, lineWidth, alwaysOnTop);
                renderSelectionBox(new AABB(selection.pos2()).inflate(SELECTION_BOX_EXPANSION),
                        positionTwoColor, lineWidth, alwaysOnTop);
            }
        }
        return count;
    }

    private static void renderClickPreview() {
        lastSubmittedClickHover = false;
        lastSubmittedClickDrag = false;
        if (!(Minecraft.getInstance().gui.screen() instanceof GuiClick click)) {
            return;
        }
        click.updateHoveredBlock(Minecraft.getInstance().gameRenderer.mainCamera());
        BlockPos hovered = click.hoveredBlock();
        if (hovered != null) {
            lastSubmittedClickHover = true;
            renderSelectionBox(new AABB(hovered).inflate(0.01D), 0xFF00FFFF, 2.0F, true);
        }
        AABB preview = click.selectionPreviewBounds();
        if (preview != null) {
            lastSubmittedClickDrag = true;
            renderSelectionBox(preview.inflate(SELECTION_BOX_EXPANSION), 0xFFFF5252, 2.0F, true);
        }
    }

    private static void renderSelectionBox(AABB bounds, int color, float lineWidth, boolean alwaysOnTop) {
        GizmoProperties marker = Gizmos.cuboid(bounds, GizmoStyle.stroke(color, lineWidth));
        if (alwaysOnTop) {
            marker.setAlwaysOnTop();
        }
    }

    static int withOpacity(Color color, float opacity) {
        int alpha = Math.clamp(Math.round(opacity * 255.0F), 0, 255);
        return alpha << 24 | color.getRGB() & 0x00FFFFFF;
    }

    /** Read-only live counts used by the in-engine acceptance test and diagnostics. */
    public static VisualizationState inspect() {
        IPathingBehavior behavior = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior();
        IPathExecutor current = behavior.getCurrent();
        IPathExecutor next = behavior.getNext();
        int pathPositions = pathPositionCount(current) + pathPositionCount(next);
        if (pathPositions == 0) {
            pathPositions = behavior.getInProgress()
                    .flatMap(finder -> finder.bestPathSoFar().or(finder::pathToMostRecentNodeConsidered))
                    .map(path -> path.positions().size())
                    .orElse(0);
        }
        int actionMarkers = current == null ? 0
                : current.toBreak().size() + current.toPlace().size() + current.toWalkInto().size();
        return new VisualizationState(pathPositions,
                countRenderableGoalMarkers(behavior.getGoal(), MAXIMUM_GOAL_MARKERS), actionMarkers,
                renderInvocations, lastSubmittedPathPositions, lastSubmittedGoalMarkers);
    }

    /** Read-only state proving that the live in-game Gizmo phase submitted click/selection geometry. */
    public static InteractiveVisualizationState inspectInteractive() {
        return new InteractiveVisualizationState(
                lastSubmittedSelections, lastSubmittedClickHover, lastSubmittedClickDrag
        );
    }

    private static void renderPath(BaritoneNavigation module, IPathExecutor executor) {
        if (executor == null || executor.getPath() == null) {
            return;
        }
        renderPath(module, executor.getPath(), executor.getPosition());
    }

    private static int pathPositionCount(IPathExecutor executor) {
        return executor == null || executor.getPath() == null ? 0 : executor.getPath().positions().size();
    }

    private static void renderPath(BaritoneNavigation module, IPath path, int pathPosition) {
        List<BetterBlockPos> positions = path.positions();
        if (positions.size() < 2) {
            return;
        }
        int first = Math.clamp(pathPosition, 0, Math.max(0, positions.size() - 1));
        int end = Math.min(positions.size() - 1, first + MAXIMUM_PATH_SEGMENTS);
        for (int index = first; index < end; index++) {
            GizmoProperties line = Gizmos.line(center(positions.get(index)), center(positions.get(index + 1)),
                    module.pathColor(), module.lineWidth());
            applyDepth(module, line);
            Vec3 point = center(positions.get(index));
            applyDepth(module, Gizmos.line(point.add(0.0D, -0.22D, 0.0D), point.add(0.0D, 0.22D, 0.0D),
                    module.pathColor(), module.lineWidth()));
            BetterBlockPos node = positions.get(index);
            AABB ribbon = new AABB(node.x + 0.12D, node.y + 0.035D, node.z + 0.12D,
                    node.x + 0.88D, node.y + 0.085D, node.z + 0.88D);
            applyDepth(module, Gizmos.cuboid(ribbon, GizmoStyle.strokeAndFill(
                    module.pathColor(), module.lineWidth(), translucent(module.pathColor(), 0x48))));
        }
    }

    private static void renderGoal(BaritoneNavigation module, Goal goal, MarkerBudget budget) {
        if (goal == null || budget.exhausted()) {
            return;
        }
        if (goal instanceof GoalComposite composite) {
            for (Goal child : composite.goals()) {
                renderGoal(module, child, budget);
                if (budget.exhausted()) {
                    return;
                }
            }
            return;
        }
        if (goal instanceof GoalInverted inverted) {
            renderGoal(module, inverted.origin, budget);
            return;
        }
        if (goal instanceof IGoalRenderPos positioned) {
            if (!budget.take()) {
                return;
            }
            BlockPos position = positioned.getGoalPos();
            renderBox(module, position, module.goalColor());
            Vec3 center = Vec3.atCenterOf(position);
            applyDepth(module, Gizmos.line(center, center.add(0.0D, 3.0D, 0.0D),
                    module.goalColor(), module.lineWidth()));
            return;
        }
        if (goal instanceof GoalXZ xz) {
            if (!budget.take()) {
                return;
            }
            double y = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().playerFeet().y;
            Vec3 start = new Vec3(xz.getX() + 0.5D, y - 32.0D, xz.getZ() + 0.5D);
            Vec3 end = new Vec3(xz.getX() + 0.5D, y + 32.0D, xz.getZ() + 0.5D);
            applyDepth(module, Gizmos.line(start, end, module.goalColor(), module.lineWidth()));
            return;
        }
        if (goal instanceof GoalYLevel yLevel && budget.take()) {
            BetterBlockPos feet = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().playerFeet();
            AABB level = new AABB(feet.x - 16.0D, yLevel.level, feet.z - 16.0D,
                    feet.x + 16.0D, yLevel.level + 0.04D, feet.z + 16.0D);
            GizmoProperties marker = Gizmos.cuboid(level, GizmoStyle.stroke(module.goalColor(), module.lineWidth()));
            applyDepth(module, marker);
        }
    }

    private static void renderBlocks(BaritoneNavigation module, Set<BlockPos> positions, int color) {
        int rendered = 0;
        for (BlockPos position : positions) {
            renderBox(module, position, color);
            if (++rendered >= MAXIMUM_ACTION_MARKERS) {
                return;
            }
        }
    }

    private static void renderBox(BaritoneNavigation module, BlockPos position, int color) {
        GizmoProperties marker = Gizmos.cuboid(new AABB(position).inflate(0.03D),
                GizmoStyle.strokeAndFill(color, module.lineWidth(), translucent(color, 0x38)));
        applyDepth(module, marker);
    }

    private static int translucent(int color, int alpha) {
        return Math.clamp(alpha, 1, 255) << 24 | color & 0x00FFFFFF;
    }

    private static Vec3 center(BetterBlockPos position) {
        return new Vec3(position.x + 0.5D, position.y + 0.5D, position.z + 0.5D);
    }

    private static void applyDepth(BaritoneNavigation module, GizmoProperties properties) {
        if (module.rendersThroughWalls()) {
            properties.setAlwaysOnTop();
        }
    }

    static int countRenderableGoalMarkers(Goal goal, int maximum) {
        if (goal == null || maximum <= 0) {
            return 0;
        }
        if (goal instanceof GoalComposite composite) {
            int count = 0;
            for (Goal child : composite.goals()) {
                count += countRenderableGoalMarkers(child, maximum - count);
                if (count >= maximum) {
                    break;
                }
            }
            return count;
        }
        if (goal instanceof GoalInverted inverted) {
            return countRenderableGoalMarkers(inverted.origin, maximum);
        }
        return goal instanceof IGoalRenderPos || goal instanceof GoalXZ || goal instanceof GoalYLevel ? 1 : 0;
    }

    private static final class MarkerBudget {
        private int remaining;

        private MarkerBudget(int remaining) {
            this.remaining = remaining;
        }

        private boolean take() {
            if (remaining <= 0) {
                return false;
            }
            remaining--;
            return true;
        }

        private boolean exhausted() {
            return remaining <= 0;
        }
    }

    public record VisualizationState(
            int pathPositions,
            int goalMarkers,
            int actionMarkers,
            long renderInvocations,
            int lastSubmittedPathPositions,
            int lastSubmittedGoalMarkers
    ) {
    }

    public record InteractiveVisualizationState(
            int selections,
            boolean clickHover,
            boolean clickDrag
    ) {
    }
}
