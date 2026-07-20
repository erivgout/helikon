package dev.helikon.client.module.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Generates a deterministic escape-aware plan without inspecting or changing the Minecraft world. */
public final class DomainPlanGenerator {
    public enum ExitMode {
        NO_EXIT,
        ONE_BLOCK_DOOR,
        TWO_BLOCK_DOOR,
        MANUAL_FINAL_SEAL
    }

    private enum Side {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    private DomainPlanGenerator() {
    }

    public static DomainPlacementPlan generate(
            DomainBounds bounds,
            DomainPosition localFeet,
            DomainTarget target,
            boolean buildRoof,
            boolean buildFloor,
            ExitMode exitMode,
            boolean closeEscapeSideFirst
    ) {
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(localFeet, "localFeet");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(exitMode, "exitMode");

        List<Side> sideOrder = sideOrder(target, closeEscapeSideFirst);
        Set<DomainPosition> doorway = doorway(bounds, localFeet, exitMode);
        List<DomainPlacementPlan.Entry> entries = new ArrayList<>();
        addLowerWalls(entries, bounds, target, sideOrder, doorway);
        addUpperWalls(entries, bounds, sideOrder, doorway);
        if (buildRoof) {
            addRoof(entries, bounds);
        }
        if (buildFloor) {
            addFloor(entries, bounds);
        }
        return new DomainPlacementPlan(bounds, entries, List.copyOf(doorway));
    }

    private static void addLowerWalls(List<DomainPlacementPlan.Entry> result, DomainBounds bounds,
                                      DomainTarget target, List<Side> sideOrder, Set<DomainPosition> doorway) {
        Map<Side, Integer> ranks = ranks(sideOrder);
        List<WallPosition> lower = wallLayer(bounds, bounds.floorY()).stream()
                .filter(position -> !doorway.contains(position.position()))
                .sorted(Comparator.comparingInt((WallPosition position) -> ranks.get(position.side()))
                        .thenComparingLong(position -> position.position()
                                .horizontalDistanceSquared(target.x(), target.z()))
                        .thenComparingInt(position -> position.position().x())
                        .thenComparingInt(position -> position.position().z()))
                .toList();

        Set<DomainPosition> critical = new LinkedHashSet<>();
        lower.stream()
                .sorted(Comparator.comparingLong(position -> position.position()
                        .horizontalDistanceSquared(target.x(), target.z())))
                .limit(Math.min(6, lower.size()))
                .forEach(position -> critical.add(position.position()));
        for (WallPosition position : lower) {
            if (critical.contains(position.position())) {
                result.add(new DomainPlacementPlan.Entry(position.position(), DomainPlacementPlan.Part.WALL));
            }
        }
        for (WallPosition position : lower) {
            if (!critical.contains(position.position())) {
                result.add(new DomainPlacementPlan.Entry(position.position(), DomainPlacementPlan.Part.WALL));
            }
        }
    }

    private static void addUpperWalls(List<DomainPlacementPlan.Entry> result, DomainBounds bounds,
                                      List<Side> sideOrder, Set<DomainPosition> doorway) {
        Map<Side, Integer> ranks = ranks(sideOrder);
        for (int y = bounds.floorY() + 1; y < bounds.roofY(); y++) {
            wallLayer(bounds, y).stream()
                    .filter(position -> !doorway.contains(position.position()))
                    .sorted(Comparator.comparingInt(position -> ranks.get(position.side())))
                    .forEach(position -> result.add(new DomainPlacementPlan.Entry(
                            position.position(), DomainPlacementPlan.Part.WALL)));
        }
    }

    private static void addRoof(List<DomainPlacementPlan.Entry> result, DomainBounds bounds) {
        int y = bounds.roofY();
        for (int x = bounds.wallMinX(); x <= bounds.wallMaxX(); x++) {
            result.add(new DomainPlacementPlan.Entry(new DomainPosition(x, y, bounds.wallMinZ()),
                    DomainPlacementPlan.Part.ROOF_PERIMETER));
            if (bounds.wallMaxZ() != bounds.wallMinZ()) {
                result.add(new DomainPlacementPlan.Entry(new DomainPosition(x, y, bounds.wallMaxZ()),
                        DomainPlacementPlan.Part.ROOF_PERIMETER));
            }
        }
        for (int z = bounds.wallMinZ() + 1; z < bounds.wallMaxZ(); z++) {
            result.add(new DomainPlacementPlan.Entry(new DomainPosition(bounds.wallMinX(), y, z),
                    DomainPlacementPlan.Part.ROOF_PERIMETER));
            if (bounds.wallMaxX() != bounds.wallMinX()) {
                result.add(new DomainPlacementPlan.Entry(new DomainPosition(bounds.wallMaxX(), y, z),
                        DomainPlacementPlan.Part.ROOF_PERIMETER));
            }
        }
        for (int x = bounds.wallMinX() + 1; x < bounds.wallMaxX(); x++) {
            for (int z = bounds.wallMinZ() + 1; z < bounds.wallMaxZ(); z++) {
                result.add(new DomainPlacementPlan.Entry(new DomainPosition(x, y, z),
                        DomainPlacementPlan.Part.ROOF));
            }
        }
    }

