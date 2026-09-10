package com.phantomcorridor.model.combat;

import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.entity.EnemyKind;

/** 敌人释放后的独立攻击对象；本体动作与飞行/命中特效不绑定。 */
public final class EnemyAttack {
    private double x;
    private double y;
    private final double velocityX;
    private final double velocityY;
    private final double radius;
    private final WorldType world;
    private final EnemyKind source;
    private double remainingLifetime;

    public EnemyAttack(double x, double y, double velocityX, double velocityY, double radius,
                       WorldType world, EnemyKind source, double lifetime) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.radius = radius;
        this.world = world;
        this.source = source;
        this.remainingLifetime = lifetime;
    }

    public void update(double dt) { x += velocityX * dt; y += velocityY * dt; remainingLifetime -= dt; }
    public void expire() { remainingLifetime = 0.0; }
    public boolean isExpired() { return remainingLifetime <= 0.0; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public WorldType getWorld() { return world; }
    public EnemyKind getSource() { return source; }
}
