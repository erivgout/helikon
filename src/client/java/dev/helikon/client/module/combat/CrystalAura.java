package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Objects;

/**
 * Decides, from Minecraft-free local observations, whether to request one ordinary end-crystal
 * placement or one normal attack against an already-spawned crystal at a bounded cadence.
 *
 * <p>The logic never fabricates packets. A thin adapter turns a {@link Action} into Minecraft's
 * normal held-item-use or attack interaction, and the server remains authoritative: it may reject,
 * correct, rubber-band, or kick for reach, cooldown, obstruction, or gamemode reasons.
 */
public final class CrystalAura extends Module {
    /** A candidate obsidian/bedrock support block whose space above can host a crystal. */
    public record Placement(int x, int y, int z, double playerDistance, double targetDistance) {
        public Placement {
            if (!Double.isFinite(playerDistance) || playerDistance < 0.0D
                    || !Double.isFinite(targetDistance) || targetDistance < 0.0D) {
                throw new IllegalArgumentException("placement distances must be finite and non-negative");
            }
        }
    }

    /** An already-spawned local crystal entity that could be detonated. */
    public record Crystal(String id, double playerDistance, double targetDistance) {
        public Crystal {
            if (id == null || id.isBlank() || !Double.isFinite(playerDistance) || playerDistance < 0.0D
                    || !Double.isFinite(targetDistance) || targetDistance < 0.0D) {
                throw new IllegalArgumentException("crystal facts must be valid");
            }
        }
    }

    /** Immutable local snapshot the decision reads for one client tick. */
    public record State(boolean holdingCrystal, boolean placeReady, boolean attackReady, boolean hasTarget,
                        List<Placement> placements, List<Crystal> crystals) {
        public State {
            placements = List.copyOf(Objects.requireNonNull(placements, "placements"));
            crystals = List.copyOf(Objects.requireNonNull(crystals, "crystals"));
        }
    }

    public enum ActionType {
        NONE,
        PLACE,
        ATTACK
    }

    /** One bounded local request; PLACE carries a support block, ATTACK carries a crystal id. */
    public record Action(ActionType type, int x, int y, int z, String crystalId) {
        private static final Action NONE = new Action(ActionType.NONE, 0, 0, 0, "");

        public static Action none() {
            return NONE;
        }

        public static Action place(Placement placement) {
            return new Action(ActionType.PLACE, placement.x(), placement.y(), placement.z(), "");
        }

        public static Action attack(String crystalId) {
            return new Action(ActionType.ATTACK, 0, 0, 0, crystalId);
        }
    }

    private final BooleanSetting place;
    private final BooleanSetting detonate;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting excludeFriends;
    private final NumberSetting placeRange;
    private final NumberSetting attackRange;
    private final NumberSetting targetRange;
    private final NumberSetting damageRadius;
    private final NumberSetting delayTicks;

    private long lastActionTick = -1L;

    public CrystalAura() {
        super("crystal_aura", "CrystalAura",
                "Requests ordinary end-crystal placements and normal crystal attacks near an eligible target.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        place = addSetting(new BooleanSetting("place", "Place",
                "Request a normal end-crystal placement on nearby obsidian or bedrock.", true));
        detonate = addSetting(new BooleanSetting("detonate", "Detonate",
                "Request a normal attack on an already-spawned crystal near a target.", true));
        players = addSetting(new BooleanSetting("players", "Players", "Engage non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Engage hostile mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never engage locally listed friends.", true));
        placeRange = addSetting(new NumberSetting("place_range", "Place range",
                "Maximum local distance from the player to a placed crystal.", 4.5D, 1.0D, 6.0D));
        attackRange = addSetting(new NumberSetting("attack_range", "Attack range",
                "Maximum local distance from the player to a detonated crystal.", 4.5D, 1.0D, 6.0D));
        targetRange = addSetting(new NumberSetting("target_range", "Target range",
                "Maximum local distance from the player to an eligible target before engaging.", 8.0D, 1.0D, 16.0D));
        damageRadius = addSetting(new NumberSetting("damage_radius", "Damage radius",
                "Maximum distance from a crystal to the nearest target so it can plausibly hurt them.",
                4.0D, 1.0D, 6.0D));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Action delay",
                "Minimum client ticks between Helikon crystal placements or attacks.", 2.0D, 1.0D, 40.0D));
    }

    public boolean placeEnabled() {
        return place.value();
    }

    public boolean detonateEnabled() {
        return detonate.value();
    }

    public boolean allowPlayers() {
        return players.value();
    }

    public boolean allowHostiles() {
        return hostiles.value();
    }

    public boolean excludeFriends() {
        return excludeFriends.value();
    }

    public double placeRange() {
        return placeRange.value();
    }

    public double attackRange() {
        return attackRange.value();
    }

    public double targetRange() {
        return targetRange.value();
    }

    public double damageRadius() {
        return damageRadius.value();
    }

    /**
     * Chooses at most one bounded action for this tick. Detonating an existing crystal is preferred
     * over placing a new one; both honor the configured ranges, damage radius, and action delay.
     */
    public Action decide(long tick, State state) {
        if (tick < 0L || state == null) {
            throw new IllegalArgumentException("CrystalAura inputs are invalid");
        }
        if (!isEnabled() || !state.hasTarget()
                || (lastActionTick >= 0L && tick - lastActionTick < Math.round(delayTicks.value()))) {
            return Action.none();
        }
        if (detonate.value() && state.attackReady()) {
            Crystal crystal = bestCrystal(state.crystals());
            if (crystal != null) {
                lastActionTick = tick;
                return Action.attack(crystal.id());
            }
        }
        if (place.value() && state.placeReady() && state.holdingCrystal()) {
            Placement placement = bestPlacement(state.placements());
            if (placement != null) {
                lastActionTick = tick;
                return Action.place(placement);
            }
        }
        return Action.none();
    }

    private Crystal bestCrystal(List<Crystal> crystals) {
        Crystal best = null;
        for (Crystal crystal : crystals) {
            if (crystal.playerDistance() > attackRange.value() || crystal.targetDistance() > damageRadius.value()) {
                continue;
            }
            if (best == null || crystal.targetDistance() < best.targetDistance()
                    || (crystal.targetDistance() == best.targetDistance()
                            && crystal.playerDistance() < best.playerDistance())) {
                best = crystal;
            }
        }
        return best;
    }

    private Placement bestPlacement(List<Placement> placements) {
        Placement best = null;
        for (Placement placement : placements) {
            if (placement.playerDistance() > placeRange.value() || placement.targetDistance() > damageRadius.value()) {
                continue;
            }
            if (best == null || placement.targetDistance() < best.targetDistance()
                    || (placement.targetDistance() == best.targetDistance()
                            && placement.playerDistance() < best.playerDistance())) {
                best = placement;
            }
        }
        return best;
    }

    @Override
    protected void onDisable() {
        lastActionTick = -1L;
    }
}
