package com.phantomcorridor.model.entity;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.ItemType;
import com.phantomcorridor.model.EquipmentType;
import com.phantomcorridor.model.WorldType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 玩家运行时模型，不依赖任何 JavaFX 控件。 */
public final class Player {

    private int hp;
    private double x;
    private double y;
    private int coins;
    private double phaseEnergy;
    private int attackCharges;
    private int maxAttackCharges;
    private double attackChargeRecoveryTimer;
    private WorldType currentWorld;
    private PlayerAnimationState animationState;
    private double animationTime;
    private double facingX;
    private double facingY;
    private double hitInvulnerability;
    private double dashTimeRemaining;
    private double dashCooldownRemaining;
    private double dashDirectionX;
    private double dashDirectionY;
    private final List<DashTrailPoint> dashTrail = new ArrayList<>();
    private final List<ItemType> items = new ArrayList<>();
    private final List<EquipmentType> equipment = new ArrayList<>();

    public Player(double x, double y) {
        reset(x, y);
    }

    public void reset(double x, double y) {
        this.hp = GameConfig.PLAYER_MAX_HP;
        this.x = x;
        this.y = y;
        this.coins = 0;
        this.phaseEnergy = GameConfig.PHASE_ENERGY_INITIAL;
        this.maxAttackCharges = GameConfig.ATTACK_CHARGE_MAX;
        this.attackCharges = maxAttackCharges;
        this.attackChargeRecoveryTimer = 0.0;
        this.currentWorld = WorldType.LIGHT;
        this.animationState = PlayerAnimationState.IDLE;
        this.animationTime = 0.0;
        this.facingX = 1.0;
        this.facingY = 0.0;
        this.hitInvulnerability = 0.0;
        this.dashTimeRemaining = 0.0;
        this.dashCooldownRemaining = 0.0;
        this.dashDirectionX = 1.0;
        this.dashDirectionY = 0.0;
        this.dashTrail.clear();
        this.items.clear();
        this.equipment.clear();
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

    /**
     * 触发一次闪避冲刺（空格）。
     *
     * <p>冲刺期间位移完全由冲刺方向决定，玩家输入的转向被忽略——这是"闪避"而不是"加速跑"。
     * 方向为空向量时沿当前朝向冲，保证站着不动按空格也能朝看得见的方向翻出去。
     *
     * @param directionX 期望的冲刺方向（通常是当前的移动输入；未归一化）
     * @param directionY 期望的冲刺方向
     * @return 是否真的开始冲刺：冷却未好、已在冲刺中或已阵亡时返回 {@code false}
     */
    public boolean tryStartDash(double directionX, double directionY) {
        if (hp <= 0 || isDashing() || dashCooldownRemaining > 0.0) return false;
        double length = Math.hypot(directionX, directionY);
        if (length > 0.0) {
            dashDirectionX = directionX / length;
            dashDirectionY = directionY / length;
        } else {
            dashDirectionX = facingX;
            dashDirectionY = facingY;
        }
        // 朝向理论上不会为零向量，兜底避免"原地冲刺"这种什么都看不见的手感。
        if (dashDirectionX == 0.0 && dashDirectionY == 0.0) dashDirectionX = 1.0;
        dashTimeRemaining = GameConfig.DASH_DURATION;
        return true;
    }

    /**
     * 推进冲刺计时：冲刺中递减剩余时间（归零时开始算内置冷却），否则递减冷却；
     * 同时老化拖尾残影，让冲刺结束后拖尾自然消散。
     */
    public void updateDash(double dt) {
        double step = Math.max(0.0, dt);
        for (int i = 0; i < dashTrail.size(); i++) dashTrail.set(i, dashTrail.get(i).aged(step));
        dashTrail.removeIf(DashTrailPoint::expired);
        if (dashTimeRemaining > 0.0) {
            dashTimeRemaining = Math.max(0.0, dashTimeRemaining - step);
            if (dashTimeRemaining == 0.0) dashCooldownRemaining = GameConfig.DASH_COOLDOWN;
            return;
        }
        dashCooldownRemaining = Math.max(0.0, dashCooldownRemaining - step);
    }

    /** 在当前位置留下一段拖尾残影；只在冲刺中记录，所以拖尾形状与冲刺轨迹一致。 */
    public void recordDashTrail() {
        if (!isDashing()) return;
        if (dashTrail.size() >= GameConfig.DASH_TRAIL_MAX) dashTrail.remove(0);
        dashTrail.add(new DashTrailPoint(x, y,
                GameConfig.DASH_TRAIL_LIFETIME, GameConfig.DASH_TRAIL_LIFETIME));
    }

    /** 是否正在冲刺（伤害免疫与渲染拖尾都以此为准）。 */
    public boolean isDashing() {
        return dashTimeRemaining > 0.0;
    }

    /** 冲刺剩余时间（秒）。 */
    public double getDashTimeRemaining() { return dashTimeRemaining; }

    /** 下一次冲刺还要等多久（秒）：冲刺剩余时间与内置冷却取较大者，0 表示随时可用。 */
    public double getDashCooldownRemaining() {
        return Math.max(dashTimeRemaining, dashCooldownRemaining);
    }

    /**
     * 冲刺是否已经可用。
     *
     * <p>冷却是内置的：界面不显示进度，冷却没走完时按键就是不生效，节奏由玩家自己掌握。
     * 这个方法只用于可用性判定（测试与后续需要读状态的系统），不是给 HUD 用的。
     */
    public boolean isDashReady() {
        return hp > 0 && !isDashing() && dashCooldownRemaining <= 0.0;
    }

    /** 冲刺方向（单位向量）：冲刺位移与拖尾朝向都用它，且不受冲刺途中的输入影响。 */
    public double getDashDirectionX() { return dashDirectionX; }
    public double getDashDirectionY() { return dashDirectionY; }

    /** 冲刺拖尾残影（按记录顺序，最早的在前）；渲染层只读。 */
    public List<DashTrailPoint> getDashTrail() {
        return Collections.unmodifiableList(dashTrail);
    }

    public void restorePhaseEnergy(double amount) {
        phaseEnergy = clamp(phaseEnergy + Math.max(0.0, amount), 0.0, GameConfig.PHASE_ENERGY_MAX);
    }

    public void consumePhaseEnergy(double amount) {
        phaseEnergy = clamp(phaseEnergy - Math.max(0.0, amount), 0.0, GameConfig.PHASE_ENERGY_MAX);
    }

    public void restoreAttackCharges(int amount) { attackCharges = Math.min(maxAttackCharges, attackCharges + Math.max(0, amount)); }
    public void updateAttackCharges(double dt) {
        if (attackCharges >= maxAttackCharges) { attackChargeRecoveryTimer = 0.0; return; }
        attackChargeRecoveryTimer += Math.max(0.0, dt);
        while (attackChargeRecoveryTimer >= GameConfig.ATTACK_CHARGE_RECOVERY_TIME
                && attackCharges < maxAttackCharges) {
            attackCharges++;
            attackChargeRecoveryTimer -= GameConfig.ATTACK_CHARGE_RECOVERY_TIME;
        }
    }
    public void increaseAttackChargeCapacity(int amount) {
        maxAttackCharges = Math.max(GameConfig.ATTACK_CHARGE_MAX, maxAttackCharges + Math.max(0, amount));
        attackCharges = maxAttackCharges;
    }
    public void addItem(ItemType item) {
        if (item == null || items.contains(item)) return;
        items.add(item);
        if (item == ItemType.UNIVERSAL || item == ItemType.DUAL) increaseAttackChargeCapacity(1);
    }
    public void equip(EquipmentType item) {
        if (item == null || equipment.contains(item)) return;
        equipment.removeIf(existing -> existing.affinity().equals(item.affinity())
                && !existing.affinity().equals("双界") && !existing.affinity().equals("通用"));
        equipment.add(item);
        while (equipment.size() > 3) equipment.remove(0);
    }
    public List<EquipmentType> getEquipment() { return Collections.unmodifiableList(equipment); }
    public boolean consumeAttackCharge() {
        if (attackCharges <= 0) return false;
        attackCharges--; return true;
    }

    public void toggleWorld() {
        currentWorld = currentWorld == WorldType.LIGHT ? WorldType.SHADOW : WorldType.LIGHT;
    }

    public void updateAnimation(double dt, double movementX, double movementY, boolean attacking,
                                boolean shifting) {
        hitInvulnerability = Math.max(0.0, hitInvulnerability - Math.max(0.0, dt));
        animationTime += Math.max(0.0, dt);
        // 冲刺中朝向锁在冲刺方向上：拖尾与角色朝向必须一致，否则拖尾会"横着飘"。
        double lookX = isDashing() ? dashDirectionX : movementX;
        double lookY = isDashing() ? dashDirectionY : movementY;
        if (lookX != 0.0 || lookY != 0.0) {
            double length = Math.hypot(lookX, lookY);
            facingX = lookX / length;
            facingY = lookY / length;
        }
        PlayerAnimationState next = hp <= 0 ? PlayerAnimationState.DOWN
                : isDashing() ? PlayerAnimationState.DASHING
                : shifting ? PlayerAnimationState.SHIFTING
                : attacking ? PlayerAnimationState.ATTACKING
                : movementX != 0.0 || movementY != 0.0 ? PlayerAnimationState.MOVING
                : PlayerAnimationState.IDLE;
        if (next != animationState) animationTime = 0.0;
        animationState = next;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 受到伤害。
     *
     * <p>两条免伤路径：受击后的短暂无敌时间（避免一帧内被重叠弹幕重复扣血），
     * 以及冲刺全程的无敌（闪避的位移必须配得上"躲开"这个词）。
     *
     * @return 是否真的掉血；免疫时返回 {@code false}
     */
    public boolean takeDamage(int damage) {
        // 冲刺全程免伤：闪避就是"用无敌帧换位移"，否则这段位移只是跑得快一点。
        if (damage <= 0 || isDashing() || hitInvulnerability > 0.0 || hp <= 0) return false;
        hp = Math.max(0, hp - damage);
        hitInvulnerability = GameConfig.PLAYER_HIT_INVULNERABILITY;
        return true;
    }

    public int getHp() { return hp; }
    public void restoreHealth(int amount) { hp = Math.min(GameConfig.PLAYER_MAX_HP, hp + Math.max(0, amount)); }
    public double getX() { return x; }
    public double getY() { return y; }

    /** 本局金币：击杀、奖励房与宝箱获得，商店消费。 */
    public int getCoins() { return coins; }

    public void addCoins(int amount) { coins = Math.max(0, coins + Math.max(0, amount)); }

    /** 扣款；金币不足时不扣并在返回 false。 */
    public boolean spendCoins(int amount) {
        if (amount < 0 || coins < amount) return false;
        coins -= amount;
        return true;
    }
    public double getPhaseEnergy() { return phaseEnergy; }
    public int getAttackCharges() { return attackCharges; }
    public int getMaxAttackCharges() { return maxAttackCharges; }
    public int getAttackDamage() {
        int bonus = items.stream().mapToInt(item -> item == ItemType.DUAL || item == ItemType.UNIVERSAL
                || (currentWorld == WorldType.LIGHT && item == ItemType.LIGHT)
                || (currentWorld == WorldType.SHADOW && item == ItemType.SHADOW) ? 1 : 0).sum();
        bonus += equipment.stream().mapToInt(item -> item == EquipmentType.DAWN_WAND && currentWorld == WorldType.LIGHT ? 1
                : item == EquipmentType.SHADOW_FANG && currentWorld == WorldType.SHADOW ? 1
                : item == EquipmentType.RIFT_TWINBLADE || item == EquipmentType.DAWN_SEAL && currentWorld == WorldType.LIGHT ? 1
                : 0).sum();
        return 1 + bonus;
    }
    public WorldType getCurrentWorld() { return currentWorld; }
    public PlayerAnimationState getAnimationState() { return animationState; }
    public double getAnimationTime() { return animationTime; }
    public double getFacingX() { return facingX; }
    public double getFacingY() { return facingY; }
    public List<ItemType> getItems() { return Collections.unmodifiableList(items); }
    public boolean isHitInvulnerable() { return hitInvulnerability > 0.0; }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
