package com.phantomcorridor.model.entity;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.ItemType;
import com.phantomcorridor.model.WorldType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 玩家运行时模型，不依赖任何 JavaFX 控件。 */
public final class Player {

    private int hp;
    private double x;
    private double y;
    private double phaseEnergy;
    private WorldType currentWorld;
    private final List<ItemType> items = new ArrayList<>();

    public Player(double x, double y) {
        reset(x, y);
    }

    public void reset(double x, double y) {
        this.hp = GameConfig.PLAYER_MAX_HP;
        this.x = x;
        this.y = y;
        this.phaseEnergy = GameConfig.PHASE_ENERGY_INITIAL;
        this.currentWorld = WorldType.LIGHT;
        this.items.clear();
    }

    public void move(double directionX, double directionY, double dt,
                     double minX, double minY, double maxX, double maxY) {
        double length = Math.hypot(directionX, directionY);
        if (length == 0.0) {
            return;
        }
        double speed = GameConfig.PLAYER_BASE_SPEED
                * (currentWorld == WorldType.SHADOW ? GameConfig.SHADOW_SPEED_MULTIPLIER : 1.0);
        x = clamp(x + directionX / length * speed * dt, minX, maxX);
        y = clamp(y + directionY / length * speed * dt, minY, maxY);
    }

    public void restorePhaseEnergy(double amount) {
        phaseEnergy = clamp(phaseEnergy + Math.max(0.0, amount), 0.0, GameConfig.PHASE_ENERGY_MAX);
    }

    public void consumePhaseEnergy(double amount) {
        phaseEnergy = clamp(phaseEnergy - Math.max(0.0, amount), 0.0, GameConfig.PHASE_ENERGY_MAX);
    }

    public void toggleWorld() {
        currentWorld = currentWorld == WorldType.LIGHT ? WorldType.SHADOW : WorldType.LIGHT;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public int getHp() { return hp; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getPhaseEnergy() { return phaseEnergy; }
    public WorldType getCurrentWorld() { return currentWorld; }
    public List<ItemType> getItems() { return Collections.unmodifiableList(items); }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
