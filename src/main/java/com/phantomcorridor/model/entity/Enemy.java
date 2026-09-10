package com.phantomcorridor.model.entity;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.Difficulty;
import com.phantomcorridor.model.WorldType;

/** 纯运行时敌人模型。非当前世界的敌人存在但不会更新 AI 或造成碰撞。 */
public final class Enemy {
    private final EnemyKind kind;
    private WorldType world;
    private final boolean boss;
    private final Difficulty difficulty;
    private final int maxHp;
    private final int defense;
    private double x;
    private double y;
    private int hp;
    private boolean aware;
    private double alertRemaining;
    private double attackCooldown;
    private double blinkCooldown;
    private int avoidanceSide;
    private double avoidanceHeading = Double.NaN;
    private double avoidanceStuckTime;
    private double closestDistance = Double.MAX_VALUE;
    private double stuckTime;
    private double progressAnchorX;
    private double progressAnchorY;
    private int lastMeleeHitId = -1;
    /** 素材包中的 body 动作目录名，例如 move、attack_windup、shield_bash_release。 */
    private String animationAction = "idle";
    /** front / back / right / left。左向优先使用已导出的镜像帧，不再二次镜像。 */
    private String facing = "front";
    private double animationTime;
    private double animationDuration = 1.0 / 6.0;
    private boolean animationLoop = true;
    private int nextSkillIndex;
    private double guardRemaining;

    /**
     * @param floor      所在层数（从 1 开始）：生命值按层增长，防御随层提高
     * @param difficulty 本局难度：在同一套层数成长之上再乘难度倍率
     */
    public Enemy(EnemyKind kind, WorldType world, double x, double y, int floor, Difficulty difficulty) {
        this.kind = kind;
        this.world = world;
        this.boss = kind == EnemyKind.WATCHER;
        this.x = x;
        this.y = y;
        this.difficulty = difficulty == null ? Difficulty.NORMAL : difficulty;
        this.maxHp = scaledHitPoints(kind, floor, this.difficulty);
        this.defense = floorDefense(floor, this.difficulty);
        this.hp = maxHp;
        this.alertRemaining = GameConfig.ENEMY_ALERT_TIME;
    }

    /** 标准难度下的敌人。 */
    public Enemy(EnemyKind kind, WorldType world, double x, double y, int floor) {
        this(kind, world, x, y, floor, Difficulty.NORMAL);
    }

    public EnemyKind getKind() { return kind; }
    public WorldType getWorld() { return world; }
    public void setWorld(WorldType world) { this.world = world; this.alertRemaining = GameConfig.ENEMY_ALERT_TIME; }
    public boolean isBoss() { return boss; }
    public double getX() { return x; }
    public double getY() { return y; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public boolean isDead() { return hp <= 0; }
    /** 防御：每次受击固定减免的伤害量。 */
    public int getDefense() { return defense; }

    /** 生成这只敌人时的难度（生命与防御已按它算好）。 */
    public Difficulty getDifficulty() { return difficulty; }

    /**
     * 承受玩家一次攻击：先扣除防御，但每次至少造成 1 点伤害。
     *
     * <p>“至少 1 点”是硬规则：否则高层数下玩家攻击力不足时会完全打不动敌人。
     *
     * @return 实际造成的伤害
     */
    public int takeHit(int attackDamage) {
        int dealt = Math.max(1, attackDamage - defense);
        damage(dealt);
        return dealt;
    }

    /** 伤害同时驱动受击/死亡本体动画；死亡残影由 EnemySystem 保持播放。 */
    public void damage(int amount) {
        if (isDead()) return;
        hp = Math.max(0, hp - Math.max(0, amount));
        playAnimation(hp <= 0 ? "death" : "hurt", hp <= 0 ? .70 : .28, false);
    }
    public double getAlertRemaining() { return alertRemaining; }

    /** 起手时间：裂隙闪现落地后重设，给玩家留出反应窗口。 */
    public void setAlertRemaining(double seconds) { alertRemaining = Math.max(0.0, seconds); }

    /** 距离下一次裂隙闪现还有多久（秒）。 */
    public double getBlinkCooldown() { return blinkCooldown; }

    public void setBlinkCooldown(double seconds) { blinkCooldown = Math.max(0.0, seconds); }

    public void updateTimers(double dt) {
        alertRemaining = Math.max(0.0, alertRemaining - dt);
        attackCooldown = Math.max(0.0, attackCooldown - dt);
        blinkCooldown = Math.max(0.0, blinkCooldown - dt);
        guardRemaining = Math.max(0.0, guardRemaining - dt);
        animationTime += Math.max(0.0, dt);
    }
    public boolean canAttack() { return alertRemaining <= 0.0 && attackCooldown <= 0.0; }
    public void setAttackCooldown(double seconds) { attackCooldown = seconds; }
    public int getLastMeleeHitId() { return lastMeleeHitId; }
    public void setLastMeleeHitId(int id) { lastMeleeHitId = id; }

    public String getAnimationAction() { return animationAction; }
    public String getFacing() { return facing; }
    public double getAnimationTime() { return animationTime; }
    public double getAnimationDuration() { return animationDuration; }
    public boolean isAnimationLooping() { return animationLoop; }
    public boolean isAnimationFinished() { return !animationLoop && animationTime >= animationDuration; }
    public int nextSkillIndex() { return nextSkillIndex++; }

    /** 切换动作时才重置计时，循环动作重复赋值不会导致待机/移动第一帧抖动。 */
    public void playAnimation(String action, double duration, boolean loop) {
        if (animationAction.equals(action) && animationLoop == loop) return;
        animationAction = action;
        animationDuration = Math.max(0.05, duration);
        animationLoop = loop;
        animationTime = 0.0;
    }

    public void setFacingFromVector(double dx, double dy) {
        if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) return;
        if (Math.abs(dx) >= Math.abs(dy)) facing = dx >= 0.0 ? "right" : "left";
        else facing = dy >= 0.0 ? "front" : "back";
    }

