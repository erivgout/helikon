package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Optional;

/**
 * Minecraft-free defensive target selection for a bounded teleport-attack loop. The normal attack
 * can apply server-authorized knockback; it does not claim to make the player invulnerable.
 */
public final class GojosInfinity extends Module {
    public record Threat(String id, CombatEntityType type, boolean friend, boolean suspectedBot,
                         boolean alive, boolean lineOfSight, double distance, double closingSpeed) {
        public Threat {
            if (id == null || id.isBlank() || type == null || !Double.isFinite(distance) || distance < 0.0D
                    || !Double.isFinite(closingSpeed)) {
                throw new IllegalArgumentException("Infinity threat facts are invalid");
            }
        }
    }

    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final BooleanSetting requireApproaching;
    private final BooleanSetting requireAttackReady;
    private final NumberSetting barrierRadius;
    private final NumberSetting minimumClosingSpeed;
    private final NumberSetting attackDistance;
    private final IntegerSetting delayTicks;
    private long lastAttackTick = -1L;

    public GojosInfinity() {
        super("gojo_infinity", "Gojo's Infinity",
                "Defensively teleport-attacks closing threats so accepted hits can knock them away.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Defend against non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Defend against hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Defend against passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never select a locally listed friend.", true));
        requireApproaching = addSetting(new BooleanSetting("require_approaching", "Approaching only",
                "Only react when relative motion is closing the distance.", true));
        requireAttackReady = addSetting(new BooleanSetting("require_attack_ready", "Require attack ready",
                "Wait for ordinary attack strength before reacting.", true));
        barrierRadius = addSetting(new NumberSetting("barrier_radius", "Barrier radius",
                "Maximum local distance at which a closing threat can trigger the loop.",
                4.5D, 2.0D, 8.0D));
        minimumClosingSpeed = addSetting(new NumberSetting("minimum_closing_speed", "Minimum closing speed",
                "Minimum relative horizontal approach speed in blocks per tick.",
                0.03D, 0.0D, 1.0D));
        attackDistance = addSetting(new NumberSetting("attack_distance", "Attack distance",
                "Desired distance from the target for the temporary attack position.",
                2.5D, 1.0D, 3.0D));
        delayTicks = addSetting(new IntegerSetting("delay_ticks", "Attack delay",
                "Minimum client ticks between defensive attack loops.", 10, 1, 40));
    }

    /** Selects at most the nearest eligible threat and consumes cadence only on selection. */
    public Optional<String> selectThreat(long tick, boolean attackReady, List<Threat> threats) {
        if (tick < 0L || threats == null) {
            throw new IllegalArgumentException("Infinity inputs are invalid");
        }
        if (!isEnabled() || (requireAttackReady.value() && !attackReady)
                || (lastAttackTick >= 0L && tick - lastAttackTick < delayTicks.value())) {
            return Optional.empty();
        }

        Threat best = null;
        for (Threat threat : threats) {
            if (!threat.alive() || !threat.lineOfSight() || threat.suspectedBot()
                    || threat.distance() > barrierRadius.value()
                    || (threat.friend() && excludeFriends.value())
                    || !typeEnabled(threat.type())
                    || (requireApproaching.value() && threat.closingSpeed() < minimumClosingSpeed.value())) {
                continue;
            }
            if (best == null || threat.distance() < best.distance()) {
                best = threat;
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        lastAttackTick = tick;
        return Optional.of(best.id());
    }

    private boolean typeEnabled(CombatEntityType type) {
        return switch (type) {
            case PLAYER -> players.value();
            case HOSTILE -> hostiles.value();
            case PASSIVE -> passive.value();
        };
    }

    public double barrierRadius() {
        return barrierRadius.value();
    }

    public double attackDistance() {
        return attackDistance.value();
    }

    public boolean excludeFriends() {
        return excludeFriends.value();
    }

    public void resetTransientState() {
        lastAttackTick = -1L;
    }

    @Override
    protected void onDisable() {
        resetTransientState();
    }
}
