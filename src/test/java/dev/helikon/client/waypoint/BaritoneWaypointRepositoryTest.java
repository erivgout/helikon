package dev.helikon.client.waypoint;

import baritone.api.cache.IWaypoint;
import baritone.api.cache.IWaypointCollection;
import baritone.api.utils.BetterBlockPos;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaritoneWaypointRepositoryTest {
    private static final WaypointContext CONTEXT =
            new WaypointContext("world:baritone-test", "minecraft:overworld");
    private static final WaypointLocation LOCATION = new WaypointLocation(0, 64, 0, CONTEXT);

    @Test
    void mutationsOperateOnBaritoneCollectionAndPreserveTags() {
        MemoryCollection collection = new MemoryCollection();
        collection.addWaypoint(new baritone.api.cache.Waypoint(
                "Spawn", IWaypoint.Tag.BED, new BetterBlockPos(1, 65, 2), 10L));
        BaritoneWaypointRepository repository = repository(collection);

        assertTrue(repository.addAndSave("Mine", 20, 12, -4, CONTEXT));
        assertFalse(repository.addAndSave("mine", 0, 0, 0, CONTEXT));
        assertEquals("bed", repository.find("spawn", CONTEXT).orElseThrow().icon());
        assertTrue(repository.renameAndSave("Spawn", "Home Bed", CONTEXT));
        assertEquals(IWaypoint.Tag.BED, collection.named("Home Bed").orElseThrow().getTag());
        assertTrue(repository.removeAndSave("Mine", CONTEXT));
        assertTrue(repository.find("mine", CONTEXT).isEmpty());
    }

    @Test
    void migratesLegacyEntriesOnlyForCurrentContextWithoutDuplicates() {
        MemoryCollection collection = new MemoryCollection();
        collection.addWaypoint(new baritone.api.cache.Waypoint(
                "Existing", IWaypoint.Tag.USER, new BetterBlockPos(1, 2, 3), 1L));
        BaritoneWaypointRepository repository = repository(collection);
        Waypoint otherWorld = new Waypoint("Other", 0, 0, 0,
                new WaypointContext("world:other", "minecraft:overworld"),
                Waypoint.DEFAULT_COLOR, "", true, 2L);

        int migrated = repository.migrateCurrent(java.util.List.of(
                waypoint("Existing", 9L), waypoint("Legacy", 42L), otherWorld));

        assertEquals(1, migrated);
        assertEquals(42L, collection.named("Legacy").orElseThrow().getCreationTimestamp());
        assertEquals(2, collection.getAllWaypoints().size());
    }

    private static BaritoneWaypointRepository repository(MemoryCollection collection) {
        return new BaritoneWaypointRepository(() -> Optional.of(LOCATION), () -> collection);
    }

    private static Waypoint waypoint(String name, long createdAt) {
        return new Waypoint(name, 4, 70, -2, CONTEXT,
                Waypoint.DEFAULT_COLOR, "", true, createdAt);
    }

    private static final class MemoryCollection implements IWaypointCollection {
        private final EnumMap<IWaypoint.Tag, Set<IWaypoint>> byTag = new EnumMap<>(IWaypoint.Tag.class);

        private MemoryCollection() {
            for (IWaypoint.Tag tag : IWaypoint.Tag.values()) {
                byTag.put(tag, new HashSet<>());
            }
        }

        @Override
        public void addWaypoint(IWaypoint waypoint) {
            byTag.get(waypoint.getTag()).add(waypoint);
        }

        @Override
        public void removeWaypoint(IWaypoint waypoint) {
            byTag.get(waypoint.getTag()).remove(waypoint);
        }

        @Override
        public IWaypoint getMostRecentByTag(IWaypoint.Tag tag) {
            return byTag.get(tag).stream()
                    .max(java.util.Comparator.comparingLong(IWaypoint::getCreationTimestamp))
                    .orElse(null);
        }

        @Override
        public Set<IWaypoint> getByTag(IWaypoint.Tag tag) {
            return Set.copyOf(byTag.get(tag));
        }

        @Override
        public Set<IWaypoint> getAllWaypoints() {
            Set<IWaypoint> all = new HashSet<>();
            byTag.values().forEach(all::addAll);
            return all;
        }

        private Optional<IWaypoint> named(String name) {
            return getAllWaypoints().stream().filter(value -> value.getName().equals(name)).findFirst();
        }
    }
}