    public boolean isInLockedAnimation() {
        return !animationLoop && !animationAction.equals("hurt") && !animationAction.equals("death")
                && !animationAction.equals("idle") && !animationAction.equals("move");
    }
    public boolean isGuarding() { return guardRemaining > 0.0; }
    public void beginGuard(double seconds) {
        guardRemaining = Math.max(0.0, seconds);
        playAnimation("guard", seconds, true);
    }

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

    /**
     * 记录本帧与玩家的距离，用于识别“被墙卡住”。
     *
     * <p>两项都满足才算卡死：最近 {@link GameConfig#ENEMY_STUCK_TIME} 秒内既没有比历史最近距离
     * 再近 {@link GameConfig#ENEMY_STUCK_PROGRESS_STEP} 像素，自身也没挪出
     * {@link GameConfig#ENEMY_STUCK_MOVE_STEP} 像素。只看“没靠近”会把绕路中的敌人误判成卡死——
     * 绕着一大块障碍走的时候，直线距离本来就会先变远。
     *
     * @param unreachable 寻路是否判定“玩家根本走不到”（例如玩家躲在首领挤不进去的角落）。
     *                    这种情况即使敌人还在原地绕圈也算卡住，否则它会绕着障碍转一辈子。
     * @return 是否需要脱困
     */
    public boolean trackProgress(double distance, double dt, boolean unreachable) {
        boolean closer = distance < closestDistance - GameConfig.ENEMY_STUCK_PROGRESS_STEP;
        boolean stillMoving = Math.hypot(x - progressAnchorX, y - progressAnchorY)
                > GameConfig.ENEMY_STUCK_MOVE_STEP;
        if (closer) closestDistance = distance;
        if (!unreachable && (closer || stillMoving)) {
            stuckTime = 0.0;
            progressAnchorX = x;
            progressAnchorY = y;
            return false;
        }
        stuckTime += Math.max(0.0, dt);
        return stuckTime >= GameConfig.ENEMY_STUCK_TIME;
    }

    /** 站定保持开火站位、或刚被挪到新位置时重置跟踪。 */
    public void resetProgressTracking(double distance) {
        closestDistance = distance;
        stuckTime = 0.0;
        progressAnchorX = x;
        progressAnchorY = y;
    }

    /** 连续没有更接近玩家的累计时间（秒），供调试与测试观察。 */
    public double getStuckTime() { return stuckTime; }

    /** 第 N 层、指定难度下的生命值：基础生命值 × 层数成长 × 难度倍率（四舍五入，至少 1）。 */
    private static int scaledHitPoints(EnemyKind kind, int floor, Difficulty difficulty) {
        double floorScale = 1.0 + (Math.max(1, floor) - 1) * GameConfig.ENEMY_HP_GROWTH_PER_FLOOR;
        return Math.max(1, (int) Math.round(kind.hitPoints() * floorScale * difficulty.enemyStatMultiplier()));
    }

    /**
     * 第 N 层、指定难度下的防御：每层 +1 后乘难度倍率并向下取整（简单难度会更晚才有防御），
     * 上限同样随难度缩放。
     */
    private static int floorDefense(int floor, Difficulty difficulty) {
        double multiplier = difficulty.enemyStatMultiplier();
        double raw = (Math.max(1, floor) - 1) * GameConfig.ENEMY_DEFENSE_PER_FLOOR * multiplier;
        int cap = (int) Math.round(GameConfig.ENEMY_DEFENSE_MAX * multiplier);
        return Math.min(cap, (int) Math.floor(raw));
    }
}
