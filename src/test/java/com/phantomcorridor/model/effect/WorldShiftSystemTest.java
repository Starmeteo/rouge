package com.phantomcorridor.model.effect;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldShiftSystemTest {

    @Test
    void shiftConsumesEnergyAndEmitsOnePulse() {
        Player player = new Player(100, 100);
        WorldShiftSystem system = new WorldShiftSystem();

        assertTrue(system.tryShift(player));
        assertEquals(WorldType.SHADOW, player.getCurrentWorld());
        assertEquals(0.0, player.getPhaseEnergy());
        assertTrue(system.consumePulse());
        assertFalse(system.consumePulse());
        assertEquals(GameConfig.WORLD_SWITCH_COOLDOWN, system.getCooldownRemaining());
    }

    @Test
    void cannotShiftWithoutFullEnergy() {
        Player player = new Player(100, 100);
        WorldShiftSystem system = new WorldShiftSystem();
        assertTrue(system.tryShift(player));
        system.update(GameConfig.WORLD_SWITCH_COOLDOWN);

        assertFalse(system.tryShift(player));
        assertEquals(WorldType.SHADOW, player.getCurrentWorld());
    }
}
