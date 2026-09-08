package com.phantomcorridor.model;

import com.phantomcorridor.config.AppConfig;
import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.config.RoomConfig;
import com.phantomcorridor.model.effect.WorldShiftSystem;
import com.phantomcorridor.model.entity.Player;

/** 一局游戏的聚合状态。后续房间、敌人、掉落都从这里接入。 */
public final class GameSession {

    private final Player player = new Player(AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT / 2.0);
    private final WorldShiftSystem worldShift = new WorldShiftSystem();
    private boolean combatActive;
    private int lightEnemyCount;
    private int shadowEnemyCount;
    private int coins;

    public void newRun() {
        player.reset(AppConfig.VIEW_WIDTH / 2.0, AppConfig.VIEW_HEIGHT / 2.0);
        worldShift.reset();
        combatActive = false;
        lightEnemyCount = 0;
        shadowEnemyCount = 0;
        coins = 0;
    }

    public void update(double dt, double movementX, double movementY) {
        worldShift.update(dt);
        player.move(movementX, movementY, dt,
                RoomConfig.ROOM_INNER_PADDING, RoomConfig.ROOM_INNER_PADDING,
                AppConfig.VIEW_WIDTH - RoomConfig.ROOM_INNER_PADDING,
                AppConfig.VIEW_HEIGHT - RoomConfig.ROOM_INNER_PADDING);
        if (!combatActive) {
            player.restorePhaseEnergy(GameConfig.PHASE_ENERGY_REGEN_PER_SEC * dt);
        }
    }

    public boolean tryShiftWorld() {
        return worldShift.tryShift(player);
    }

    public Player getPlayer() { return player; }
    public WorldShiftSystem getWorldShift() { return worldShift; }
    public int getLightEnemyCount() { return lightEnemyCount; }
    public int getShadowEnemyCount() { return shadowEnemyCount; }
    public int getCoins() { return coins; }
}
