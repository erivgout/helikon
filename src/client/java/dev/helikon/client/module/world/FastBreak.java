package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.BlockIdList;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Objects;
import java.util.Set;

/** Lowers an existing ordinary local destroy cooldown for configured block IDs. */
public final class FastBreak extends Module {
    public record Action(boolean setDelay, int delay) {
        private static final Action NONE = new Action(false, -1);

        public static Action none() {
            return NONE;
        }
    }

    private final NumberSetting breakDelay;
    private final NumberSetting speedMultiplier;
    private final StringSetting blocks;
    private final CooldownAccess cooldown;
    private Set<String> targetBlocks;
    private int originalDelay = -1;
    private int lastAppliedDelay = -1;

    public FastBreak(CooldownAccess cooldown) {
        super("fast_break", "FastBreak", "Accelerates active local block damage and lowers the next-break delay.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
        breakDelay = addSetting(new NumberSetting("break_delay", "Client break delay",
                "Requested normal client destroy delay after an ordinary block break.", 0.0D, 0.0D, 5.0D));
        speedMultiplier = addSetting(new NumberSetting("speed_multiplier", "Speed multiplier",
                "Number of bounded ordinary destroy-progress steps requested per client tick.",
                2.0D, 1.0D, 5.0D));
        blocks = addSetting(new StringSetting("blocks", "Blocks",
                "Optional semicolon-separated block IDs; blank applies to every normal block target.", "", 1_024, true));
        targetBlocks = BlockIdList.parse(blocks.value());
        blocks.addChangeListener(ignored -> targetBlocks = BlockIdList.parse(blocks.value()));
    }

    /** Produces a reduction only for a held, visible ordinary target that passes the local block filter. */
    public Action update(boolean attackHeld, boolean hasBlockTarget, String blockId, int currentDelay) {
        if (currentDelay < 0) {
            throw new IllegalArgumentException("currentDelay must not be negative");
        }
        if (!isEnabled() || !attackHeld || !hasBlockTarget || !matches(blockId)) {
            return Action.none();
        }
        int requested = (int) Math.round(breakDelay.value());
        return currentDelay > requested ? new Action(true, requested) : Action.none();
    }

    /** Applies one reduction through the narrow platform port and remembers only its own value for restoration. */
    public Action tick(boolean attackHeld, boolean hasBlockTarget, String blockId) {
        int currentDelay = cooldown.delay();
        Action action = update(attackHeld, hasBlockTarget, blockId, currentDelay);
        if (!action.setDelay()) {
            return action;
        }
        if (originalDelay < 0) {
            originalDelay = currentDelay;
        }
        lastAppliedDelay = action.delay();
        cooldown.setDelay(action.delay());
        return action;
    }

    /** Additional ordinary progress calls after vanilla's first held-attack step. */
    public int extraDestroySteps(boolean attackHeld, boolean hasBlockTarget, String blockId) {
        if (!isEnabled() || !attackHeld || !hasBlockTarget || !matches(blockId)) {
            return 0;
        }
        return Math.max(0, (int) Math.round(speedMultiplier.value()) - 1);
    }

    public int breakDelayTicks() {
        return (int) Math.round(breakDelay.value());
    }

    public boolean appliesTo(String blockId) {
        return isEnabled() && matches(blockId);
    }

    private boolean matches(String blockId) {
        return blockId != null && !blockId.isBlank() && (targetBlocks.isEmpty() || targetBlocks.contains(blockId));
    }

    @Override
    protected void onDisable() {
        if (originalDelay >= 0 && cooldown.delay() == lastAppliedDelay) {
            cooldown.setDelay(originalDelay);
        }
        originalDelay = -1;
        lastAppliedDelay = -1;
    }

    /** Narrow Minecraft-free port for the transient ordinary destroy cooldown. */
    public interface CooldownAccess {
        int delay();

        void setDelay(int value);
    }
}
