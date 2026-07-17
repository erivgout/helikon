package dev.helikon.client.render;

/** Minecraft-free category, friend, and range gate shared by local entity visualizers. */
public final class EntityRenderFilter {
    private EntityRenderFilter() {
    }

    public static boolean shouldRender(Options options, EntityType entityType, boolean friend,
                                       boolean localPlayer, double distanceSquared) {
        if (options == null || entityType == null || localPlayer || !Double.isFinite(distanceSquared)
                || distanceSquared < 0.0D || distanceSquared > options.maximumDistance() * options.maximumDistance()) {
            return false;
        }
        if (friend) {
            return options.friends();
        }
        return switch (entityType) {
            case PLAYER -> options.players();
            case HOSTILE -> options.hostiles();
            case PASSIVE -> options.passive();
            case ITEM -> options.items();
            case PROJECTILE -> options.projectiles();
            case OTHER -> false;
        };
    }

    public enum EntityType {
        PLAYER,
        HOSTILE,
        PASSIVE,
        ITEM,
        PROJECTILE,
        OTHER
    }

    /** Immutable view of a module's category settings. */
    public record Options(boolean players, boolean hostiles, boolean passive, boolean items,
                          boolean projectiles, boolean friends, double maximumDistance) {
        public Options {
            if (!Double.isFinite(maximumDistance) || maximumDistance <= 0.0D) {
                throw new IllegalArgumentException("maximumDistance must be positive and finite");
            }
        }
    }
}
