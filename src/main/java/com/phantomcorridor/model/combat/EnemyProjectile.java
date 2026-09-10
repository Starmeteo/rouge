package com.phantomcorridor.model.combat;

import com.phantomcorridor.model.WorldType;

/** 敌方弹幕的最小纯模型契约，供相位脉冲及后续敌人系统复用。 */
public final class EnemyProjectile {
    private double x;
    private double y;
    private final double velocityX, velocityY, radius;
    private double lifetime;
    private final boolean pulseClearable;
    private final WorldType world;

    public EnemyProjectile(double x, double y, WorldType world) {
        this(x, y, 0, 0, 8, world, Double.POSITIVE_INFINITY, true);
    }
    public EnemyProjectile(double x, double y, double velocityX, double velocityY, double radius,
                           WorldType world, double lifetime, boolean pulseClearable) {
        this.x = x;
        this.y = y;
        this.world = world;
        this.velocityX=velocityX; this.velocityY=velocityY; this.radius=radius; this.lifetime=lifetime; this.pulseClearable=pulseClearable;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public WorldType getWorld() { return world; }
    public double getRadius(){return radius;} public boolean isPulseClearable(){return pulseClearable;}
    public void update(double dt){x+=velocityX*dt; y+=velocityY*dt; lifetime-=dt;}
    public void expire(){ lifetime = 0; }
    public boolean isExpired(){return lifetime<=0;}
}
