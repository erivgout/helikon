package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Objects;

/** Minecraft-free place, charge, and detonate decisions for bounded respawn-anchor combat. */
public final class AutoAnchor extends Module {
    public record Anchor(int x, int y, int z, int charges, double playerDistance,
                         double targetDistance, double friendDistance, boolean explosive) {
        public Anchor {
            validateDistances(playerDistance, targetDistance, friendDistance);
            if (charges < 0 || charges > 4) {
                throw new IllegalArgumentException("anchor charges must be between zero and four");
            }
        }
    }

    public record Placement(int x, int y, int z, double playerDistance,
                            double targetDistance, double friendDistance, boolean explosive) {
        public Placement {
            validateDistances(playerDistance, targetDistance, friendDistance);
        }
    }

    public record State(boolean interactionReady, boolean hasTarget, int anchorSlot,
                        int glowstoneSlot, int emptySlot, List<Anchor> anchors, List<Placement> placements) {
        public State {
            requireOptionalSlot(anchorSlot);
            requireOptionalSlot(glowstoneSlot);
            requireOptionalSlot(emptySlot);
            anchors = List.copyOf(Objects.requireNonNull(anchors, "anchors"));
            placements = List.copyOf(Objects.requireNonNull(placements, "placements"));
        }
    }

    public enum ActionType {
        NONE,
        PLACE,
        CHARGE,
        DETONATE
    }

    public record Action(ActionType type, int x, int y, int z, int hotbarSlot) {
        private static final Action NONE = new Action(ActionType.NONE, 0, 0, 0, -1);

        public Action {
            if (type == null || (type == ActionType.NONE && hotbarSlot != -1)
                    || (type != ActionType.NONE && (hotbarSlot < 0 || hotbarSlot > 8))) {
                throw new IllegalArgumentException("AutoAnchor action is invalid");
            }
        }

        public static Action none() {
            return NONE;
        }

        private static Action place(Placement placement, int slot) {
            return new Action(ActionType.PLACE, placement.x(), placement.y(), placement.z(), slot);
        }

        private static Action anchor(ActionType type, Anchor anchor, int slot) {
            return new Action(type, anchor.x(), anchor.y(), anchor.z(), slot);
        }
    }

    private final BooleanSetting place;
    private final BooleanSetting charge;
    private final BooleanSetting detonate;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting excludeFriends;
    private final IntegerSetting detonateAtCharges;
    private final NumberSetting interactRange;
    private final NumberSetting targetRange;
    private final NumberSetting blastRadius;
    private final NumberSetting minimumSelfDistance;
    private final IntegerSetting delayTicks;
    private long lastActionTick = -1L;

