package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

/**
 * Small, independently-toggleable legacy visual/pose effects. Minecraft access
 * is deliberately kept in {@link MinecraftLegacyFunAccess}.
 */
public final class LegacyFunModules {
    private LegacyFunModules() {
    }

    public static final class Derp extends Module {
        private final NumberSetting speed;

        public Derp() {
            super("derp", "Derp", "Continuously rotates the local player's view using ordinary rotations.",
                    ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
            speed = addSetting(new NumberSetting("speed", "Speed", "Degrees added per client tick.",
                    35.0, 1.0, 180.0));
        }

        public Rotation rotation(long tick) {
            double yaw = Math.floorMod(Math.round(tick * speed.value()), 360);
            double pitch = Math.sin(tick * 0.35) * 89.0;
            return new Rotation((float) yaw, (float) pitch);
        }
    }

    public static final class HeadRoll extends Module {
        private final NumberSetting speed;
        private final NumberSetting amount;

        public HeadRoll() {
            super("head_roll", "HeadRoll", "Rolls the local view in a circular pitch/yaw approximation.",
                    ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
            speed = addSetting(new NumberSetting("speed", "Speed", "Animation speed.", 0.18, 0.02, 1.0));
            amount = addSetting(new NumberSetting("amount", "Amount", "Maximum pitch/yaw displacement.",
                    35.0, 5.0, 89.0));
        }

        public Rotation rotation(long tick, float baseYaw) {
            double angle = tick * speed.value();
            return new Rotation((float) (baseYaw + Math.cos(angle) * amount.value()),
                    (float) (Math.sin(angle) * amount.value()));
        }
    }

    public static final class Lsd extends Module {
        private final IntegerSetting refreshTicks;

        public Lsd() {
            super("lsd", "LSD", "Applies a reversible client-local nausea visual effect.",
                    ModuleCategory.RENDER, false, Keybind.unbound());
            refreshTicks = addSetting(new IntegerSetting("refresh_ticks", "Refresh ticks",
                    "How often the local visual effect is refreshed.", 160, 40, 600));
        }

        public int refreshTicks() {
            return refreshTicks.value();
        }
    }

    public static final class MileyCyrus extends Module {
        private final IntegerSetting interval;

        public MileyCyrus() {
            super("miley_cyrus", "MileyCyrus", "Repeatedly swings both hands for a local wrecking-ball-like pose.",
                    ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
            interval = addSetting(new IntegerSetting("interval", "Swing interval",
                    "Ticks between hand swing animations.", 8, 2, 40));
        }

        public boolean shouldSwing(long tick) {
            return isEnabled() && tick % interval.value() == 0;
        }
    }

    public static final class Tired extends Module {
        public Tired() {
            super("tired", "Tired", "Keeps the local player in a prone swimming pose while enabled.",
                    ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        }
    }

    public static final class Headless extends Module {
        public Headless() {
            super("headless", "Headless", "Displaces the local head by forcing an extreme downward pose.",
                    ModuleCategory.RENDER, false, Keybind.unbound());
        }

        public float displacedPitch() {
            return isEnabled() ? 89.0F : 0.0F;
        }
    }

    public record Rotation(float yaw, float pitch) {
    }
}
