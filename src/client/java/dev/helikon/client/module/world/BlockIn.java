package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Plans a bounded protective block wall around the local player and requests only normal
 * held-block placements. The plan is Minecraft-free; the server still validates every placement.
 */
public final class BlockIn extends Module {
    private static final List<BuildVector> CARDINALS = List.of(
            new BuildVector(1, 0, 0),
            new BuildVector(-1, 0, 0),
            new BuildVector(0, 0, 1),
            new BuildVector(0, 0, -1)
    );
    private static final List<BuildVector[]> CORNERS = List.of(
            new BuildVector[]{new BuildVector(1, 0, 0), new BuildVector(0, 0, 1)},
            new BuildVector[]{new BuildVector(1, 0, 0), new BuildVector(0, 0, -1)},
            new BuildVector[]{new BuildVector(-1, 0, 0), new BuildVector(0, 0, 1)},
            new BuildVector[]{new BuildVector(-1, 0, 0), new BuildVector(0, 0, -1)}
    );

    /** The local player's feet block plus the loaded, replaceable subset of the current plan. */
    public record Context(BuildPoint feet, Set<BuildPoint> replaceable) {
        public Context {
            feet = Objects.requireNonNull(feet, "feet");
            replaceable = Set.copyOf(Objects.requireNonNull(replaceable, "replaceable"));
        }
    }

    private final NumberSetting height;
    private final BooleanSetting includeCorners;
    private final BooleanSetting floor;
    private final BooleanSetting roof;
    private final NumberSetting blocksPerTick;
    private final NumberSetting placementDelayTicks;
    private long nextActionTick;

    public BlockIn() {
        super("block_in", "BlockIn",
                "Surrounds the local player with a protective wall of held blocks through normal use requests.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        height = addSetting(new NumberSetting("height", "Height",
                "Number of wall layers built upward from the player's feet.", 2.0D, 1.0D, 3.0D));
        includeCorners = addSetting(new BooleanSetting("include_corners", "Include corners",
                "Also fill the four diagonal gaps around the player.", false));
        floor = addSetting(new BooleanSetting("floor", "Floor",
                "Place a block directly beneath the player's feet.", false));
        roof = addSetting(new BooleanSetting("roof", "Roof",
                "Place a block directly above the wall to fully enclose the player.", false));
        blocksPerTick = addSetting(new NumberSetting("blocks_per_tick", "Blocks per tick",
                "Maximum ordinary placement requests made in a single tick.", 4.0D, 1.0D, 8.0D));
        placementDelayTicks = addSetting(new NumberSetting("placement_delay_ticks", "Placement delay",
                "Minimum ticks between placement cycles.", 1.0D, 0.0D, 40.0D));
    }

    /** Returns the full ordered protective plan for the given feet position, most protective first. */
    public List<BuildPoint> targets(BuildPoint feet) {
        Objects.requireNonNull(feet, "feet");
        int layers = size(height);
        List<BuildPoint> result = new ArrayList<>();
        for (int layer = 0; layer < layers; layer++) {
            BuildPoint base = feet.offset(BuildVector.UP, layer);
            for (BuildVector cardinal : CARDINALS) {
                result.add(base.offset(cardinal, 1));
            }
        }
        if (includeCorners.value()) {
            for (int layer = 0; layer < layers; layer++) {
                BuildPoint base = feet.offset(BuildVector.UP, layer);
                for (BuildVector[] corner : CORNERS) {
                    result.add(base.offset(corner[0], 1).offset(corner[1], 1));
                }
            }
        }
        if (floor.value()) {
            result.add(feet.offset(BuildVector.UP, -1));
        }
        if (roof.value()) {
            result.add(feet.offset(BuildVector.UP, layers));
        }
        return List.copyOf(result);
    }

    /** Returns a bounded set of replaceable plan positions to place this tick, honoring the placement delay. */
    public List<BuildPoint> nextPlacements(long tick, Context context) {
        Objects.requireNonNull(context, "context");
        if (!isEnabled() || tick < nextActionTick) {
            return List.of();
        }
        int cap = size(blocksPerTick);
        List<BuildPoint> chosen = new ArrayList<>();
        for (BuildPoint point : targets(context.feet())) {
            if (context.replaceable().contains(point)) {
                chosen.add(point);
                if (chosen.size() >= cap) {
                    break;
                }
            }
        }
        if (!chosen.isEmpty()) {
            nextActionTick = tick + Math.round(placementDelayTicks.value());
        }
        return List.copyOf(chosen);
    }

    @Override
    protected void onDisable() {
        nextActionTick = 0L;
    }

    private static int size(NumberSetting setting) {
        return (int) Math.round(setting.value());
    }
}