    private static void addFloor(List<DomainPlacementPlan.Entry> result, DomainBounds bounds) {
        for (int x = bounds.wallMinX(); x <= bounds.wallMaxX(); x++) {
            for (int z = bounds.wallMinZ(); z <= bounds.wallMaxZ(); z++) {
                result.add(new DomainPlacementPlan.Entry(new DomainPosition(x, bounds.floorBlockY(), z),
                        DomainPlacementPlan.Part.FLOOR));
            }
        }
    }

    private static List<WallPosition> wallLayer(DomainBounds bounds, int y) {
        List<WallPosition> result = new ArrayList<>();
        for (int x = bounds.wallMinX(); x <= bounds.wallMaxX(); x++) {
            result.add(new WallPosition(new DomainPosition(x, y, bounds.wallMinZ()), Side.NORTH));
            result.add(new WallPosition(new DomainPosition(x, y, bounds.wallMaxZ()), Side.SOUTH));
        }
        for (int z = bounds.wallMinZ() + 1; z < bounds.wallMaxZ(); z++) {
            result.add(new WallPosition(new DomainPosition(bounds.wallMinX(), y, z), Side.WEST));
            result.add(new WallPosition(new DomainPosition(bounds.wallMaxX(), y, z), Side.EAST));
        }
        return result;
    }

    private static Set<DomainPosition> doorway(DomainBounds bounds, DomainPosition localFeet, ExitMode mode) {
        if (mode == ExitMode.NO_EXIT) {
            return Set.of();
        }
        Side nearest = nearestSide(bounds, localFeet);
        DomainPosition lower = switch (nearest) {
            case NORTH -> new DomainPosition(clamp(localFeet.x(), bounds.interiorMinX(), bounds.interiorMaxX()),
                    bounds.floorY(), bounds.wallMinZ());
            case SOUTH -> new DomainPosition(clamp(localFeet.x(), bounds.interiorMinX(), bounds.interiorMaxX()),
                    bounds.floorY(), bounds.wallMaxZ());
            case WEST -> new DomainPosition(bounds.wallMinX(), bounds.floorY(),
                    clamp(localFeet.z(), bounds.interiorMinZ(), bounds.interiorMaxZ()));
            case EAST -> new DomainPosition(bounds.wallMaxX(), bounds.floorY(),
                    clamp(localFeet.z(), bounds.interiorMinZ(), bounds.interiorMaxZ()));
        };
        if (mode == ExitMode.ONE_BLOCK_DOOR) {
            return Set.of(lower);
        }
        return Set.of(lower, lower.offset(0, 1, 0));
    }

    private static Side nearestSide(DomainBounds bounds, DomainPosition localFeet) {
        int north = Math.abs(localFeet.z() - bounds.wallMinZ());
        int east = Math.abs(bounds.wallMaxX() - localFeet.x());
        int south = Math.abs(bounds.wallMaxZ() - localFeet.z());
        int west = Math.abs(localFeet.x() - bounds.wallMinX());
        int minimum = Math.min(Math.min(north, east), Math.min(south, west));
        if (north == minimum) {
            return Side.NORTH;
        }
        if (east == minimum) {
            return Side.EAST;
        }
        if (south == minimum) {
            return Side.SOUTH;
        }
        return Side.WEST;
    }

    private static List<Side> sideOrder(DomainTarget target, boolean closeEscapeSideFirst) {
        if (!closeEscapeSideFirst) {
            return List.of(Side.NORTH, Side.EAST, Side.SOUTH, Side.WEST);
        }
        double x = target.escapeX();
        double z = target.escapeZ();
        Side front = Math.abs(x) >= Math.abs(z)
                ? (x >= 0.0D ? Side.EAST : Side.WEST)
                : (z >= 0.0D ? Side.SOUTH : Side.NORTH);
        return switch (front) {
            case NORTH -> List.of(Side.NORTH, Side.EAST, Side.WEST, Side.SOUTH);
            case EAST -> List.of(Side.EAST, Side.SOUTH, Side.NORTH, Side.WEST);
            case SOUTH -> List.of(Side.SOUTH, Side.WEST, Side.EAST, Side.NORTH);
            case WEST -> List.of(Side.WEST, Side.NORTH, Side.SOUTH, Side.EAST);
        };
    }

    private static Map<Side, Integer> ranks(List<Side> order) {
        Map<Side, Integer> result = new EnumMap<>(Side.class);
        for (int index = 0; index < order.size(); index++) {
            result.put(order.get(index), index);
        }
        return result;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record WallPosition(DomainPosition position, Side side) {
    }
}
