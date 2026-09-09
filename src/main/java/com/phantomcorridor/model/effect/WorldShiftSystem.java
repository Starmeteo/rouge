package com.phantomcorridor.model.effect;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.entity.Player;

/** 世界切换规则：满能量、冷却、切换消耗以及一次性相位脉冲。 */
public final class WorldShiftSystem {

    private double cooldownRemaining;
    private boolean pulsePending;

    public void reset() {
        cooldownRemaining = 0.0;
        pulsePending = false;
    }

    public void update(double dt) {
        cooldownRemaining = Math.max(0.0, cooldownRemaining - Math.max(0.0, dt));
    }

    public boolean tryShift(Player player) {
        if (cooldownRemaining > 0.0
                || player.getPhaseEnergy() < GameConfig.PHASE_ENERGY_PER_SWITCH) {
            return false;
        }
        player.toggleWorld();
        player.consumePhaseEnergy(GameConfig.PHASE_ENERGY_PER_SWITCH);
        cooldownRemaining = GameConfig.WORLD_SWITCH_COOLDOWN;
        pulsePending = true;
        return true;
    }

    public boolean consumePulse() {
        boolean result = pulsePending;
        pulsePending = false;
        return result;
    }

    public double getCooldownRemaining() {
        return cooldownRemaining;
    }
}
