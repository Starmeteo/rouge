package com.phantomcorridor.model;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.combat.EnemyProjectile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    @Test
    void worldShiftConsumesPulseAndClearsNearbyEnemyProjectiles() {
        GameSession session = new GameSession();
        session.newRun();
        double x = session.getPlayer().getX();
        double y = session.getPlayer().getY();
        session.getEnemyProjectiles().add(new EnemyProjectile(x + 20.0, y, WorldType.LIGHT));
        session.getEnemyProjectiles().add(new EnemyProjectile(
                x + GameConfig.PHASE_PULSE_RADIUS + 20.0, y, WorldType.LIGHT));

        assertTrue(session.tryShiftWorld());
        assertTrue(session.isPhasePulseVisible());
        assertEquals(1, session.getEnemyProjectiles().getProjectiles().size());
        assertFalse(session.getWorldShift().consumePulse(), "脉冲事件必须由会话层即时消费");
    }
}
