package com.phantomcorridor.model.combat;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.entity.Player;
import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.dungeon.DungeonMap;
import com.phantomcorridor.model.room.Room;
import com.phantomcorridor.model.room.RoomArea;
import com.phantomcorridor.model.room.RoomNavigationSystem;
import com.phantomcorridor.model.room.RoomShape;
import com.phantomcorridor.model.room.Wall;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void lightProjectileStopsAtCurrentRoomWall() {
        Room room = new Room(0, RoomType.ENTRANCE, 0, 0, RoomShape.RECTANGLE,
                List.of(new RoomArea(100, 100, 600, 500)),
                List.of(new Wall(250, 100, 24, 500, null)));
        RoomNavigationSystem navigation = new RoomNavigationSystem();
        navigation.reset(new DungeonMap(List.of(room)));
        Player player = new Player(200, 300);
        PlayerAttackSystem attacks = new PlayerAttackSystem();

        attacks.tryAttack(player, 500, 300);
        attacks.update(0.2, navigation);

        assertTrue(attacks.getProjectiles().isEmpty());
    }
}
