package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.integration.BaritoneAccess;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.ActionSetting;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Objects;

/** Helikon-facing controls for the embedded Baritone pathfinding component. */
public final class BaritoneNavigation extends Module {
    private final BaritoneAccess access;
    private final BooleanSetting allowBreak;
    private final BooleanSetting allowPlace;
    private final BooleanSetting allowSprint;
    private final BooleanSetting allowParkour;
    private final BooleanSetting allowInventory;
    private final BooleanSetting hashCommands;
    private final BooleanSetting renderPath;
    private final BooleanSetting renderGoal;
    private final BooleanSetting renderActions;
    private final BooleanSetting renderThroughWalls;
    private final ColorSetting pathColor;
    private final ColorSetting goalColor;
    private final ColorSetting breakColor;
    private final ColorSetting placeColor;
    private final ColorSetting walkIntoColor;
    private final NumberSetting lineWidth;
    private final StringSetting destination;
    private final StringSetting mineBlocks;
    private final StringSetting command;

    public BaritoneNavigation(BaritoneAccess access) {
        super("baritone", "Baritone", "Embedded pathfinding, mining, exploration, following, farming, and building.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        this.access = Objects.requireNonNull(access, "access");
        allowBreak = booleanSetting("allow_break", "Allow breaking", "Permit normal block-breaking actions.", true);
        allowPlace = booleanSetting("allow_place", "Allow placing", "Permit normal held-block placement actions.", true);
        allowSprint = booleanSetting("allow_sprint", "Allow sprinting", "Sprint when the calculated route permits it.", true);
        allowParkour = booleanSetting("allow_parkour", "Allow parkour", "Permit longer jump movements in routes.", false);
        allowInventory = booleanSetting("allow_inventory", "Use inventory", "Permit Baritone inventory management.", true);
        hashCommands = booleanSetting("hash_commands", "# commands", "Accept Baritone's local # command prefix.", true);
        renderPath = booleanSetting("render_path", "Render path", "Draw the current route using 26.2 gizmos.", true);
        renderGoal = booleanSetting("render_goal", "Render goal", "Draw the active destination using 26.2 gizmos.", true);
        renderActions = booleanSetting("render_actions", "Render block actions",
                "Highlight blocks Baritone plans to break, place, or walk into.", true);
        renderThroughWalls = addSetting(new BooleanSetting("render_through_walls", "Render through walls",
                "Keep route, goal, and block-action markers visible behind blocks.", true));
        pathColor = addSetting(new ColorSetting("path_color", "Path color", "ARGB route color.", 0xFF4FC3F7));
        goalColor = addSetting(new ColorSetting("goal_color", "Goal color", "ARGB destination color.", 0xFFFFCA28));
        breakColor = addSetting(new ColorSetting("break_color", "Break target color",
                "ARGB color for blocks Baritone plans to mine or clear.", 0xFFFF5252));
        placeColor = addSetting(new ColorSetting("place_color", "Place target color",
                "ARGB color for blocks Baritone plans to place.", 0xFF69F0AE));
        walkIntoColor = addSetting(new ColorSetting("walk_into_color", "Walk-into color",
                "ARGB color for blocks Baritone plans to enter.", 0xFF40C4FF));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width", "Path and goal line width.",
                2.0D, 0.5D, 8.0D));
        destination = addSetting(new StringSetting("destination", "Destination",
                "Coordinates, waypoint, or block accepted by Baritone's goto command.", "0 64 0", 128, false));
        addSetting(new ActionSetting("go_to_destination", "Go to destination",
                "Runs goto with the Destination value. Enable Baritone first.", this::goToDestination));
        mineBlocks = addSetting(new StringSetting("mine_blocks", "Mine blocks",
                "Space-separated block identifiers accepted by Baritone's mine command.", "diamond_ore", 256, false));
        addSetting(new ActionSetting("mine_selected_blocks", "Mine selected blocks",
                "Runs mine with the Mine blocks value. Enable Baritone first.", this::mineSelectedBlocks));
        addSetting(new ActionSetting("pause_pathing", "Pause pathing",
                "Temporarily pauses the active Baritone process.", () -> runAction("pause")));
        addSetting(new ActionSetting("resume_pathing", "Resume pathing",
                "Resumes a process paused through Baritone.", () -> runAction("resume")));
        addSetting(new ActionSetting("stop_pathing", "Stop pathing",
                "Cancels every active Baritone process and releases movement input.", this::cancel));
        command = addSetting(new StringSetting("command", "Baritone command",
                "Any Baritone command without the # prefix, such as explore, farm, follow, waypoint, or build.",
                "help", 512, false));
        addSetting(new ActionSetting("run_command", "Run Baritone command",
                "Runs the command above through the embedded Baritone command manager.", this::runConfiguredCommand));
        synchronize();
    }

    public boolean execute(String command) {
        if (!isEnabled()) {
            throw new IllegalStateException("Baritone is disabled");
        }
        synchronize();
        return access.execute(command);
    }

    public void cancel() {
        access.cancel();
    }

    public String status() {
        String state = access.isPathing() ? "pathing" : access.hasPath() ? "paused" : "idle";
        return state + ", goal: " + access.goalDescription();
    }

    /**
     * Reasserts Helikon's saved controls after Baritone finishes loading its
     * own local settings file. This is intentionally idempotent.
     */
    public void tick() {
        synchronize();
    }

    public boolean rendersPath() {
        return isEnabled() && renderPath.value();
    }

    public boolean rendersGoal() {
        return isEnabled() && renderGoal.value();
    }

    public boolean rendersActions() {
        return isEnabled() && renderActions.value();
    }

    public boolean rendersThroughWalls() {
        return renderThroughWalls.value();
    }

    public int pathColor() {
        return pathColor.value();
    }

    public int goalColor() {
        return goalColor.value();
    }

    public int breakColor() {
        return breakColor.value();
    }

    public int placeColor() {
        return placeColor.value();
    }

    public int walkIntoColor() {
        return walkIntoColor.value();
    }

    public float lineWidth() {
        return lineWidth.value().floatValue();
    }

    public void shutdown() {
        access.cancel();
        access.apply(options(false));
    }

    @Override
    protected void onEnable() {
        synchronize();
    }

    @Override
    protected void onDisable() {
        access.cancel();
        access.apply(options(false));
    }

    private BooleanSetting booleanSetting(String id, String name, String description, boolean defaultValue) {
        BooleanSetting setting = addSetting(new BooleanSetting(id, name, description, defaultValue));
        setting.addChangeListener(ignored -> synchronize());
        return setting;
    }

    private void goToDestination() {
        runAction("goto " + destination.value().trim());
    }

    private void mineSelectedBlocks() {
        runAction("mine " + mineBlocks.value().trim());
    }

    private void runConfiguredCommand() {
        runAction(command.value().trim());
    }

    private void runAction(String command) {
        if (isEnabled()) {
            synchronize();
            access.execute(command);
        }
    }

    private void synchronize() {
        access.apply(options(isEnabled()));
    }

    private BaritoneAccess.Options options(boolean active) {
        return new BaritoneAccess.Options(active, allowBreak.value(), allowPlace.value(), allowSprint.value(),
                allowParkour.value(), allowInventory.value(), hashCommands.value(), renderPath.value(),
                renderGoal.value());
    }
}
