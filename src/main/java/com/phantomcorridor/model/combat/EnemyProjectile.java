package com.phantomcorridor.model.combat;

import com.phantomcorridor.model.WorldType;

/** 敌方弹幕的最小纯模型契约，供相位脉冲及后续敌人系统复用。 */
public final class EnemyProjectile {
    private final double x;
    private final double y;
    private final WorldType world;

    public EnemyProjectile(double x, double y, WorldType world) {
        this.x = x;
        this.y = y;
        this.world = world;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public WorldType getWorld() { return world; }
}
