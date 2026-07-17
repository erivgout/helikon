package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.world.BuildPoint;
import dev.helikon.client.module.world.BuildVector;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Produces bounded underfoot/ahead scaffold targets and safe local hotbar-selection decisions. */
public final class Scaffold extends Module {
    public enum Placement {
        BELOW,
        AHEAD
    }

    public record HotbarBlock(int slot, int count) {
        public HotbarBlock {
            if (slot < 0 || slot > 8 || count <= 0) {
                throw new IllegalArgumentException("hotbar block facts are invalid");
            }
        }
    }

    private final EnumSetting<Placement> placement;
    private final BooleanSetting selectHotbarBlock;
    private final BooleanSetting rotateToTarget;
    private final BooleanSetting tower;
    private final BooleanSetting edgeSafety;
    private final NumberSetting placementDelayTicks;
    private long nextPlacementTick;

    public Scaffold() {
        super("scaffold", "Scaffold", "Places player-provided hotbar blocks with normal local use interactions.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        placement = addSetting(new EnumSetting<>("placement", "Placement", "Choose below or ahead target selection.",
                Placement.class, Placement.BELOW));
        selectHotbarBlock = addSetting(new BooleanSetting("select_hotbar_block", "Select hotbar block",
                "Select the fullest available hotbar block only when no selected block is available.", true));
        rotateToTarget = addSetting(new BooleanSetting("rotate_to_target", "Rotate to target",
                "Locally face the target support before requesting a normal placement.", true));
        tower = addSetting(new BooleanSetting("tower", "Tower", "Request normal jump input while placing below.", false));
        edgeSafety = addSetting(new BooleanSetting("edge_safety", "Edge safety", "Request local sneak near an open target.", true));
        placementDelayTicks = addSetting(new NumberSetting("placement_delay_ticks", "Placement delay",
                "Minimum ticks between ordinary held-block placement requests.", 4.0D, 1.0D, 40.0D));
    }

    public Optional<BuildPoint> nextTarget(long tick, BuildPoint playerBlock, BuildVector forward,
                                            boolean moving, boolean targetReplaceable) {
        if (playerBlock == null || forward == null) {
            throw new IllegalArgumentException("scaffold position facts must not be null");
        }
        if (!isEnabled() || !targetReplaceable || tick < nextPlacementTick) {
            return Optional.empty();
        }
        BuildPoint target = candidateTarget(playerBlock, forward, moving);
        nextPlacementTick = tick + Math.round(placementDelayTicks.value());
        return Optional.of(target);
    }

    /** Calculates the bounded target without consuming the ordinary placement delay. */
    public BuildPoint candidateTarget(BuildPoint playerBlock, BuildVector forward, boolean moving) {
        if (playerBlock == null || forward == null) {
            throw new IllegalArgumentException("scaffold position facts must not be null");
        }
        BuildPoint target = playerBlock.offset(BuildVector.UP, -1);
        return placement.value() == Placement.AHEAD && moving ? target.offset(forward, 1) : target;
    }

    public Optional<Integer> selectBlockSlot(int selectedSlot, boolean selectedIsBlock, List<HotbarBlock> blocks) {
        if (selectedSlot < 0 || selectedSlot > 8 || blocks == null || !isEnabled() || selectedIsBlock
                || !selectHotbarBlock.value()) {
            return Optional.empty();
        }
        return blocks.stream().max(Comparator.comparingInt(HotbarBlock::count).thenComparingInt(HotbarBlock::slot))
                .map(HotbarBlock::slot);
    }

    public boolean shouldRequestTowerJump(boolean onGround, boolean replacingBelow) {
        return isEnabled() && tower.value() && onGround && replacingBelow;
    }

    public boolean shouldRequestEdgeSafety(boolean openTarget) {
        return isEnabled() && edgeSafety.value() && openTarget;
    }

    public boolean rotateToTarget() {
        return isEnabled() && rotateToTarget.value();
    }

    @Override
    protected void onDisable() {
        nextPlacementTick = 0L;
    }
}
