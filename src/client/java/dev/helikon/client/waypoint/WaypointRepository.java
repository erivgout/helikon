package dev.helikon.client.waypoint;

import java.util.List;
import java.util.Optional;

/** Storage boundary shared by the legacy migration reader and Baritone's live waypoint collection. */
public interface WaypointRepository {
    List<Waypoint> forContext(WaypointContext context);

    List<Waypoint> snapshotForContext(WaypointContext context);

    default List<Waypoint> visible(WaypointContext context) {
        return snapshotForContext(context).stream().filter(Waypoint::enabled).toList();
    }

    Optional<Waypoint> find(String name, WaypointContext context);

    long revision();

    boolean addAndSave(String name, int x, int y, int z, WaypointContext context);

    boolean removeAndSave(String name, WaypointContext context);

    boolean renameAndSave(String sourceName, String targetName, WaypointContext context);
}
