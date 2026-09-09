package com.phantomcorridor.model.combat;

import com.phantomcorridor.model.WorldType;

/** 不依赖 JavaFX 的光形态投射物模型。 */
public final class Projectile {
    private double x;
    private double y;
    private final double velocityX;
    private final double velocityY;
    private final double radius;
    private final WorldType world;
    private double remainingLifetime;

    public Projectile(double x, double y, double velocityX, double velocityY,
                      double radius, WorldType world, double lifetime) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.radius = radius;
        this.world = world;
        this.remainingLifetime = lifetime;
    }

    public void update(double dt) {
        x += velocityX * dt;
        y += velocityY * dt;
        remainingLifetime -= dt;
    }

    public void expire() { remainingLifetime = 0.0; }

    public boolean isExpired() { return remainingLifetime <= 0.0; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public WorldType getWorld() { return world; }
}
