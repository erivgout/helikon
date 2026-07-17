package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberRange;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.RangeSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Selects a bounded nearest-first set of distant, locally loaded players. */
public final class PlayerFinder extends Module {
    private final RangeSetting distance;
    private final BooleanSetting includeFriends;
    private final BooleanSetting includeSpectators;
    private final BooleanSetting tracers;
    private final BooleanSetting beacons;
    private final BooleanSetting labels;
    private final NumberSetting beaconHeight;
    private final BooleanSetting alwaysOnTop;
    private final IntegerSetting maximumPlayers;
    private final NumberSetting lineWidth;
    private final ColorSetting color;
    private final ColorSetting friendColor;

    public PlayerFinder() {
        super("player_finder", "PlayerFinder",
                "Marks distant players already loaded by the local client.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        distance = addSetting(new RangeSetting("distance", "Distance",
                "Inclusive local distance band in blocks.", new NumberRange(64.0D, 1_024.0D),
                0.0D, 4_096.0D));
        includeFriends = addSetting(new BooleanSetting("include_friends", "Include friends",
                "Include locally saved friends in finder results.", false));
        includeSpectators = addSetting(new BooleanSetting("include_spectators", "Include spectators",
                "Include locally loaded spectator-mode players.", false));
        tracers = addSetting(new BooleanSetting("tracers", "Tracers",
                "Draw lines from the local view toward selected players.", true));
        beacons = addSetting(new BooleanSetting("beacons", "Beacons",
                "Draw vertical world-space lines at selected players.", true));
        labels = addSetting(new BooleanSetting("labels", "Labels",
                "Draw player-name and rounded-distance labels.", true));
        beaconHeight = addSetting(new NumberSetting("beacon_height", "Beacon height",
                "Height of each local vertical finder line in blocks.", 48.0D, 8.0D, 256.0D));
        alwaysOnTop = addSetting(new BooleanSetting("always_on_top", "Always on top",
                "Draw finder visuals through nearby terrain.", true));
        maximumPlayers = addSetting(new IntegerSetting("maximum_players", "Maximum players",
                "Hard cap for nearest finder results per frame.", 16, 1, 64));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width",
                "Local tracer and beacon line width.", 1.5D, 0.5D, 4.0D));
        color = addSetting(new ColorSetting("color", "Color",
                "ARGB color for non-friend finder visuals.", 0xFFFF7043));
        friendColor = addSetting(new ColorSetting("friend_color", "Friend color",
                "ARGB color for included friend finder visuals.", 0xFF61D17B));
    }

    /** Filters and orders immutable Minecraft-free observations for the render adapter. */
    public List<Candidate> select(List<Candidate> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        NumberRange range = distance.value();
        double minimumSquared = range.minimum() * range.minimum();
        double maximumSquared = range.maximum() * range.maximum();
        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(candidate -> candidate.distanceSquared() >= minimumSquared
                        && candidate.distanceSquared() <= maximumSquared)
                .filter(candidate -> includeFriends.value() || !candidate.friend())
                .filter(candidate -> includeSpectators.value() || !candidate.spectator())
                .sorted(Comparator.comparingDouble(Candidate::distanceSquared)
                        .thenComparingInt(Candidate::entityId))
                .limit(maximumPlayers.value())
                .toList();
    }

    public boolean tracers() {
        return tracers.value();
    }

    public boolean beacons() {
        return beacons.value();
    }

    public boolean labels() {
        return labels.value();
    }

    public double beaconHeight() {
        return beaconHeight.value();
    }

    public boolean alwaysOnTop() {
        return alwaysOnTop.value();
    }

    public float lineWidth() {
        return (float) lineWidth.value().doubleValue();
    }

    public int color(boolean friend) {
        return friend ? friendColor.value() : color.value();
    }

    public String label(Candidate candidate) {
        Objects.requireNonNull(candidate, "candidate");
        return candidate.name() + " " + Math.round(Math.sqrt(candidate.distanceSquared())) + "m";
    }

    public record Candidate(int entityId, String name, double distanceSquared, boolean friend, boolean spectator) {
        public Candidate {
            name = Objects.requireNonNull(name, "name").trim();
            if (name.isEmpty() || !Double.isFinite(distanceSquared) || distanceSquared < 0.0D) {
                throw new IllegalArgumentException("Candidate fields must be valid");
            }
        }
    }
}
