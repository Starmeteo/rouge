package com.phantomcorridor.model.combat;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerAttackSystemTest {

    @Test
    void lightAttackCreatesProjectileAndHonorsCooldown() {
        Player player = new Player(100.0, 100.0);
        PlayerAttackSystem attacks = new PlayerAttackSystem();

        assertTrue(attacks.tryAttack(player, 200.0, 100.0));
        assertEquals(1, attacks.getProjectiles().size());
        assertFalse(attacks.tryAttack(player, 200.0, 100.0));

        double startX = attacks.getProjectiles().getFirst().getX();
        attacks.update(GameConfig.LIGHT_ATTACK_COOLDOWN);
        assertTrue(attacks.getProjectiles().getFirst().getX() > startX);
        assertTrue(attacks.tryAttack(player, 200.0, 100.0));
    }

    @Test
    void shadowAttackCreatesTemporaryMeleeArc() {
        Player player = new Player(100.0, 100.0);
        player.toggleWorld();
        PlayerAttackSystem attacks = new PlayerAttackSystem();

        assertTrue(attacks.tryAttack(player, 100.0, 200.0));
        assertTrue(attacks.isMeleeVisible());
        assertTrue(attacks.getProjectiles().isEmpty());
        assertEquals(Math.PI / 2.0, attacks.getMeleeAngleRadians(), 0.0001);

        attacks.update(GameConfig.SHADOW_MELEE_VISIBLE_TIME + 0.01);
        assertFalse(attacks.isMeleeVisible());
    }

    @Test
    void expiredProjectilesAreRemoved() {
        Player player = new Player(100.0, 100.0);
        PlayerAttackSystem attacks = new PlayerAttackSystem();
        attacks.tryAttack(player, 200.0, 100.0);

        attacks.update(GameConfig.LIGHT_PROJECTILE_LIFETIME + 0.01);
        assertTrue(attacks.getProjectiles().isEmpty());
    }
}
