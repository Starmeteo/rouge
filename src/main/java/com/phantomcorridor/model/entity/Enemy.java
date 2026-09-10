package com.phantomcorridor.model.entity;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.WorldType;

/** 纯运行时敌人模型。非当前世界的敌人存在但不会更新 AI 或造成碰撞。 */
public final class Enemy {
    private final EnemyKind kind;
    private WorldType world;
    private final boolean boss;
    private double x;
    private double y;
    private int hp;
    private boolean aware;
    private double alertRemaining;
    private double attackCooldown;
    private int avoidanceSide;
    private double avoidanceHeading = Double.NaN;
    private double avoidanceStuckTime;
    private int lastMeleeHitId = -1;

    public Enemy(EnemyKind kind, WorldType world, double x, double y) {
        this.kind = kind;
        this.world = world;
        this.boss = kind == EnemyKind.WATCHER;
        this.x = x;
        this.y = y;
        this.hp = kind.hitPoints();
        this.alertRemaining = GameConfig.ENEMY_ALERT_TIME;
    }

    public EnemyKind getKind() { return kind; }
    public WorldType getWorld() { return world; }
    public void setWorld(WorldType world) { this.world = world; this.alertRemaining = GameConfig.ENEMY_ALERT_TIME; }
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

    /** 是否已锁定玩家（索敌成功）。锁定后即使被墙挡住视线也会持续追击。 */
    public boolean isAware() { return aware; }

    /** 索敌成功，进入追击状态。 */
    public void markAware() { aware = true; }

    /** 玩家切界离开本世界后丢失目标；下次回到该世界需要重新索敌并重新起手。 */
    public void loseAwareness() {
        aware = false;
        alertRemaining = GameConfig.ENEMY_ALERT_TIME;
    }

    /**
     * 当前贴墙绕行方向：0 表示未在绕行，+1/-1 表示沿切线的哪一侧绕过障碍。
     *
     * <p>绕行方向必须记住：如果每帧都重新挑方向，敌人会在两个相邻位置之间来回横跳，
     * 看上去完全卡死却始终无法绕开障碍。
     */
    public int getAvoidanceSide() { return avoidanceSide; }

    /** 最近一次实际前进的方向（弧度）；从未移动过时为 {@link Double#NaN}，用于禁止绕行时掉头。 */
    public double getAvoidanceHeading() { return avoidanceHeading; }

    /** 沿当前一侧已经走不动的累计时间（秒）。 */
    public double getAvoidanceStuckTime() { return avoidanceStuckTime; }

    public void setAvoidanceSide(int side) {
        this.avoidanceSide = side;
        this.avoidanceStuckTime = 0.0;
    }

    public void setAvoidanceHeading(double radians) { this.avoidanceHeading = radians; }

    public void addAvoidanceStuckTime(double dt) { avoidanceStuckTime += Math.max(0.0, dt); }

    public void resetAvoidanceStuckTime() { avoidanceStuckTime = 0.0; }

    /** 已经能直线接近玩家时结束绕行。 */
    public void clearAvoidance() {
        avoidanceSide = 0;
        avoidanceStuckTime = 0.0;
    }
}
