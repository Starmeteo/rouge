package com.phantomcorridor.model.entity;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.ItemType;
import com.phantomcorridor.model.EquipmentType;
import com.phantomcorridor.model.WorldType;
import com.phantomcorridor.model.combat.DamageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 玩家运行时模型，不依赖任何 JavaFX 控件。 */
public final class Player {

    /** 受击结算结果：护盾吸收了多少、生命实际掉了多少、伤害类型是什么。 */
    public record DamageResult(double absorbedByShield, int healthLost, double remainingShield,
                               DamageType type) {
        /** 是否发生了有效受击（护盾或生命至少有一项被扣）。 */
        public boolean landed() { return absorbedByShield > 0.0 || healthLost > 0; }
    }

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
    /** 护盾：在生命值之前挨打的临时生命值。 */
    private Shield shield;
    private final List<ItemType> items = new ArrayList<>();
    private final List<EquipmentType> equipment = new ArrayList<>();

    public Player(double x, double y) {
        reset(x, y);
    }

    public void reset(double x, double y) {
        // 先清理上一局的装备，再计算初始生命上限，避免行者心核把新局初始血量错误地保留为 120。
        this.items.clear();
        this.equipment.clear();
        this.hp = maxHp();
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
        this.shield = new Shield(shieldCapacity());
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
        // 护盾类装备会撑大容量：只同步容量、保留剩余量，
        // 否则“在商店换一件饰品”就白送一整条护盾。
        if (shield != null) shield.setCapacity(shieldCapacity());
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
        if (movementX != 0.0 || movementY != 0.0) {
            double length = Math.hypot(movementX, movementY);
            facingX = movementX / length;
            facingY = movementY / length;
        }
        PlayerAnimationState next = hp <= 0 ? PlayerAnimationState.DOWN
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
     * 结算一次敌人攻击的伤害。
     *
     * <p>顺序是固定的：<b>先扣护盾，扣穿之后剩下的才进生命值</b>。
     * 护盾是临时生命值而不是减伤，所以它只顶“点数”，一记 40 点重击打穿 30 点护盾后，
     * 依然会把剩下的 10 点实打实送进生命条。
     *
     * <p>受击后有一段短暂无敌，避免同帧的重叠弹幕重复扣血；
     * 无敌期间连护盾也不会被消耗。被击会获得少量相位能量（§3.4）。
     *
     * @param damage 伤害点数（可以带小数；打进生命值时向上取整，保证任何非零伤害都至少掉 1 点）
     * @return 本次受击的结算明细；未生效时返回 {@code null}
     */
    public DamageResult takeDamage(double damage, DamageType type) {
        if (damage <= 0.0 || hitInvulnerability > 0.0 || hp <= 0) return null;
        // Shield.absorb 返回的是“护盾没挡下、要继续打进生命值”的溢出量。
        double overflow = shield.absorb(damage);
        double absorbed = damage - overflow;
        int healthLost = (int) Math.ceil(overflow - 1e-9);
        if (healthLost <= 0 && absorbed <= 0.0) return null;
        hp = Math.max(0, hp - healthLost);
        hitInvulnerability = GameConfig.PLAYER_HIT_INVULNERABILITY;
        restorePhaseEnergy(GameConfig.PHASE_ENERGY_ON_HIT);
        return new DamageResult(absorbed, healthLost, shield.getCurrent(),
                type == null ? DamageType.PHYSICAL : type);
    }

    /** 无属性伤害的便捷入口（测试与不含相位的来源使用）。 */
    public DamageResult takeDamage(double damage) {
        return takeDamage(damage, DamageType.PHYSICAL);
    }

    public int getHp() { return hp; }

    /** 当前最大生命值；行者心核按设计提升 20%，装备时不自动补血。 */
    public int maxHp() {
        boolean hasHeart = equipment.contains(EquipmentType.WAYFARER_HEART);
        return hasHeart ? (int) Math.ceil(GameConfig.PLAYER_MAX_HP * 1.20) : GameConfig.PLAYER_MAX_HP;
    }

    public void restoreHealth(int amount) { hp = Math.min(maxHp(), hp + Math.max(0, amount)); }

    /** 按「几瓶生命恢复药剂」回血：一瓶 = {@link GameConfig#PLAYER_HEAL_PER_PICKUP} 点。 */
    public void restoreHealthByPickups(int pickups) {
        restoreHealth(Math.max(0, pickups) * GameConfig.PLAYER_HEAL_PER_PICKUP);
    }

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
        // 新增武器的首轮数值采用整数模型的最小可见增幅；精确多段伤害由攻击方案继续细分。
        bonus += equipment.stream().mapToInt(item -> switch (item) {
            case PRISM_FAN_WAND, SUNLANCE, MIRROR_ORB, SOLAR_BURST_STAFF ->
                    currentWorld == WorldType.LIGHT ? 1 : 0;
            case CRESCENT_REAPER, RETURNING_FANG, NIGHTFALL_GREATSWORD ->
                    currentWorld == WorldType.SHADOW ? 1 : 0;
            case ECLIPSE_RELAY -> 1;
            case FOCUS_LENS -> currentWorld == WorldType.LIGHT ? 1 : 0;
            case HUNTERS_FANG -> currentWorld == WorldType.SHADOW ? 1 : 0;
            default -> 0;
        }).sum();
        return 1 + bonus;
    }
    public boolean hasEquipment(EquipmentType item) { return item != null && equipment.contains(item); }
    public WorldType getCurrentWorld() { return currentWorld; }
    public PlayerAnimationState getAnimationState() { return animationState; }
    public double getAnimationTime() { return animationTime; }
    public double getFacingX() { return facingX; }
    public double getFacingY() { return facingY; }
    public List<ItemType> getItems() { return Collections.unmodifiableList(items); }
    public boolean isHitInvulnerable() { return hitInvulnerability > 0.0; }

    // ---- 护盾 ----

    /** 护盾容量：基础容量 + 装备加成（相位容器会额外撑盾）。 */
    public double shieldCapacity() {
        return GameConfig.PLAYER_SHIELD_CAPACITY
                + equipment.stream().mapToDouble(EquipmentType::shieldCapacityBonus).sum();
    }

    /** 当前护盾剩余点数（0 表示没有护盾）。 */
    public double getShield() { return shield.getCurrent(); }

    /** 护盾容量。 */
    public double getMaxShield() { return shield.getCapacity(); }

    /** 是否还剩护盾。 */
    public boolean hasShield() { return shield.isActive(); }

    /** 护盾剩余比例（0～1），供 HUD 画条。 */
    public double getShieldRatio() { return shield.getRatio(); }

    /**
     * 护盾换算成“血条上的等价长度比例”（0～1）。
     *
     * <p>护盾条是叠在血条上的，所以必须先换算到**生命刻度**上：
     * 30 点护盾对着 100 点血就是血条长度的 30%，而不是护盾自己容量的 100%。
     * 换算放在模型层而不是渲染层，是为了让这个比例能被测试直接钉住。
     */
    public double getShieldBarRatio() {
        int maxHp = maxHp();
        if (maxHp <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, shield.getCurrent() / maxHp));
    }


    /** 回满护盾（换层时调用）。 */
    public void refillShield() { shield.reset(shieldCapacity()); }

    /** 补充护盾（不超过容量）。 */
    public void restoreShield(double amount) { shield.restore(amount); }

    /** 清空护盾：只清剩余量，容量不变。 */
    public void clearShield() { shield.clear(); }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
