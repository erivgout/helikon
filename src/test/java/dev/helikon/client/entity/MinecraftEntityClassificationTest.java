package dev.helikon.client.entity;

import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftEntityClassificationTest {
    @Test
    void treatsEnemyImplementationsAsHostileEvenWhenTheyDoNotExtendMonster() {
        assertTrue(MinecraftEntityClassification.isHostileType(Phantom.class));
        assertTrue(MinecraftEntityClassification.isHostileType(Zombie.class));
        assertFalse(MinecraftEntityClassification.isHostileType(Cow.class));
    }
}
