package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Optional;

/**
 * Decides whether a bow release should be preceded by a bounded movement sequence.
 * Minecraft-facing packet construction is isolated in {@link MinecraftArrowDmgAccess}.
 */
public final class ArrowDmg extends Module {
    private static final double MAXIMUM_SERVER_MOVEMENT_DISTANCE = Math.sqrt(500.0D);

    private final NumberSetting strength;
    private final IntegerSetting minimumDrawTicks;
    private final IntegerSetting stationaryPackets;
    private final BooleanSetting sprintSignal;

    public ArrowDmg() {
        super("arrow_dmg", "ArrowDMG",
                "Attempts to increase bow launch velocity with a bounded movement sequence immediately before release.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        strength = addSetting(new NumberSetting("strength", "Strength",
                "Movement-sequence strength; higher values are more likely to be corrected or rejected.",
                3.0D, 0.1D, 10.0D));
        minimumDrawTicks = addSetting(new IntegerSetting("minimum_draw_ticks", "Minimum draw ticks",
                "Only attempt the sequence after the bow has been drawn for at least this many ticks.",
                20, 1, 20));
        stationaryPackets = addSetting(new IntegerSetting("stationary_packets", "Stationary packets",
                "Number of ordinary same-position updates sent before the bounded movement pair.",
                4, 1, 4));
        sprintSignal = addSetting(new BooleanSetting("sprint_signal", "Sprint signal",
                "Send vanilla's start-sprinting command before the movement sequence.", true));
    }

    /**
     * Produces the bounded, Minecraft-free release plan for the current bow facts.
     */
    public Optional<MovementPlan> planRelease(ReleaseContext context) {
        if (!isEnabled() || !context.bow() || !context.connected() || context.screenOpen()
                || context.drawTicks() < minimumDrawTicks.value()) {
            return Optional.empty();
        }
        double scale = strength.value() / 10.0D * MAXIMUM_SERVER_MOVEMENT_DISTANCE;
        double offsetX = -context.lookX() * scale;
        double offsetZ = -context.lookZ() * scale;
        if (Math.abs(offsetX) + Math.abs(offsetZ) < 1.0E-6D) {
            return Optional.empty();
        }
        return Optional.of(new MovementPlan(stationaryPackets.value(), offsetX, offsetZ, sprintSignal.value()));
    }

    public record ReleaseContext(boolean bow, int drawTicks, boolean screenOpen, boolean connected,
                                 double lookX, double lookZ) {
        public ReleaseContext {
            if (drawTicks < 0 || !Double.isFinite(lookX) || !Double.isFinite(lookZ)) {
                throw new IllegalArgumentException("ArrowDMG release facts must be finite and non-negative");
            }
        }
    }

    public record MovementPlan(int stationaryPackets, double offsetX, double offsetZ, boolean sprintSignal) {
        public MovementPlan {
            if (stationaryPackets < 1 || stationaryPackets > 4
                    || !Double.isFinite(offsetX) || !Double.isFinite(offsetZ)) {
                throw new IllegalArgumentException("ArrowDMG movement plan is invalid");
            }
        }
    }
}
