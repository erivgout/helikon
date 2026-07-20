package dev.helikon.client.module.combat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Ordered placement plan split into construction phases and a separately deferred self-exit. */
public record DomainPlacementPlan(
        DomainBounds bounds,
        List<Entry> entries,
        List<DomainPosition> doorway
) {
    public enum Part {
        WALL,
        ROOF_PERIMETER,
        ROOF,
        FLOOR
    }

    public record Entry(DomainPosition position, Part part) {
        public Entry {
            position = Objects.requireNonNull(position, "position");
            part = Objects.requireNonNull(part, "part");
        }
    }

    public DomainPlacementPlan {
        bounds = Objects.requireNonNull(bounds, "bounds");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        doorway = List.copyOf(Objects.requireNonNull(doorway, "doorway"));
        Set<DomainPosition> unique = new LinkedHashSet<>();
        for (Entry entry : entries) {
            if (!unique.add(entry.position())) {
                throw new IllegalArgumentException("Domain placement plan contains duplicate positions");
            }
        }
        for (DomainPosition position : doorway) {
            if (unique.contains(position)) {
                throw new IllegalArgumentException("Deferred doorway must not be part of the required plan");
            }
        }
    }

    public List<Entry> entries(Part part) {
        return entries.stream().filter(entry -> entry.part() == part).toList();
    }

    public int requiredBlocks() {
        return entries.size();
    }
}
