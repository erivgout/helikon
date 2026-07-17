package dev.helikon.client.render;

import java.util.LinkedHashSet;
import java.util.Set;

/** Builds the bounded local block-entity target set without Minecraft registry access. */
public final class StorageEspTargets {
    private static final Set<String> CHESTS = Set.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest");
    private static final Set<String> BARRELS = Set.of("minecraft:barrel");
    private static final Set<String> SHULKERS = Set.of(
            "minecraft:shulker_box", "minecraft:white_shulker_box", "minecraft:orange_shulker_box",
            "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box",
            "minecraft:lime_shulker_box", "minecraft:pink_shulker_box", "minecraft:gray_shulker_box",
            "minecraft:light_gray_shulker_box", "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box",
            "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:green_shulker_box",
            "minecraft:red_shulker_box", "minecraft:black_shulker_box"
    );
    private static final Set<String> FURNACES = Set.of("minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker");
    private static final Set<String> HOPPERS = Set.of("minecraft:hopper");
    private static final Set<String> SPAWNERS = Set.of("minecraft:spawner");

    private StorageEspTargets() {
    }

    public static Set<String> resolve(boolean chests, boolean barrels, boolean shulkers, boolean furnaces,
                                      boolean hoppers, boolean spawners, String configuredBlockEntities) {
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        if (chests) {
            targets.addAll(CHESTS);
        }
        if (barrels) {
            targets.addAll(BARRELS);
        }
        if (shulkers) {
            targets.addAll(SHULKERS);
        }
        if (furnaces) {
            targets.addAll(FURNACES);
        }
        if (hoppers) {
            targets.addAll(HOPPERS);
        }
        if (spawners) {
            targets.addAll(SPAWNERS);
        }
        targets.addAll(BlockIdList.parse(configuredBlockEntities));
        return Set.copyOf(targets);
    }
}
