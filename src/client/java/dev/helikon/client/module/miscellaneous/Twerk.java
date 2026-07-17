package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.movement.MovementInput;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Alternates a local sneak input while enabled, without retaining any key state. */
public final class Twerk extends Module {
    private final NumberSetting halfCycleTicks;
    private int phaseTick;

    public Twerk() {
        super("twerk", "Twerk", "Alternates a local sneak input at a bounded interval.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        halfCycleTicks = addSetting(new NumberSetting("half_cycle_ticks", "Half-cycle ticks",
                "Ticks to hold and then release the local sneak input.", 4.0D, 1.0D, 20.0D));
    }

    /** Applies one local crouch-pulse decision to freshly polled movement input. */
    public MovementInput apply(MovementInput input, boolean screenOpen) {
        MovementInput current = Objects.requireNonNull(input, "input");
        if (!isEnabled() || screenOpen) {
            resetPhase();
            return current;
        }

        int halfCycle = halfCycleTicks();
        boolean addSneak = phaseTick < halfCycle;
        phaseTick = (phaseTick + 1) % (halfCycle * 2);
        if (!addSneak) {
            return current;
        }
        return new MovementInput(current.forward(), current.backward(), current.left(), current.right(),
                current.jump(), true, current.sprint());
    }

    @Override
    protected void onEnable() {
        resetPhase();
    }

    @Override
    protected void onDisable() {
        resetPhase();
    }

    private int halfCycleTicks() {
        return (int) Math.round(halfCycleTicks.value());
    }

    private void resetPhase() {
        phaseTick = 0;
    }
}
