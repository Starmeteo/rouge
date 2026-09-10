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
    private final String effectId;
    private final double angleRadians;
    private double startupDelay;
    private double remainingLifetime;
    private boolean impactPlayed;

    public EnemyAttack(double x, double y, double velocityX, double velocityY, double radius,
                       WorldType world, EnemyKind source, double lifetime) {
        this(x, y, velocityX, velocityY, radius, world, source, lifetime, "", 0.0, 0.0);
    }

    public EnemyAttack(double x, double y, double velocityX, double velocityY, double radius,
                       WorldType world, EnemyKind source, double lifetime, String effectId,
                       double angleRadians, double startupDelay) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.radius = radius;
        this.world = world;
        this.source = source;
        this.remainingLifetime = lifetime;
        this.effectId = effectId;
        this.angleRadians = angleRadians;
        this.startupDelay = startupDelay;
    }

    public void update(double dt) {
        if (startupDelay > 0.0) { startupDelay -= dt; return; }
        x += velocityX * dt; y += velocityY * dt; remainingLifetime -= dt;
    }
    public void expire() { remainingLifetime = 0.0; }
    public boolean isExpired() { return remainingLifetime <= 0.0; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public WorldType getWorld() { return world; }
    public EnemyKind getSource() { return source; }
    public String getEffectId() { return effectId; }
    public double getAngleRadians() { return angleRadians; }
    public boolean isActive() { return startupDelay <= 0.0; }
    public boolean isMoving() { return Math.abs(velocityX) > 0.001 || Math.abs(velocityY) > 0.001; }
    /** 命中、撞墙或寿命结束时只允许生成一次独立消散/命中特效。 */
    public boolean consumeImpact() {
        if (impactPlayed) return false;
        impactPlayed = true;
        return true;
    }
}
