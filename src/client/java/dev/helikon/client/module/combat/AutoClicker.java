package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Repeats ordinary local left-click attacks at a configurable rate while its
 * conditions pass. The decision logic here is Minecraft-free; a thin combat
 * adapter turns each due click into Minecraft's normal entity attack or arm
 * swing. The server remains authoritative over attack cooldown and reach.
 */
public final class AutoClicker extends Module {
    /** Local per-tick facts the click policy needs; all primitives, no Minecraft types. */
    public record Context(boolean attackHeld, boolean screenOpen, boolean crosshairEntity, boolean crosshairFriend) {
    }

    private final NumberSetting minCps;
    private final NumberSetting maxCps;
    private final BooleanSetting requireAttackHeld;
    private final BooleanSetting requireEntityTarget;
    private final BooleanSetting excludeFriends;
    private final RandomGenerator random;

    private long nextClickTime = -1L;

    public AutoClicker() {
        this(new java.util.Random());
    }

    /** Test seam: an injected generator makes the click jitter deterministic. */
    public AutoClicker(RandomGenerator random) {
        super("auto_clicker", "AutoClicker",
                "Repeats ordinary left-click attacks at a configurable rate while attack conditions pass.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        this.random = Objects.requireNonNull(random, "random");
        minCps = addSetting(new NumberSetting("min_cps", "Minimum CPS",
                "Lowest clicks per second for the randomized interval.", 8.0D, 1.0D, 20.0D));
        maxCps = addSetting(new NumberSetting("max_cps", "Maximum CPS",
                "Highest clicks per second for the randomized interval.", 12.0D, 1.0D, 20.0D));
        requireAttackHeld = addSetting(new BooleanSetting("require_attack_held", "Require attack held",
                "Only click while the attack button is physically held.", true));
        requireEntityTarget = addSetting(new BooleanSetting("require_entity_target", "Require entity target",
                "Only click when an attackable entity is under the crosshair.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never attack a locally listed friend under the crosshair.", true));
    }

    /**
     * Decides whether a click fires at {@code timeMillis}. Schedules the next
     * randomized interval on the first eligible moment and after each click.
     */
    public boolean shouldClick(long timeMillis, Context context) {
        Objects.requireNonNull(context, "context");
        if (!isEnabled() || context.screenOpen()
                || (requireAttackHeld.value() && !context.attackHeld())
                || (requireEntityTarget.value() && !shouldAttackEntity(context))) {
            nextClickTime = -1L;
            return false;
        }
        if (nextClickTime < 0L || timeMillis >= nextClickTime) {
            nextClickTime = timeMillis + intervalMillis(minCps.value(), maxCps.value(), random.nextDouble());
            return true;
        }
        return false;
    }

    /** Whether a fired click should attack the crosshair entity rather than only swing. */
    public boolean shouldAttackEntity(Context context) {
        Objects.requireNonNull(context, "context");
        return context.crosshairEntity() && !(excludeFriends.value() && context.crosshairFriend());
    }

    /** Resets the click schedule when the local player, level, or game mode is unavailable. */
    public void onContextLost() {
        nextClickTime = -1L;
    }

    /** Pure interval math: maps a [0,1] fraction between the two rates to a bounded millisecond gap. */
    static long intervalMillis(double minCps, double maxCps, double fraction) {
        double low = Math.min(minCps, maxCps);
        double high = Math.max(minCps, maxCps);
        double clamped = Double.isFinite(fraction) ? Math.max(0.0D, Math.min(1.0D, fraction)) : 0.0D;
        double cps = low + (high - low) * clamped;
        if (cps <= 0.0D) {
            cps = 1.0D;
        }
        return Math.max(1L, Math.round(1000.0D / cps));
    }

    @Override
    protected void onDisable() {
        nextClickTime = -1L;
    }
}