    public AutoAnchor() {
        super("auto_anchor", "AutoAnchor",
                "Places, charges, and detonates player-owned respawn anchors near eligible targets.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        place = addSetting(new BooleanSetting("place", "Place", "Place an anchor from the hotbar.", true));
        charge = addSetting(new BooleanSetting("charge", "Charge", "Charge an anchor with hotbar Glowstone.", true));
        detonate = addSetting(new BooleanSetting("detonate", "Detonate",
                "Use an empty hotbar slot to detonate a charged anchor.", true));
        players = addSetting(new BooleanSetting("players", "Players", "Engage non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Engage hostile mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never target friends or act when a friend is inside the configured blast radius.", true));
        detonateAtCharges = addSetting(new IntegerSetting("detonate_at_charges", "Detonate at charges",
                "Charge an anchor to this value before detonating it.", 1, 1, 4));
        interactRange = addSetting(new NumberSetting("interact_range", "Interact range",
                "Maximum eye-to-anchor distance for each normal interaction.", 4.5D, 1.0D, 6.0D));
        targetRange = addSetting(new NumberSetting("target_range", "Target range",
                "Maximum local distance to an eligible target.", 8.0D, 1.0D, 16.0D));
        blastRadius = addSetting(new NumberSetting("blast_radius", "Blast radius",
                "Maximum anchor-to-target distance and friend safety radius.", 4.0D, 1.0D, 6.0D));
        minimumSelfDistance = addSetting(new NumberSetting("minimum_self_distance", "Minimum self distance",
                "Require this much distance from the player's eyes to the anchor.", 3.0D, 0.0D, 6.0D));
        delayTicks = addSetting(new IntegerSetting("delay_ticks", "Action delay",
                "Minimum ticks between place, charge, and detonate requests.", 4, 1, 40));
    }

    /** Chooses at most one action, preferring detonation, then charging, then placement. */
    public Action decide(long tick, State state) {
        if (tick < 0L || state == null) {
            throw new IllegalArgumentException("AutoAnchor inputs are invalid");
        }
        if (!isEnabled() || !state.interactionReady() || !state.hasTarget()
                || (lastActionTick >= 0L && tick - lastActionTick < delayTicks.value())) {
            return Action.none();
        }

        if (detonate.value() && state.emptySlot() >= 0) {
            Anchor anchor = bestAnchor(state.anchors(), true);
            if (anchor != null) {
                lastActionTick = tick;
                return Action.anchor(ActionType.DETONATE, anchor, state.emptySlot());
            }
        }
        if (charge.value() && state.glowstoneSlot() >= 0) {
            Anchor anchor = bestAnchor(state.anchors(), false);
            if (anchor != null) {
                lastActionTick = tick;
                return Action.anchor(ActionType.CHARGE, anchor, state.glowstoneSlot());
            }
        }
        if (place.value() && state.anchorSlot() >= 0) {
            Placement placement = bestPlacement(state.placements());
            if (placement != null) {
                lastActionTick = tick;
                return Action.place(placement, state.anchorSlot());
            }
        }
        return Action.none();
    }

    private Anchor bestAnchor(List<Anchor> anchors, boolean readyToDetonate) {
        Anchor best = null;
        for (Anchor anchor : anchors) {
            boolean chargeMatches = readyToDetonate
                    ? anchor.charges() >= detonateAtCharges.value()
                    : anchor.charges() < detonateAtCharges.value();
            if (!chargeMatches || !eligible(anchor.playerDistance(), anchor.targetDistance(),
                    anchor.friendDistance(), anchor.explosive())) {
                continue;
            }
            if (best == null || closerToTarget(anchor.targetDistance(), anchor.playerDistance(),
                    best.targetDistance(), best.playerDistance())) {
                best = anchor;
            }
        }
        return best;
    }

    private Placement bestPlacement(List<Placement> placements) {
        Placement best = null;
        for (Placement placement : placements) {
            if (!eligible(placement.playerDistance(), placement.targetDistance(),
                    placement.friendDistance(), placement.explosive())) {
                continue;
            }
            if (best == null || closerToTarget(placement.targetDistance(), placement.playerDistance(),
                    best.targetDistance(), best.playerDistance())) {
                best = placement;
            }
        }
        return best;
    }

    private boolean eligible(double playerDistance, double targetDistance, double friendDistance, boolean explosive) {
        return explosive
                && playerDistance <= interactRange.value()
                && playerDistance >= minimumSelfDistance.value()
                && targetDistance <= blastRadius.value()
                && (!excludeFriends.value() || friendDistance > blastRadius.value());
    }

    private static boolean closerToTarget(double targetDistance, double playerDistance,
                                          double bestTargetDistance, double bestPlayerDistance) {
        return targetDistance < bestTargetDistance
                || (targetDistance == bestTargetDistance && playerDistance < bestPlayerDistance);
    }

    public boolean placeEnabled() {
        return place.value();
    }

    public boolean chargeEnabled() {
        return charge.value();
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

    public double interactRange() {
        return interactRange.value();
    }

    public double targetRange() {
        return targetRange.value();
    }

    public double blastRadius() {
        return blastRadius.value();
    }

    @Override
    protected void onDisable() {
        lastActionTick = -1L;
    }

    private static void requireOptionalSlot(int slot) {
        if (slot < -1 || slot > 8) {
            throw new IllegalArgumentException("hotbar slot must be absent or a valid index");
        }
    }

    private static void validateDistances(double playerDistance, double targetDistance, double friendDistance) {
        if (!Double.isFinite(playerDistance) || playerDistance < 0.0D
                || !Double.isFinite(targetDistance) || targetDistance < 0.0D
                || !Double.isFinite(friendDistance) || friendDistance < 0.0D) {
            throw new IllegalArgumentException("AutoAnchor distances must be finite and non-negative");
        }
    }
}
