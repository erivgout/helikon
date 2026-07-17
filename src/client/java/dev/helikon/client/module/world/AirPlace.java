package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;
import java.util.Optional;

/**
 * Fills the replaceable block the crosshair points at in empty space, so a held block can be placed
 * without a clicked supporting neighbor. The decision logic here is Minecraft-free: it computes the
 * target block, a placement face, and a surface hit point from the player's eye ray. A thin adapter
 * turns that into one ordinary held-block use interaction. The interaction is vanilla-shaped and the
 * server stays authoritative: an unsupported placement may be rejected, corrected, or rubber-banded.
 */
public final class AirPlace extends Module {
    /** Immutable eye ray with a normalized (or near-normalized) look direction. */
    public record Ray(double originX, double originY, double originZ, double dirX, double dirY, double dirZ) {
    }

    /** One planned local placement: the block to fill, the clicked face, and a surface hit point. */
    public record Placement(BuildPoint block, BuildVector face, double hitX, double hitY, double hitZ) {
        public Placement {
            block = Objects.requireNonNull(block, "block");
            face = Objects.requireNonNull(face, "face");
        }
    }

    /** Minecraft-free snapshot the adapter supplies each tick. */
    public record Input(boolean useHeld, boolean heldBlock, boolean hasBlockTarget, Ray ray) {
        public Input {
            ray = Objects.requireNonNull(ray, "ray");
        }
    }

    private final NumberSetting range;
    private final NumberSetting placementDelayTicks;
    private final BooleanSetting onlyWithoutTarget;
    private long nextActionTick;

    public AirPlace() {
        super("air_place", "AirPlace",
                "Places held blocks into the empty space the crosshair points at, with no clicked supporting block.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        range = addSetting(new NumberSetting("range", "Range",
                "How far along the crosshair to pick the air block to fill, in blocks.", 4.5D, 1.0D, 6.0D));
        placementDelayTicks = addSetting(new NumberSetting("placement_delay_ticks", "Placement delay",
                "Minimum ticks between air-placement use actions.", 4.0D, 1.0D, 20.0D));
        onlyWithoutTarget = addSetting(new BooleanSetting("only_without_target", "Only without target",
                "Only air-place when the crosshair is not already on a block, leaving normal placement untouched.", true));
    }

    /**
     * Plans at most one air placement. Returns empty when the module is idle, gated by its delay, or
     * deferring to ordinary placement because the crosshair is on a real block.
     */
    public Optional<Placement> plan(long tick, Input input) {
        Objects.requireNonNull(input, "input");
        if (!isEnabled() || !input.useHeld() || !input.heldBlock() || tick < nextActionTick) {
            return Optional.empty();
        }
        if (onlyWithoutTarget.value() && input.hasBlockTarget()) {
            return Optional.empty();
        }

        Ray ray = input.ray();
        double reach = range.value();
        double pointX = ray.originX() + ray.dirX() * reach;
        double pointY = ray.originY() + ray.dirY() * reach;
        double pointZ = ray.originZ() + ray.dirZ() * reach;
        BuildPoint block = new BuildPoint(floor(pointX), floor(pointY), floor(pointZ));
        BuildVector face = dominantFaceToward(ray.dirX(), ray.dirY(), ray.dirZ());

        double hitX = block.x() + 0.5D + face.x() * 0.5D;
        double hitY = block.y() + 0.5D + face.y() * 0.5D;
        double hitZ = block.z() + 0.5D + face.z() * 0.5D;

        nextActionTick = tick + Math.round(placementDelayTicks.value());
        return Optional.of(new Placement(block, face, hitX, hitY, hitZ));
    }

    @Override
    protected void onDisable() {
        nextActionTick = 0L;
    }

    /** Face pointing back toward the viewer: the dominant axis of the negated look direction. */
    static BuildVector dominantFaceToward(double dirX, double dirY, double dirZ) {
        double absX = Math.abs(dirX);
        double absY = Math.abs(dirY);
        double absZ = Math.abs(dirZ);
        if (absY >= absX && absY >= absZ) {
            return new BuildVector(0, dirY <= 0.0D ? 1 : -1, 0);
        }
        if (absX >= absZ) {
            return new BuildVector(dirX <= 0.0D ? 1 : -1, 0, 0);
        }
        return new BuildVector(0, 0, dirZ <= 0.0D ? 1 : -1);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
