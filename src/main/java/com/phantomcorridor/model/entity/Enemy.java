package com.phantomcorridor.model.entity;

import com.phantomcorridor.model.WorldType;

/** 纯运行时敌人模型。非当前世界的敌人存在但不会更新 AI 或造成碰撞。 */
public final class Enemy {
    private final EnemyKind kind;
    private WorldType world;
    private final boolean boss;
    private double x;
    private double y;
    private int hp;
    private double alertRemaining;
    private double attackCooldown;
    private int lastMeleeHitId = -1;

    public Enemy(EnemyKind kind, WorldType world, double x, double y) {
        this.kind = kind;
        this.world = world;
        this.boss = kind == EnemyKind.WATCHER;
        this.x = x;
        this.y = y;
        this.hp = kind.hitPoints();
        this.alertRemaining = 0.6;
    }

    public EnemyKind getKind() { return kind; }
    public WorldType getWorld() { return world; }
    public void setWorld(WorldType world) { this.world = world; this.alertRemaining = 0.6; }
    public boolean isBoss() { return boss; }
    public double getX() { return x; }
    public double getY() { return y; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return kind.hitPoints(); }
    public boolean isDead() { return hp <= 0; }
    public void damage(int amount) { hp = Math.max(0, hp - Math.max(0, amount)); }
    public double getAlertRemaining() { return alertRemaining; }
    public void updateTimers(double dt) { alertRemaining = Math.max(0.0, alertRemaining - dt); attackCooldown = Math.max(0.0, attackCooldown - dt); }
    public boolean canAttack() { return alertRemaining <= 0.0 && attackCooldown <= 0.0; }
    public void setAttackCooldown(double seconds) { attackCooldown = seconds; }
    public int getLastMeleeHitId() { return lastMeleeHitId; }
    public void setLastMeleeHitId(int id) { lastMeleeHitId = id; }
}
