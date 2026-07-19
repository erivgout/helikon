package dev.helikon.client.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;

/** Shared Minecraft entity categories that follow capability interfaces instead of fragile class hierarchies. */
public final class MinecraftEntityClassification {
    private MinecraftEntityClassification() {
    }

    /** Includes hostile mobs such as Phantoms that implement Enemy without extending Monster. */
    public static boolean isHostile(Entity entity) {
        return entity != null && isHostileType(entity.getClass());
    }

    static boolean isHostileType(Class<?> entityType) {
        return entityType != null && Enemy.class.isAssignableFrom(entityType);
    }
}
