package com.phantomcorridor.model.entity;

import com.phantomcorridor.config.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerTest {

    @Test
    void diagonalMovementIsNormalized() {
        Player player = new Player(100, 100);
        player.move(1, 1, 1.0, 0, 0, 1000, 1000);

        double travelled = Math.hypot(player.getX() - 100, player.getY() - 100);
        assertEquals(GameConfig.PLAYER_BASE_SPEED, travelled, 0.0001);
    }

    @Test
    void movementIsClampedToRoomBounds() {
        Player player = new Player(100, 100);
        player.move(-1, 0, 10.0, 50, 50, 200, 200);
        assertEquals(50, player.getX());
    }
}
