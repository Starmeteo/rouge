package com.phantomcorridor.model.combat;

import com.phantomcorridor.model.WorldType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnemyProjectileSystemTest {

    @Test
    void pulseClearsOnlyProjectilesInsideRadius() {
        EnemyProjectileSystem system = new EnemyProjectileSystem();
        system.add(new EnemyProjectile(105.0, 100.0, WorldType.LIGHT));
        system.add(new EnemyProjectile(170.0, 100.0, WorldType.SHADOW));

        assertEquals(1, system.clearWithin(100.0, 100.0, 50.0));
        assertEquals(1, system.getProjectiles().size());
        assertEquals(170.0, system.getProjectiles().getFirst().getX());
    }
}
