package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Optional;

/**
 * Requests one ordinary local attack against the nearest eligible in-range fireball so Minecraft's
 * normal interaction deflects it. The decision logic here is Minecraft-free and unit-tested; the
 * {@link MinecraftAntiFireballAccess} adapter owns every version-sensitive observation and the
 * single normal attack request.
 */
public final class AntiFireball extends Module {
    /** The two vanilla fireball families this module can deflect by attacking them. */
    public enum FireballKind {
        /** Ghast {@code LargeFireball}. */
        GHAST,
        /** Blaze {@code SmallFireball}. */
        BLAZE
    }

    /**
     * Minecraft-free snapshot of a single observed fireball. {@code approaching} is true when the
     * fireball's velocity points toward the local player; {@code lineOfSight} is true when the local
     * player has an unobstructed view of it.
     */
    public record FireballObservation(String id, FireballKind kind, double distance, boolean approaching,
                                      boolean lineOfSight) {
        public FireballObservation {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("fireball id must not be blank");
            }
            if (kind == null) {
                throw new IllegalArgumentException("fireball kind must not be null");
            }
            if (!Double.isFinite(distance) || distance < 0.0D) {
                throw new IllegalArgumentException("fireball distance must be finite and non-negative");
            }
        }
    }

    private final NumberSetting range;
    private final BooleanSetting ghastFireballs;
    private final BooleanSetting blazeFireballs;
    private final BooleanSetting requireApproaching;
    private final NumberSetting delayTicks;
    private long lastAttackTick = -1L;

    public AntiFireball() {
        super("anti_fireball", "AntiFireball",
                "Requests a normal attack against the nearest incoming fireball so Minecraft deflects it.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        range = addSetting(new NumberSetting("range", "Range",
                "Maximum local distance to a fireball before a normal attack is requested.",
                4.0D, 3.0D, 6.0D));
        ghastFireballs = addSetting(new BooleanSetting("ghast_fireballs", "Ghast fireballs",
                "Deflect large ghast fireballs.", true));
        blazeFireballs = addSetting(new BooleanSetting("blaze_fireballs", "Blaze fireballs",
                "Deflect small blaze fireballs.", true));
        requireApproaching = addSetting(new BooleanSetting("require_approaching", "Incoming only",
                "Only attack fireballs whose velocity points toward you.", true));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay",
                "Minimum ticks between normal attacks.", 1.0D, 0.0D, 20.0D));
    }

    /**
     * Chooses at most one fireball to attack this tick. Returns the id of the nearest eligible
     * fireball with local line of sight, or empty when nothing qualifies or the delay has not
     * elapsed. Records the cooldown only when it selects a target.
     */
    public Optional<String> selectTarget(long tick, List<FireballObservation> observations) {
        if (tick < 0L) {
            throw new IllegalArgumentException("tick must not be negative");
        }
        if (observations == null) {
            throw new IllegalArgumentException("observations must not be null");
        }
        if (!isEnabled()) {
            return Optional.empty();
        }
        if (lastAttackTick >= 0L && tick - lastAttackTick < Math.round(delayTicks.value())) {
            return Optional.empty();
        }
        FireballObservation best = null;
        for (FireballObservation observation : observations) {
            if (!kindEnabled(observation.kind()) || !observation.lineOfSight()
                    || observation.distance() > range.value()
                    || (requireApproaching.value() && !observation.approaching())) {
                continue;
            }
            if (best == null || observation.distance() < best.distance()) {
                best = observation;
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        lastAttackTick = tick;
        return Optional.of(best.id());
    }

    private boolean kindEnabled(FireballKind kind) {
        return switch (kind) {
            case GHAST -> ghastFireballs.value();
            case BLAZE -> blazeFireballs.value();
        };
    }

    @Override
    protected void onDisable() {
        lastAttackTick = -1L;
    }
}
