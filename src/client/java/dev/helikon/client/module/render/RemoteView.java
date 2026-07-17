package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EntitySelectorSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Chooses one already-loaded entity for a reversible local camera view. */
public final class RemoteView extends Module {
    public enum TargetMode {
        CROSSHAIR,
        NEAREST,
        NAMED_PLAYER
    }

    private final EnumSetting<TargetMode> targetMode;
    private final EntitySelectorSetting entityTypes;
    private final StringSetting targetName;
    private final NumberSetting range;
    private final BooleanSetting includeFriends;
    private final BooleanSetting includeInvisible;
    private final BooleanSetting includeSpectators;
    private final BooleanSetting retargetOnLoss;
    private long selectionRevision;
    private Runnable viewRestorer = () -> {
    };

    public RemoteView() {
        super("remote_view", "RemoteView",
                "Locally views the perspective of one already-loaded entity.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        targetMode = addSetting(new EnumSetting<>("target_mode", "Target mode",
                "Choose the aimed entity, nearest eligible entity, or a named loaded player.",
                TargetMode.class, TargetMode.CROSSHAIR));
        entityTypes = addSetting(new EntitySelectorSetting("entity_types", "Entity types",
                "Eligible loaded entity IDs.", List.of("minecraft:player"), 16));
        targetName = addSetting(new StringSetting("target_name", "Target name",
                "Exact loaded player name used by Named Player mode.", "", 64, true,
                () -> targetMode.value() == TargetMode.NAMED_PLAYER));
        range = addSetting(new NumberSetting("range", "Range",
                "Maximum local target-selection distance in blocks.", 128.0D, 4.0D, 1_024.0D));
        includeFriends = addSetting(new BooleanSetting("include_friends", "Include friends",
                "Allow locally saved friends to be selected.", false));
        includeInvisible = addSetting(new BooleanSetting("include_invisible", "Include invisible",
                "Allow entities currently invisible to the local player.", false));
        includeSpectators = addSetting(new BooleanSetting("include_spectators", "Include spectators",
                "Allow loaded spectator-mode players to be selected.", false));
        retargetOnLoss = addSetting(new BooleanSetting("retarget_on_loss", "Retarget on loss",
                "Automatically choose another eligible entity when the current target unloads.", false));
        targetMode.addChangeListener(ignored -> selectionRevision++);
        entityTypes.addChangeListener(ignored -> selectionRevision++);
        targetName.addChangeListener(ignored -> selectionRevision++);
        range.addChangeListener(ignored -> selectionRevision++);
        includeFriends.addChangeListener(ignored -> selectionRevision++);
        includeInvisible.addChangeListener(ignored -> selectionRevision++);
        includeSpectators.addChangeListener(ignored -> selectionRevision++);
        retargetOnLoss.addChangeListener(ignored -> selectionRevision++);
    }

    /** Filters Minecraft-free observations and chooses exactly one deterministic target. */
    public Optional<Candidate> select(List<Candidate> candidates, Integer crosshairEntityId) {
        Objects.requireNonNull(candidates, "candidates");
        Set<String> allowedTypes = Set.copyOf(entityTypes.value());
        double maximumDistanceSquared = range.value() * range.value();
        List<Candidate> eligible = candidates.stream()
                .filter(Objects::nonNull)
                .filter(candidate -> allowedTypes.contains(candidate.typeId()))
                .filter(candidate -> candidate.distanceSquared() <= maximumDistanceSquared)
                .filter(candidate -> includeFriends.value() || !candidate.friend())
                .filter(candidate -> includeInvisible.value() || !candidate.invisible())
                .filter(candidate -> includeSpectators.value() || !candidate.spectator())
                .toList();
        return switch (targetMode.value()) {
            case CROSSHAIR -> crosshairEntityId == null ? Optional.empty() : eligible.stream()
                    .filter(candidate -> candidate.entityId() == crosshairEntityId)
                    .findFirst();
            case NEAREST -> eligible.stream().min(Comparator.comparingDouble(Candidate::distanceSquared)
                    .thenComparingInt(Candidate::entityId));
            case NAMED_PLAYER -> {
                String requestedName = targetName.value().trim();
                yield requestedName.isEmpty() ? Optional.empty() : eligible.stream()
                        .filter(candidate -> candidate.typeId().equals("minecraft:player"))
                        .filter(candidate -> candidate.name().equalsIgnoreCase(requestedName))
                        .min(Comparator.comparingDouble(Candidate::distanceSquared)
                                .thenComparingInt(Candidate::entityId));
            }
        };
    }

    public long selectionRevision() {
        return selectionRevision;
    }

    public boolean retargetOnLoss() {
        return retargetOnLoss.value();
    }

    /** Installs the narrow camera cleanup hook without introducing Minecraft types. */
    public void setViewRestorer(Runnable viewRestorer) {
        this.viewRestorer = Objects.requireNonNull(viewRestorer, "viewRestorer");
    }

    @Override
    protected void onDisable() {
        viewRestorer.run();
    }

    public record Candidate(
            int entityId,
            String typeId,
            String name,
            double distanceSquared,
            boolean friend,
            boolean invisible,
            boolean spectator
    ) {
        public Candidate {
            typeId = Objects.requireNonNull(typeId, "typeId").trim().toLowerCase(Locale.ROOT);
            name = Objects.requireNonNull(name, "name").trim();
            if (typeId.isEmpty() || name.isEmpty()
                    || !Double.isFinite(distanceSquared) || distanceSquared < 0.0D) {
                throw new IllegalArgumentException("Candidate fields must be valid");
            }
        }
    }
}
